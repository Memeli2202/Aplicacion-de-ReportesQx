/**
 * Everything about a patient's identity document in one place: the two
 * types (cedula / passport), how each one is typed and displayed, and how it
 * is stored.
 *
 * - Stored in the database WITHOUT formatting: a cedula as 11 digits
 *   ("00112345678"), a passport as upper-case letters+digits ("AB123456").
 * - Shown to people (form, PDF, lists) formatted: a cedula as 000-0000000-0.
 */
public final class Documento {

    public static final String CEDULA = "cedula";
    public static final String PASAPORTE = "pasaporte";

    public static final int LARGO_CEDULA = 11;
    public static final int LARGO_MAX_PASAPORTE = 20;

    private Documento() {}

    /** Anything that isn't the passport type is treated as a cedula (also covers old rows with no type). */
    public static String normalizarTipo(String tipo) {
        return PASAPORTE.equals(tipo) ? PASAPORTE : CEDULA;
    }

    /** Label shown next to the value, e.g. in the PDF. */
    public static String etiqueta(String tipo) {
        return PASAPORTE.equals(tipo) ? "Pasaporte" : "Cédula";
    }

    /** The value exactly as it must be stored: no dashes or spaces. */
    public static String paraGuardar(String tipo, String texto) {
        if (texto == null) {
            return "";
        }
        if (PASAPORTE.equals(tipo)) {
            String limpio = texto.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            return limpio.length() > LARGO_MAX_PASAPORTE ? limpio.substring(0, LARGO_MAX_PASAPORTE) : limpio;
        }
        String digitos = texto.replaceAll("[^0-9]", "");
        return digitos.length() > LARGO_CEDULA ? digitos.substring(0, LARGO_CEDULA) : digitos;
    }

    /**
     * The value as people read it. For a cedula the dashes appear as the
     * digits are typed (001, 001-1, 001-1234567, 001-1234567-8); a passport
     * is shown as stored. Works on partial input, so it is what the form's
     * field uses while typing, and on stored values, so it is also what the
     * PDF prints.
     */
    public static String paraMostrar(String tipo, String valor) {
        String guardado = paraGuardar(tipo, valor);
        if (PASAPORTE.equals(tipo)) {
            return guardado;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < guardado.length(); i++) {
            if (i == 3 || i == 10) {
                sb.append('-');
            }
            sb.append(guardado.charAt(i));
        }
        return sb.toString();
    }

    /**
     * For places that only have the stored text and no type (lists): a
     * complete 11-digit value is a cedula and gets its dashes, anything else
     * is shown as is.
     */
    public static String paraMostrar(String valor) {
        if (valor != null && valor.matches("[0-9]{" + LARGO_CEDULA + "}")) {
            return paraMostrar(CEDULA, valor);
        }
        return valor == null ? "" : valor;
    }

    /** Letters and digits only, lower case - for comparing/searching regardless of dashes or case. */
    public static String sinFormato(String texto) {
        return texto == null ? "" : texto.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }
}
