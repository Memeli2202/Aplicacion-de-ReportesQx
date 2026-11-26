import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Patients shared by the whole clinic. Every patient has its own UID
 * (pacientes.id) and every report points to its patient through
 * reportes.paciente_id. A patient is identified by its "documento" - a
 * cedula OR a passport number, both typed in the same form field.
 */
public class PacientesClient {

    private static final String SUPABASE_URL = "https://hekiisuiehxoinqqieum.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_cPrGduWF7C8Et4WOyCsJRA_4Cy2AtYt";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String COLUMNAS = "select=id,nombre,documento,tipo_documento";

    public static class Paciente {
        public final String id;
        public final String nombre;
        public final String documento; //stored form (no dashes), never null, "" when the patient has no document
        public final String tipoDocumento; //"cedula" or "pasaporte"

        public Paciente(String id, String nombre, String documento, String tipoDocumento) {
            this.id = id;
            this.nombre = nombre;
            this.documento = documento == null ? "" : documento;
            this.tipoDocumento = Documento.normalizarTipo(tipoDocumento);
        }

        @Override
        public String toString() {
            return documento.isEmpty() ? nombre : nombre + "  -  " + Documento.paraMostrar(tipoDocumento, documento);
        }
    }

    /**
     * Same normalization the database uses for documento_norm: only letters
     * and digits, lower case - so "001-1234567-8" and "00112345678" match.
     */
    public static String normalizar(String documento) {
        return documento == null ? "" : documento.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    /**
     * Search-as-you-type: patients whose name or document contains the text.
     * Returns at most 8, ordered by name. Empty for very short input.
     */
    public static List<Paciente> buscar(SesionSupabase sesion, String texto) throws IOException, InterruptedException {
        String limpio = texto == null ? "" : texto.trim().replaceAll("[*,()\"\\\\%]", "");
        if (limpio.length() < 2) {
            return new ArrayList<>();
        }

        StringBuilder filtro = new StringBuilder("(nombre.ilike.*").append(limpio).append("*,documento.ilike.*").append(limpio).append("*");
        String norm = normalizar(limpio);
        if (!norm.isEmpty()) {
            filtro.append(",documento_norm.ilike.*").append(norm).append("*");
        }
        filtro.append(")");

        return consultar(sesion, "or=" + codificar(filtro.toString()) + "&order=nombre.asc&limit=8");
    }

    /**
     * Makes sure the report has a patient and returns that patient's UID.
     * - already linked (picked from the search box, or loaded from the cloud) -> kept as is
     * - has a document -> the existing patient with that document, or a new one
     * - no document -> an existing document-less patient with the same name, or a new one
     * - no document and no name -> null (nothing to identify a patient by)
     */
    public static String asegurarPaciente(SesionSupabase sesion, Reporte reporte) throws IOException, InterruptedException {
        if (reporte.getPacienteId() != null && !reporte.getPacienteId().isBlank()) {
            return reporte.getPacienteId();
        }

        String nombre = reporte.getNombre() == null ? "" : reporte.getNombre().trim();
        String tipo = reporte.getTipoDocumento();
        String documento = Documento.paraGuardar(tipo, reporte.getCedula());
        String norm = normalizar(documento);

        if (norm.isEmpty()) {
            if (nombre.isEmpty()) {
                return null;
            }
            for (Paciente p : consultar(sesion, "documento_norm=is.null&nombre=ilike." + codificar(nombre.replaceAll("[*,()\"\\\\%]", "")) + "&limit=20")) {
                if (p.nombre.trim().equalsIgnoreCase(nombre)) {
                    return p.id;
                }
            }
            return crear(sesion, nombre, null, tipo).id;
        }

        List<Paciente> existentes = consultar(sesion, "documento_norm=eq." + norm + "&limit=1");
        if (!existentes.isEmpty()) {
            return existentes.get(0).id;
        }

        try {
            return crear(sesion, nombre.isEmpty() ? "(sin nombre)" : nombre, documento, tipo).id;
        } catch (IOException e) {
            //another doctor may have created the same patient a moment ago (the unique
            //index on documento_norm rejects the duplicate) - use theirs instead
            existentes = consultar(sesion, "documento_norm=eq." + norm + "&limit=1");
            if (!existentes.isEmpty()) {
                return existentes.get(0).id;
            }
            throw e;
        }
    }

    private static Paciente crear(SesionSupabase sesion, String nombre, String documento, String tipo) throws IOException, InterruptedException {
        ObjectNode cuerpo = MAPPER.createObjectNode();
        cuerpo.put("nombre", nombre);
        cuerpo.put("tipo_documento", Documento.normalizarTipo(tipo));
        if (documento != null && !documento.isBlank()) {
            cuerpo.put("documento", documento);
        }

        HttpResponse<String> response = SupabaseReportesClient.enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/pacientes"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString()))
                .build());

        if (response.statusCode() >= 300) {
            throw new IOException("Error al crear el paciente: " + response.body());
        }
        List<Paciente> creados = leer(response.body());
        if (creados.isEmpty()) {
            throw new IOException("No se pudo crear el paciente");
        }
        return creados.get(0);
    }

    private static List<Paciente> consultar(SesionSupabase sesion, String filtros) throws IOException, InterruptedException {
        HttpResponse<String> response = SupabaseReportesClient.enviarConReintento(sesion, token -> HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/pacientes?" + COLUMNAS + "&" + filtros))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());

        if (response.statusCode() >= 300) {
            throw new IOException("Error al buscar pacientes: " + response.body());
        }
        return leer(response.body());
    }

    private static List<Paciente> leer(String json) throws IOException {
        List<Paciente> resultado = new ArrayList<>();
        for (JsonNode fila : MAPPER.readTree(json)) {
            resultado.add(new Paciente(
                    fila.get("id").asText(),
                    fila.path("nombre").asText(""),
                    fila.path("documento").isNull() ? "" : fila.path("documento").asText(""),
                    fila.path("tipo_documento").asText("cedula")));
        }
        return resultado;
    }

    //'*' must reach PostgREST as the wildcard, URLEncoder leaves it alone
    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
