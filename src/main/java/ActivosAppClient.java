import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


public class ActivosAppClient {

    private static final String SUPABASE_URL = "https://hekiisuiehxoinqqieum.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_cPrGduWF7C8Et4WOyCsJRA_4Cy2AtYt";
    private static final String BUCKET = "activos-app";

    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static class Activos {
        public final ImageData logo;
        public final ImageData sello;
        public final ImageData marcaAgua;

        public Activos(ImageData logo, ImageData sello, ImageData marcaAgua) {
            this.logo = logo;
            this.sello = sello;
            this.marcaAgua = marcaAgua;
        }
    }

    public static Activos cargarActivos(SesionSupabase sesion) throws IOException, InterruptedException {
        ImageData logo = descargarImagen(sesion, "logo.png");
        ImageData sello = descargarImagen(sesion, "sello.png");
        ImageData marcaAgua = descargarImagen(sesion, "marca_agua.png");
        return new Activos(logo, sello, marcaAgua);
    }

    private static ImageData descargarImagen(SesionSupabase sesion, String nombreArchivo) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + nombreArchivo))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + sesion.accessToken)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 300) {
            return null; //missing/failed asset shouldn't block report generation - just skipped, same as the old classpath fallback did
        }
        return ImageDataFactory.create(response.body());
    }
}