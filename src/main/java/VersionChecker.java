import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Checks GitHub for a newer version and, if the user agrees, installs it
 * by itself and reopens the app - the user never sees an installer.
 *
 * How the install happens (a running app can't overwrite its own files):
 *   1. Download the new installer (.msi on Windows, .dmg on macOS) to a temp file.
 *   2. Write a tiny helper script that waits until THIS process has exited,
 *      installs the update silently, then reopens the app.
 *   3. Launch that script as an independent process and exit.
 *
 * Windows: msiexec /passive shows only a small progress bar, no clicks. If the
 *   app was installed for all users (Program Files), Windows asks for admin
 *   permission once (UAC). Build with --win-per-user-install to avoid that.
 *   The MSI must be built with a fixed --win-upgrade-uuid so it replaces the
 *   old version instead of installing a second copy.
 * macOS: the new .app is copied over the current one with ditto, which keeps
 *   the signature and notarization intact. If the app's folder isn't writable
 *   by this user, the .dmg is opened instead so they can drag it in.
 */
public class VersionChecker {

    //bump this by hand whenever you tag and publish a new release
    public static final String CURRENT_VERSION = "1.2.9";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    //Public repo that holds ONLY the installers (no code). The code repo stays private;
    //the CI workflow publishes each release here. Must be public so this check needs no token.
    private static final String REPO = "Memeli2202/DoctorHelper-Releases";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";

    private static class Asset {
        final String nombre;
        final String url;
        Asset(String nombre, String url) {
            this.nombre = nombre;
            this.url = url;
        }
    }

    /**
     * Kicks off a background check; safe to call from the EDT on startup.
     * Does nothing visible if there's no update, no internet, or the
     * GitHub API is unreachable - an update check should never interrupt
     * or break normal use of the app.
     */
    public static void verificarActualizacion(Component parent) {
        new SwingWorker<String, Void>() {
            private Asset asset;

            @Override
            protected String doInBackground() {
                try {
                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(5))
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .build();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_URL))
                            .header("Accept", "application/vnd.github+json")
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) {
                        registrar("HTTP " + response.statusCode() + " al consultar " + API_URL
                                + " - cuerpo: " + recortar(response.body()));
                        return null;
                    }
                    JsonNode raiz = MAPPER.readTree(response.body());
                    String ultimaVersion = extraerTagName(raiz);
                    if (ultimaVersion == null) {
                        registrar("No se pudo extraer tag_name del JSON de respuesta. Cuerpo: " + recortar(response.body()));
                        return null;
                    }
                    if (!esMasNueva(ultimaVersion, CURRENT_VERSION)) {
                        registrar("Version remota (" + ultimaVersion + ") no es mas nueva que la version actual (" + CURRENT_VERSION + "). No hay actualizacion.");
                        return null;
                    }
                    asset = buscarAssetParaEstePlataforma(raiz);
                    if (asset == null) {
                        registrar("Version remota (" + ultimaVersion + ") es mas nueva que " + CURRENT_VERSION
                                + ", pero no se encontro un asset para esta plataforma (sufijo esperado: "
                                + detectarSufijoDePlataforma() + "). Assets en el release: " + extraerNombresDeAssets(raiz));
                        return null;
                    }
                    registrar("Actualizacion encontrada: " + ultimaVersion + " (actual: " + CURRENT_VERSION
                            + "), asset: " + asset.nombre);
                    return ultimaVersion;
                } catch (Exception ex) {
                    //offline, DNS failure, GitHub down, rate-limited, etc. - fail silently to the user,
                    //but still leave a trace in the log file so it can be diagnosed later
                    registrar("Excepcion al verificar actualizacion: " + ex);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    String ultimaVersion = get();
                    if (ultimaVersion != null && asset != null) {
                        confirmarEInstalar(parent, ultimaVersion, asset);
                    }
                } catch (Exception ex) {
                    //never let a failed update check surface as an error to the user
                    registrar("Excepcion inesperada en done(): " + ex);
                }
            }
        }.execute();
    }

    /**
     * Writes to ~/.doctorhelper/update_check.log so a failed check or install
     * can be diagnosed without a terminal attached to the packaged app.
     */
    private static void registrar(String mensaje) {
        try {
            Path log = carpetaDeLogs().resolve("update_check.log");
            String linea = LocalDateTime.now() + " - " + mensaje + System.lineSeparator();
            Files.writeString(log, linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            //logging itself must never break the app
        }
    }

    private static Path carpetaDeLogs() throws IOException {
        Path carpeta = Path.of(System.getProperty("user.home"), ".doctorhelper");
        Files.createDirectories(carpeta);
        return carpeta;
    }

    private static String extraerNombresDeAssets(JsonNode raiz) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode assetNode : raiz.path("assets")) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(assetNode.path("name").asText());
        }
        return sb.toString();
    }

    private static String recortar(String texto) {
        if (texto == null) return "null";
        return texto.length() > 300 ? texto.substring(0, 300) + "..." : texto;
    }

    /** One question, then everything else happens by itself. */
    private static void confirmarEInstalar(Component parent, String nuevaVersion, Asset asset) {
        int opcion = JOptionPane.showConfirmDialog(parent,
                "Hay una nueva versión disponible (" + nuevaVersion + ").\n"
                        + "¿Actualizar ahora? La aplicación se cerrará y se volverá a abrir sola.\n"
                        + "Guarda tu trabajo antes de continuar.",
                "Actualización disponible",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        JDialog progreso = DialogoProgreso.mostrar(parent, "Descargando actualización...");

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected Path doInBackground() {
                try {
                    return descargarArchivo(asset);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                progreso.dispose();

                if (error != null) {
                    registrar("Fallo la descarga: " + error);
                    JOptionPane.showMessageDialog(parent, "No se pudo descargar la actualización: " + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    aplicarActualizacion(get());
                } catch (Exception ex) {
                    registrar("Fallo al aplicar la actualizacion: " + ex);
                    JOptionPane.showMessageDialog(parent, "No se pudo instalar la actualización: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        progreso.setVisible(true);
    }

    private static Path descargarArchivo(Asset asset) throws IOException, InterruptedException {
        Path destino = Files.createTempFile("update_", "_" + asset.nombre);
        //GitHub download links redirect to a CDN, so redirects must be followed
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(asset.url))
                .timeout(Duration.ofMinutes(5)) //installers can be sizable
                .GET()
                .build();
        HttpResponse<Path> respuesta = client.send(request, HttpResponse.BodyHandlers.ofFile(destino));
        if (respuesta.statusCode() != 200 || Files.size(destino) < 1_000_000) {
            throw new IOException("descarga incompleta (HTTP " + respuesta.statusCode() + ")");
        }
        return destino;
    }

    /**
     * Path of the running app's own launcher (e.g. C:\...\DesktopReportBuilder.exe or
     * /Applications/DesktopReportBuilder.app/Contents/MacOS/DesktopReportBuilder).
     * Empty when running from an IDE / plain "java", where self-updating makes no sense.
     */
    private static Optional<Path> lanzadorActual() {
        Optional<String> comando = ProcessHandle.current().info().command();
        if (comando.isEmpty()) {
            return Optional.empty();
        }
        Path ruta = Path.of(comando.get());
        String nombre = ruta.getFileName().toString().toLowerCase();
        if (nombre.equals("java") || nombre.equals("java.exe") || nombre.equals("javaw.exe")) {
            return Optional.empty();
        }
        return Optional.of(ruta);
    }

    private static void aplicarActualizacion(Path archivoDescargado) throws IOException {
        String osName = System.getProperty("os.name", "").toLowerCase();
        Optional<Path> lanzador = lanzadorActual();
        if (lanzador.isEmpty()) {
            //not a packaged install (e.g. running from the IDE): just hand over the installer
            registrar("No es una instalacion empaquetada; se abre el instalador manualmente.");
            Desktop.getDesktop().open(archivoDescargado.toFile());
            return;
        }

        if (osName.contains("win")) {
            aplicarActualizacionWindows(archivoDescargado, lanzador.get());
        } else if (osName.contains("mac")) {
            aplicarActualizacionMac(archivoDescargado, lanzador.get());
        } else {
            JOptionPane.showMessageDialog(null, "Actualización automática no disponible para este sistema operativo.");
        }
    }

    private static void aplicarActualizacionWindows(Path instalador, Path lanzador) throws IOException {
        long pid = ProcessHandle.current().pid();
        Path logInstalacion = carpetaDeLogs().resolve("update_install.log");
        String nombre = instalador.getFileName().toString().toLowerCase();

        String instalar;
        if (nombre.endsWith(".msi")) {
            //silent: only a small progress bar, no wizard; UAC prompt only if installed for all users
            instalar = "start \"\" /wait msiexec /i \"" + instalador.toAbsolutePath() + "\" /passive /norestart /l*v \""
                    + logInstalacion + "\"\r\n";
        } else {
            //older releases only had the .exe wizard - run it normally as a fallback
            instalar = "start \"\" /wait \"" + instalador.toAbsolutePath() + "\"\r\n";
        }

        Path script = Files.createTempFile("actualizar_", ".bat");
        String contenido = "@echo off\r\n"
                //wait until this app has really exited, so its files can be replaced
                + ":esperar\r\n"
                + "tasklist /FI \"PID eq " + pid + "\" 2>nul | find \"" + pid + "\" >nul\r\n"
                + "if not errorlevel 1 (\r\n"
                + "  timeout /t 1 /nobreak >nul\r\n"
                + "  goto esperar\r\n"
                + ")\r\n"
                + instalar
                + "start \"\" \"" + lanzador.toAbsolutePath() + "\"\r\n"
                + "del \"" + instalador.toAbsolutePath() + "\" >nul 2>&1\r\n"
                + "(goto) 2>nul & del \"%~f0\"\r\n";
        Files.writeString(script, contenido);

        registrar("Instalando actualizacion en Windows con " + instalador.getFileName() + ", reabriendo " + lanzador);
        new ProcessBuilder("cmd", "/c", "start", "", "/min", script.toAbsolutePath().toString()).start();
        System.exit(0);
    }

    private static void aplicarActualizacionMac(Path dmg, Path lanzador) throws IOException {
        //.../DesktopReportBuilder.app/Contents/MacOS/DesktopReportBuilder -> .../DesktopReportBuilder.app
        Path app = lanzador.getParent().getParent().getParent();
        if (!app.getFileName().toString().endsWith(".app")) {
            throw new IOException("no se encontró la aplicación instalada en " + lanzador);
        }
        Path carpetaApp = app.getParent();

        if (!Files.isWritable(carpetaApp) || !Files.isWritable(app)) {
            //this user can't replace the app (e.g. installed by another admin): open the .dmg instead
            registrar("Sin permiso de escritura en " + carpetaApp + "; se abre el .dmg para instalar a mano.");
            Desktop.getDesktop().open(dmg.toFile());
            JOptionPane.showMessageDialog(null,
                    "Arrastra la aplicación a la carpeta Aplicaciones para terminar la actualización.",
                    "Actualización", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        long pid = ProcessHandle.current().pid();
        Path log = carpetaDeLogs().resolve("update_install.log");
        String nombreApp = app.getFileName().toString();
        Path script = Files.createTempFile("actualizar_", ".sh");
        String contenido = "#!/bin/bash\n"
                + "exec >>\"" + log + "\" 2>&1\n"
                + "echo \"$(date) - instalando actualizacion\"\n"
                //wait until this app has really exited
                + "while kill -0 " + pid + " 2>/dev/null; do sleep 0.5; done\n"
                + "MONTAJE=$(mktemp -d /tmp/doctorhelper_update.XXXXXX)\n"
                + "hdiutil attach \"" + dmg.toAbsolutePath() + "\" -nobrowse -noautoopen -mountpoint \"$MONTAJE\" || exit 1\n"
                + "NUEVA=\"$MONTAJE/" + nombreApp + "\"\n"
                + "if [ ! -d \"$NUEVA\" ]; then NUEVA=$(ls -d \"$MONTAJE\"/*.app | head -1); fi\n"
                + "if [ -d \"$NUEVA\" ]; then\n"
                //copy to a temp name first, then swap: never leave the doctor with no app if the copy fails
                + "  rm -rf \"" + app + ".nueva\"\n"
                + "  if ditto \"$NUEVA\" \"" + app + ".nueva\"; then\n"
                + "    rm -rf \"" + app + "\"\n"
                + "    mv \"" + app + ".nueva\" \"" + app + "\"\n"
                + "  fi\n"
                + "fi\n"
                + "hdiutil detach \"$MONTAJE\" -quiet\n"
                + "rmdir \"$MONTAJE\"\n"
                + "rm -f \"" + dmg.toAbsolutePath() + "\"\n"
                + "open \"" + app + "\"\n"
                + "rm -f \"$0\"\n";
        Files.writeString(script, contenido);
        //noinspection ResultOfMethodCallIgnored
        script.toFile().setExecutable(true);

        registrar("Instalando actualizacion en macOS desde " + dmg.getFileName() + " sobre " + app);
        new ProcessBuilder("/bin/bash", script.toAbsolutePath().toString()).start();
        System.exit(0);
    }

    private static String extraerTagName(JsonNode raiz) {
        String tag = raiz.path("tag_name").asText(null);
        if (tag == null) {
            return null;
        }
        //strip a leading "v" if present (tags are "v1.2.5", we just want "1.2.5")
        return tag.startsWith("v") ? tag.substring(1) : tag;
    }

    private static boolean esMasNueva(String remota, String actual) {
        String[] r = remota.split("\\.");
        String[] a = actual.split("\\.");
        int longitud = Math.max(r.length, a.length);
        for (int i = 0; i < longitud; i++) {
            int vr = i < r.length ? Integer.parseInt(r[i]) : 0;
            int va = i < a.length ? Integer.parseInt(a[i]) : 0;
            if (vr != va) {
                return vr > va;
            }
        }
        return false;
    }

    /**
     * Matches release asset filenames against this JVM's OS/architecture.
     * Expects filenames built by the CI workflow to contain "windows",
     * "mac-applesilicon", or "mac-intel". On Windows the .msi is preferred
     * (it can install silently); the .exe is only a fallback.
     */
    private static Asset buscarAssetParaEstePlataforma(JsonNode raiz) {
        String sufijo = detectarSufijoDePlataforma();
        if (sufijo == null) {
            return null;
        }

        Asset alternativa = null;
        for (JsonNode assetNode : raiz.path("assets")) {
            String nombreArchivo = assetNode.path("name").asText("");
            String url = assetNode.path("browser_download_url").asText(null);
            if (!nombreArchivo.contains(sufijo) || url == null) {
                continue;
            }
            String minusculas = nombreArchivo.toLowerCase();
            if (minusculas.endsWith(".msi") || minusculas.endsWith(".dmg")) {
                return new Asset(nombreArchivo, url);
            }
            if (alternativa == null && minusculas.endsWith(".exe")) {
                alternativa = new Asset(nombreArchivo, url);
            }
        }
        return alternativa;
    }

    private static String detectarSufijoDePlataforma() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();

        if (osName.contains("win")) {
            return "windows";
        }
        if (osName.contains("mac")) {
            boolean esAppleSilicon = osArch.contains("aarch64") || osArch.contains("arm");
            return esAppleSilicon ? "mac-applesilicon" : "mac-intel";
        }
        return null; //unrecognized OS (e.g. Linux)
    }
}
