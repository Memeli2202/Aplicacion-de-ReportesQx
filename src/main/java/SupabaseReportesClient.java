import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class SupabaseReportesClient {
    private static final String SUPABASE_URL = "https://hekiisuiehxoinqqieum.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_cPrGduWF7C8Et4WOyCsJRA_4Cy2AtYt";
    private static final String BUCKET = "imagenes-reportes";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public static class ResumenBorrador {
        public final String id;
        public final String nombre;
        public final String cedula;
        public final String fecha;
        public final String estado;

        public ResumenBorrador(String id, String nombre, String cedula, String fecha, String estado) {
            this.id = id;
            this.nombre = nombre;
            this.cedula = cedula;
            this.fecha = fecha;
            this.estado = estado;
        }

        @Override
        public String toString() {
            return nombre + " - " + cedula + " (" + fecha + ") [" + estado + "]";
        }
    }

    public static class ResultadoCarga {
        public final Reporte reporte;
        public final List<DialogoImagenes.ImagenComentario> imagenes;

        public ResultadoCarga(Reporte reporte, List<DialogoImagenes.ImagenComentario> imagenes) {
            this.reporte = reporte;
            this.imagenes = imagenes;
        }
    }

    /**
     * Sends a request built from the session's current access token; if the
     * response indicates an expired JWT, refreshes the session in place and
     * retries once with the new token. Every API call in this class routes
     * through this (or its byte[] counterpart below) instead of sending
     * directly, so a long-running app session doesn't start failing once
     * the access token's ~1 hour lifetime runs out mid-use.
     */
    private static HttpResponse<String> enviarConReintento(SesionSupabase sesion, Function<String, HttpRequest> construirRequest)
            throws IOException, InterruptedException {

        HttpRequest request = construirRequest.apply(sesion.accessToken);
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (tokenExpirado(response.statusCode(), response.body()) && SupabaseAuthClient.refrescarEnSitio(sesion)) {
            request = construirRequest.apply(sesion.accessToken);
            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        }

        return response;
    }

    private static HttpResponse<byte[]> enviarConReintentoBytes(SesionSupabase sesion, Function<String, HttpRequest> construirRequest)
            throws IOException, InterruptedException {

        HttpRequest request = construirRequest.apply(sesion.accessToken);
        HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 401 && SupabaseAuthClient.refrescarEnSitio(sesion)) {
            request = construirRequest.apply(sesion.accessToken);
            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        return response;
    }

    private static boolean tokenExpirado(int statusCode, String body) {
        return statusCode == 401 || (body != null && body.contains("PGRST303"));
    }

    /**
     * Saves the report (insert if reporte.getId() is null, update otherwise)
     * and replaces all of its images. On a successful first save,
     * reporte.setId(...) is set so later saves update the same row.
     */
    public static void guardarReporte(SesionSupabase sesion, Reporte reporte, String estado,
                                      List<DialogoImagenes.ImagenComentario> imagenes) throws IOException, InterruptedException {

        ObjectNode cuerpo = MAPPER.createObjectNode();
        cuerpo.put("doctor_id", sesion.userId);
        cuerpo.put("estado", estado);
        cuerpo.put("fecha", reporte.getFecha());
        cuerpo.put("nombre", reporte.getNombre());
        cuerpo.put("edad", reporte.getEdad());
        cuerpo.put("cedula", reporte.getCedula());
        cuerpo.put("ars", reporte.getArs());
        cuerpo.put("procedimiento", reporte.getProcedimiento());
        cuerpo.put("enzian_p", reporte.getEnzianP());
        cuerpo.put("enzian_o", reporte.getEnzianO());
        cuerpo.put("enzian_o2", reporte.getEnzianO2());
        cuerpo.put("enzian_t", reporte.getEnzianT());
        cuerpo.put("enzian_t2", reporte.getEnzianT2());
        cuerpo.put("enzian_a", reporte.getEnzianA());
        cuerpo.put("enzian_b", reporte.getEnzianB());
        cuerpo.put("enzian_b2", reporte.getEnzianB2());
        cuerpo.put("enzian_c", reporte.getEnzianC());
        cuerpo.put("enzian_f", reporte.getEnzianF());
        cuerpo.put("resumen_qx", reporte.getResumenQx());
        cuerpo.put("post_qx", reporte.getPostQx());

        String id = reporte.getId();
        if (id == null) {
            id = insertarReporte(sesion, cuerpo);
            reporte.setId(id);
        } else {
            actualizarReporte(sesion, id, cuerpo);
        }

        reemplazarImagenes(sesion, id, imagenes);
    }

    private static String insertarReporte(SesionSupabase sesion, ObjectNode cuerpo) throws IOException, InterruptedException {
        String cuerpoJson = MAPPER.writeValueAsString(cuerpo);

        HttpResponse<String> response = enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/reportes"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson))
                .build());

        if (response.statusCode() >= 300) {
            throw new IOException("Error al guardar el reporte: " + response.body());
        }
        JsonNode json = MAPPER.readTree(response.body());
        return json.get(0).get("id").asText();
    }

    private static void actualizarReporte(SesionSupabase sesion, String id, ObjectNode cuerpo) throws IOException, InterruptedException {
        String cuerpoJson = MAPPER.writeValueAsString(cuerpo);

        HttpResponse<String> response = enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/reportes?id=eq." + id))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(cuerpoJson))
                .build());

        if (response.statusCode() >= 300) {
            throw new IOException("Error al actualizar el reporte: " + response.body());
        }
    }

    /**
     * Replaces every image for this report: deletes the old rows + storage
     * files first, then re-uploads the current set. Simple and always
     * correct, at the cost of re-uploading unchanged images on every save.
     */
    private static void reemplazarImagenes(SesionSupabase sesion, String reporteId, List<DialogoImagenes.ImagenComentario> imagenes)
            throws IOException, InterruptedException {

        eliminarImagenesExistentes(sesion, reporteId);

        int orden = 1;
        for (DialogoImagenes.ImagenComentario ic : imagenes) {
            String path = sesion.userId + "/" + reporteId + "/" + orden + ".jpg";
            subirImagen(sesion, path, ic.getImagen());

            ObjectNode fila = MAPPER.createObjectNode();
            fila.put("reporte_id", reporteId);
            fila.put("orden", orden);
            fila.put("comentario", ic.getComentario());
            fila.put("storage_path", path);
            String filaJson = MAPPER.writeValueAsString(fila);

            HttpResponse<String> response = enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                    .uri(URI.create(SUPABASE_URL + "/rest/v1/reporte_imagenes"))
                    .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(filaJson))
                    .build());

            if (response.statusCode() >= 300) {
                throw new IOException("Error al guardar imagen " + orden + ": " + response.body());
            }
            orden++;
        }
    }

    private static void eliminarImagenesExistentes(SesionSupabase sesion, String reporteId) throws IOException, InterruptedException {
        HttpResponse<String> respuestaConsulta = enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/reporte_imagenes?reporte_id=eq." + reporteId + "&select=storage_path"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());

        JsonNode filas = MAPPER.readTree(respuestaConsulta.body());

        if (filas.isArray() && !filas.isEmpty()) {
            List<String> rutas = new ArrayList<>();
            for (JsonNode fila : filas) {
                rutas.add(fila.get("storage_path").asText());
            }
            eliminarArchivosStorage(sesion, rutas);
        }

        enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/reporte_imagenes?reporte_id=eq." + reporteId))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build());
    }

    private static void eliminarArchivosStorage(SesionSupabase sesion, List<String> rutas) throws IOException, InterruptedException {
        ObjectNode cuerpo = MAPPER.createObjectNode();
        ArrayNode prefijos = cuerpo.putArray("prefixes");
        rutas.forEach(prefijos::add);
        String cuerpoJson = MAPPER.writeValueAsString(cuerpo);

        enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1/object/" + BUCKET))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(cuerpoJson))
                .build());
    }

    private static void subirImagen(SesionSupabase sesion, String path, BufferedImage imagen) throws IOException, InterruptedException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(imagen, "jpg", os);
        byte[] bytes = os.toByteArray();

        HttpResponse<String> response = enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + path))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "image/jpeg")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build());

        if (response.statusCode() >= 300) {
            throw new IOException("Error al subir imagen: " + response.body());
        }
    }

    public static List<ResumenBorrador> listarReportes(SesionSupabase sesion) throws IOException, InterruptedException {
        HttpResponse<String> response = enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/reportes?select=id,nombre,cedula,fecha,estado&order=updated_at.desc"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());

        if (response.statusCode() >= 300) {
            throw new IOException("Error al cargar la lista de reportes: " + response.body());
        }

        List<ResumenBorrador> resultado = new ArrayList<>();
        JsonNode json = MAPPER.readTree(response.body());
        for (JsonNode fila : json) {
            resultado.add(new ResumenBorrador(
                    fila.get("id").asText(),
                    texto(fila, "nombre"),
                    texto(fila, "cedula"),
                    texto(fila, "fecha"),
                    texto(fila, "estado")
            ));
        }
        return resultado;
    }

    public static ResultadoCarga cargarReporte(SesionSupabase sesion, String id) throws IOException, InterruptedException {
        HttpResponse<String> response = enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/reportes?id=eq." + id + "&select=*"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());

        if (response.statusCode() >= 300) {
            throw new IOException("Error al cargar el reporte: " + response.body());
        }
        JsonNode filas = MAPPER.readTree(response.body());
        if (!filas.isArray() || filas.isEmpty()) {
            throw new IOException("Reporte no encontrado");
        }
        JsonNode fila = filas.get(0);

        Reporte reporte = new Reporte();
        reporte.setId(fila.get("id").asText());
        reporte.setFecha(texto(fila, "fecha"));
        reporte.setNombre(texto(fila, "nombre"));
        reporte.setEdad(texto(fila, "edad"));
        reporte.setCedula(texto(fila, "cedula"));
        reporte.setArs(texto(fila, "ars"));
        reporte.setProcedimiento(texto(fila, "procedimiento"));
        reporte.setEnzianP(texto(fila, "enzian_p"));
        reporte.setEnzianO(texto(fila, "enzian_o"));
        reporte.setEnzianO2(texto(fila, "enzian_o2"));
        reporte.setEnzianT(texto(fila, "enzian_t"));
        reporte.setEnzianT2(texto(fila, "enzian_t2"));
        reporte.setEnzianA(texto(fila, "enzian_a"));
        reporte.setEnzianB(texto(fila, "enzian_b"));
        reporte.setEnzianB2(texto(fila, "enzian_b2"));
        reporte.setEnzianC(texto(fila, "enzian_c"));
        reporte.setEnzianF(texto(fila, "enzian_f"));
        reporte.setResumenQx(texto(fila, "resumen_qx"));
        reporte.setPostQx(texto(fila, "post_qx"));

        List<DialogoImagenes.ImagenComentario> imagenes = cargarImagenesDeReporte(sesion, id);
        return new ResultadoCarga(reporte, imagenes);
    }

    private static List<DialogoImagenes.ImagenComentario> cargarImagenesDeReporte(SesionSupabase sesion, String reporteId)
            throws IOException, InterruptedException {

        HttpResponse<String> response = enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/reporte_imagenes?reporte_id=eq." + reporteId + "&select=orden,comentario,storage_path&order=orden.asc"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());

        if (response.statusCode() >= 300) {
            throw new IOException("Error al cargar las imágenes: " + response.body());
        }

        List<DialogoImagenes.ImagenComentario> resultado = new ArrayList<>();
        JsonNode filas = MAPPER.readTree(response.body());
        for (JsonNode fila : filas) {
            String path = fila.get("storage_path").asText();
            String comentario = texto(fila, "comentario");
            BufferedImage imagen = descargarImagen(sesion, path);
            if (imagen != null) {
                resultado.add(new DialogoImagenes.ImagenComentario(imagen, comentario));
            }
        }
        return resultado;
    }

    private static BufferedImage descargarImagen(SesionSupabase sesion, String path) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = enviarConReintentoBytes(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + path))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());

        if (response.statusCode() >= 300) {
            return null; //skip a missing/failed image rather than failing the whole load
        }
        return ImageIO.read(new ByteArrayInputStream(response.body()));
    }

    private static String texto(JsonNode fila, String campo) {
        JsonNode valor = fila.get(campo);
        return (valor == null || valor.isNull()) ? "" : valor.asText();
    }
}