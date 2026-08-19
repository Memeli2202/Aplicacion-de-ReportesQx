import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches the clinic's shared branding assets (logo, signature stamp,
 * watermark) from a private Supabase Storage bucket at runtime, instead of
 * bundling them as classpath resources - keeps the doctor's actual
 * signature image out of the public GitHub repo entirely.
 *
 * Uses signed URLs (POST /object/sign/... then GET the result) rather than
 * the direct authenticated object GET. This was originally a workaround for
 * a Supabase-side Storage bug (DatabaseInvalidObjectDefinition errors on
 * both access paths) that has since resolved itself - kept as-is since it
 * works reliably and there's no reason to switch back.
 *
 * IMPORTANT before this works:
 * 1. Create a PRIVATE bucket named "activos-app" in Supabase Storage.
 * 2. Upload logo.png, sello.png, and marca_agua.png there directly via
 *    the dashboard (Storage > activos-app > Upload).
 * 3. Run the storage policy in supabase_schema.sql.
 */
public class ActivosAppClient {

    private static final String SUPABASE_URL = "https://hekiisuiehxoinqqieum.supabase.co"; // <-- update this (same as the other Supabase clients)
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_cPrGduWF7C8Et4WOyCsJRA_4Cy2AtYt"; // <-- update this
    private static final String BUCKET = "activos-app";

    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        String signedUrlPath = obtenerUrlFirmada(sesion, nombreArchivo);
        if (signedUrlPath == null) {
            return null;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1" + signedUrlPath))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 300) {
            return null;
        }
        return ImageDataFactory.create(response.body());
    }

    private static String obtenerUrlFirmada(SesionSupabase sesion, String nombreArchivo) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1/object/sign/" + BUCKET + "/" + nombreArchivo))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + sesion.accessToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString("{\"expiresIn\": 3600}"))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            return null;
        }

        JsonNode json = MAPPER.readTree(response.body());
        return json.has("signedURL") ? json.get("signedURL").asText() : null;
    }
}