/**
 * Holds the tokens and identity for a logged-in doctor. nombre/rol are
 * filled in from the perfiles table right after authentication.
 *
 * accessToken/refreshToken are mutable (not final) specifically so that
 * SupabaseAuthClient.refrescarEnSitio(sesion) can update them in place when
 * the access token expires mid-session - every class holding a reference
 * to this same object then automatically sees the refreshed token, without
 * needing to thread a new SesionSupabase instance back through the app.
 */
public class SesionSupabase {
    public String accessToken;
    public String refreshToken;
    public final String userId;
    public final String email;
    public String nombre;
    public String rol;

    public SesionSupabase(String accessToken, String refreshToken, String userId, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
    }

    public boolean esAdmin() {
        return "admin".equals(rol);
    }
}