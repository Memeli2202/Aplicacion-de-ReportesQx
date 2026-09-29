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

/**
 * check GitHub for newer version
 *
 * How the actual install happens (a running app can't overwrite its own
 *  * files, so this can't be fully invisible):
 *  *   1. Download the installer to a temp file.
 *  *   2. Write a tiny helper script that waits a couple seconds (giving this
 *  *      JVM time to fully exit), then runs the installer silently.
 *  *   3. Launch that helper script as an independent process, then exit.
 *
 */
public class VersionChecker {

    //bump this by hand whenever you tag and publish a new release
    public static final String CURRENT_VERSION = "1.2.6";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String REPO = "Memeli2202/Aplicacion-de-ReportesQx"; // <-- update this
    private static final String APP_NAME = "DesktopReportBuilder"; // <-- must match jpackage --name
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
                        confirmarYDescargar(parent, ultimaVersion, asset);
                    }
                } catch (Exception ex) {
                    //never let a failed update check surface as an error to the user
                    registrar("Excepcion inesperada en done(): " + ex);
                }
            }
        }.execute();
    }

    /**
     * TEMPORARY diagnostic logging so we can see exactly why an update
     * alert isn't showing up, without needing a terminal attached to the
     * packaged app. Writes to ~/.doctorhelper/update_check.log. Safe to
     * remove once the root cause is confirmed.
     */
    private static void registrar(String mensaje) {
        try {
            Path carpeta = Path.of(System.getProperty("user.home"), ".doctorhelper");
            Files.createDirectories(carpeta);
            Path log = carpeta.resolve("update_check.log");
            String linea = LocalDateTime.now() + " - " + mensaje + System.lineSeparator();
            Files.writeString(log, linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            //logging itself must never break the app
        }
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

    private static void confirmarYDescargar(Component parent, String nuevaVersion, Asset asset) {
        int opcion = JOptionPane.showConfirmDialog(parent,
                "Hay una nueva versión disponible (" + nuevaVersion + ").\n¿Deseas descargarla ahora?",
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
                    JOptionPane.showMessageDialog(parent, "No se pudo descargar la actualización: " + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    Path archivo = get();
                    int reiniciar = JOptionPane.showConfirmDialog(parent,
                            "Actualización lista. ¿Reiniciar ahora para instalarla?",
                            "Actualización lista",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);
                    if (reiniciar == JOptionPane.YES_OPTION) {
                        aplicarActualizacion(archivo);
                    }
                } catch (Exception ignored) {
                }
            }
        };

        worker.execute();
        progreso.setVisible(true);
    }

    private static Path descargarArchivo(Asset asset) throws IOException, InterruptedException {
        Path destino = Files.createTempFile("update_", "_" + asset.nombre);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(asset.url))
                .timeout(Duration.ofMinutes(5)) //installers can be sizable
                .GET()
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofFile(destino));
        return destino;
    }

    /**
     * Writes and launches a small helper script that waits for this JVM to
     * fully exit, then installs the update - a running app can't safely
     * overwrite its own currently-loaded files, so this indirection is
     * necessary on both platforms.
     */
    private static void aplicarActualizacion(Path archivoDescargado) {
        try {
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (osName.contains("win")) {
                aplicarActualizacionWindows(archivoDescargado);
            } else if (osName.contains("mac")) {
                aplicarActualizacionMac(archivoDescargado);
            } else {
                JOptionPane.showMessageDialog(null, "Actualización automática no disponible para este sistema operativo.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "No se pudo iniciar la actualización: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void aplicarActualizacionWindows(Path instalador) throws IOException {
        Path script = Files.createTempFile("actualizar_", ".bat");
        String contenido = "@echo off\r\n"
                + "timeout /t 2 /nobreak >nul\r\n"
                + "\"" + instalador.toAbsolutePath() + "\" /quiet\r\n";
        Files.writeString(script, contenido);

        new ProcessBuilder("cmd", "/c", "start", "", script.toAbsolutePath().toString()).start();
        System.exit(0);
    }

    private static void aplicarActualizacionMac(Path dmg) throws IOException {
        Path script = Files.createTempFile("actualizar_", ".sh");
        String contenido = "#!/bin/bash\n"
                + "sleep 2\n"
                + "MOUNT_DIR=$(hdiutil attach \"" + dmg.toAbsolutePath() + "\" -nobrowse -noautoopen | tail -1 | awk '{print $NF}')\n"
                + "rm -rf \"/Applications/" + APP_NAME + ".app\"\n"
                + "cp -R \"$MOUNT_DIR/" + APP_NAME + ".app\" /Applications/\n"
                + "hdiutil detach \"$MOUNT_DIR\" -quiet\n"
                + "open \"/Applications/" + APP_NAME + ".app\"\n";
        Files.writeString(script, contenido);
        //noinspection ResultOfMethodCallIgnored
        script.toFile().setExecutable(true);

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
     * "mac-applesilicon", or "mac-intel" - matching that workflow's
     * platform suffix convention.
     */
    private static Asset buscarAssetParaEstePlataforma(JsonNode raiz) {
        String sufijo = detectarSufijoDePlataforma();
        if (sufijo == null) {
            return null;
        }

        for (JsonNode assetNode : raiz.path("assets")) {
            String nombreArchivo = assetNode.path("name").asText("");
            if (nombreArchivo.contains(sufijo)) {
                String url = assetNode.path("browser_download_url").asText(null);
                if (url != null) {
                    return new Asset(nombreArchivo, url);
                }
            }
        }
        return null;
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