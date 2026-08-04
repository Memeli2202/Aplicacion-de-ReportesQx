import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * Talks to Supabase's Auth REST API directly (there's no official Java SDK).
 * Every installed copy of the app shares these same URL/key - what makes
 * each doctor's access distinct is their own email/password login, not a
 * per-install credential.
 *
 * IMPORTANT: replace SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY below with your
 * actual project's values (Supabase dashboard > Project Settings > API Keys).
 * The publishable key is meant to be public/embedded in client apps - it's
 * Row Level Security (see supabase_schema.sql) that actually restricts
 * access, not secrecy of this key. Never use the secret key here - it
 * bypasses RLS entirely and must only run on a machine you control.
 */
public class SupabaseAuthClient {

    private static final String SUPABASE_URL = "https://hekiisuiehxoinqqieum.supabase.co"; // <-- update this
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_cPrGduWF7C8Et4WOyCsJRA_4Cy2AtYt"; // <-- update this (Project Settings > API Keys)

    private static final Path ARCHIVO_SESION = Path.of(System.getProperty("user.home"), ".desktopreportbuilder", "session.properties");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static SesionSupabase iniciarSesion(String email, String password) throws IOException, InterruptedException {
        String cuerpo = MAPPER.writeValueAsString(Map.of("email", email, "password", password));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/auth/v1/token?grant_type=password"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = MAPPER.readTree(response.body());

        if (response.statusCode() != 200) {
            throw new IOException(mensajeDeError(json, "Error al iniciar sesión"));
        }

        SesionSupabase sesion = construirSesion(json);
        cargarPerfil(sesion);
        guardarSesion(sesion);
        return sesion;
    }

    /**
     * Registers a new doctor account. Role always starts as 'doctor' - that's
     * enforced by the database trigger, not by anything client-side, so this
     * can't be bypassed by editing the app. Returns null if Supabase requires
     * email confirmation before the account can log in (check your project's
     * Auth settings if you'd rather skip that for an internal clinic tool).
     */
    public static SesionSupabase registrarse(String email, String password, String nombre) throws IOException, InterruptedException {
        String cuerpo = MAPPER.writeValueAsString(Map.of(
                "email", email,
                "password", password,
                "data", Map.of("nombre", nombre)
        ));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/auth/v1/signup"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = MAPPER.readTree(response.body());

        if (response.statusCode() != 200) {
            throw new IOException(mensajeDeError(json, "Error al registrarse"));
        }

        if (json.has("access_token") && !json.get("access_token").isNull()) {
            SesionSupabase sesion = construirSesion(json);
            sesion.nombre = nombre;
            sesion.rol = "doctor";
            guardarSesion(sesion);
            return sesion;
        }
        return null; //account created, but needs email confirmation first
    }

    /**
     * Tries to restore a previously saved session using the stored refresh
     * token. Returns null if there's no saved session or it's no longer
     * valid - the caller should fall back to showing the login dialog.
     */
    public static SesionSupabase restaurarSesion() {
        try {
            if (!Files.exists(ARCHIVO_SESION)) {
                return null;
            }
            Properties props = new Properties();
            try (var in = Files.newInputStream(ARCHIVO_SESION)) {
                props.load(in);
            }
            String refreshToken = props.getProperty("refresh_token");
            if (refreshToken == null || refreshToken.isBlank()) {
                return null;
            }
            return refrescarSesion(refreshToken);
        } catch (Exception ex) {
            return null;
        }
    }

    public static void cerrarSesion() {
        try {
            Files.deleteIfExists(ARCHIVO_SESION);
        } catch (IOException ignored) {
        }
    }

    private static SesionSupabase refrescarSesion(String refreshToken) throws IOException, InterruptedException {
        String cuerpo = MAPPER.writeValueAsString(Map.of("refresh_token", refreshToken));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token"))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        JsonNode json = MAPPER.readTree(response.body());
        SesionSupabase sesion = construirSesion(json);
        cargarPerfil(sesion);
        guardarSesion(sesion);
        return sesion;
    }

    private static SesionSupabase construirSesion(JsonNode json) {
        return new SesionSupabase(
                json.get("access_token").asText(),
                json.get("refresh_token").asText(),
                json.get("user").get("id").asText(),
                json.get("user").get("email").asText()
        );
    }

    private static void cargarPerfil(SesionSupabase sesion) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SUPABASE_URL + "/rest/v1/perfiles?id=eq." + sesion.userId + "&select=nombre,rol"))
                    .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer " + sesion.accessToken)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = MAPPER.readTree(response.body());
            if (json.isArray() && !json.isEmpty()) {
                sesion.nombre = json.get(0).get("nombre").asText();
                sesion.rol = json.get(0).get("rol").asText();
            }
        } catch (Exception ignored) {
            //non-fatal - session still works, just without the cached display name/role
        }
    }

    private static void guardarSesion(SesionSupabase sesion) {
        try {
            Files.createDirectories(ARCHIVO_SESION.getParent());
            Properties props = new Properties();
            props.setProperty("refresh_token", sesion.refreshToken);
            try (var out = Files.newOutputStream(ARCHIVO_SESION)) {
                props.store(out, "Sesion de Desktop Report Builder - no compartir este archivo");
            }
        } catch (IOException ignored) {
            //non-fatal - just means the doctor will need to log in again next time
        }
    }

    private static String mensajeDeError(JsonNode json, String porDefecto) {
        if (json.has("error_description")) {
            return json.get("error_description").asText();
        }
        if (json.has("msg")) {
            return json.get("msg").asText();
        }
        return porDefecto;
    }
}