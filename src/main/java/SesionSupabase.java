public class SesionSupabase {
    public final String accessToken;
    public final String refreshToken;
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
