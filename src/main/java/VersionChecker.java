import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * check GitHub for newer version
 */
public class VersionChecker {
    //bump this by hand whenever you tag and publish a new release
    public static final String CURRENT_VERSION = "1.0.23";

    private static final String REPO = "Memeli2202/Aplicacion-de-ReportesQx";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String RELEASES_PAGE = "https://github.com/" + REPO + "/releases/latest";

    /**
     * Kicks off a background check; safe to call from the EDT on startup.
     * Does nothing visible if there's no update, no internet, or the
     * GitHub API is unreachable - an update check should never interrupt
     * or break normal use of the app.
     */
    public static void verificarActualizacion(Component parent) {
        new SwingWorker<String, Void>() {
            private String urlDescarga;

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
                        return null;
                    }
                    String body = response.body();
                    String ultimaVersion = extraerTagName(body);
                    if (ultimaVersion == null || !esMasNueva(ultimaVersion, CURRENT_VERSION)) {
                        return null;
                    }
                    //pick the one asset matching this user's OS/architecture, if we can find it
                    urlDescarga = buscarAssetParaEstePlataforma(body);
                    return ultimaVersion;
                } catch (Exception ex) {
                    //offline, DNS failure, GitHub down, rate-limited, etc. - fail silently
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    String ultimaVersion = get();
                    if (ultimaVersion != null) {
                        mostrarAvisoActualizacion(parent, ultimaVersion, urlDescarga);
                    }
                } catch (Exception ignored) {
                    //never let a failed update check surface as an error to the user
                }
            }
        }.execute();
    }

    private static String extraerTagName(String json) {
        Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([\\d.]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
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
    private static String buscarAssetParaEstePlataforma(String json) {
        String sufijo = detectarSufijoDePlataforma();
        if (sufijo == null) {
            return null;
        }

        Matcher m = Pattern.compile(
                "\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\""
        ).matcher(json);

        while (m.find()) {
            String nombreArchivo = m.group(1);
            String url = m.group(2);
            if (nombreArchivo.contains(sufijo)) {
                return url;
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
        return null; //unrecognized OS (e.g. Linux) - fall back to the releases page
    }

    private static void mostrarAvisoActualizacion(Component parent, String nuevaVersion, String urlDescarga) {
        String mensaje = "Hay una nueva versión disponible (" + nuevaVersion + ").\n\n"
                + "Al aceptar, se abrirá la descarga en tu navegador. Una vez descargado el archivo,\n"
                + "ábrelo y sigue las instrucciones del instalador.\n\n"
                + "Nota: como esta aplicación no está registrada con Windows/Apple, es normal que\n"
                + "aparezca una advertencia de seguridad. Puedes continuar seleccionando\n"
                + "\"Más información\" > \"Ejecutar de todas formas\" (Windows) o \"Abrir de todas formas\"\n"
                + "desde Preferencias del Sistema (Mac).";

        int opcion = JOptionPane.showConfirmDialog(parent, mensaje,
                "Actualización disponible",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (opcion == JOptionPane.YES_OPTION) {
            String destino = urlDescarga != null ? urlDescarga : RELEASES_PAGE;
            try {
                Desktop.getDesktop().browse(URI.create(destino));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parent, "No se pudo abrir el navegador: " + ex.getMessage());
            }
        }
    }
}
