import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Corrección de cédulas mal escritas.
 *
 * La migración add_tipo_documento.sql marcó como 'pasaporte' todo documento
 * que no tenía exactamente 11 dígitos. Algunos son cédulas mal escritas
 * (por ejemplo, de 10 dígitos). Al iniciar la app, esta clase pregunta al
 * doctor por sus propios pacientes en esa situación y le deja corregir el
 * documento o confirmar que sí es un pasaporte.
 *
 * Usa las funciones de corregir_documentos.sql:
 *   - pacientes_por_revisar()
 *   - corregir_documento_paciente(p_paciente_id, p_documento, p_tipo, p_unir)
 *
 * El formato y la limpieza del documento salen de Documento; las llamadas
 * pasan por SupabaseReportesClient.enviarConReintento (renueva el token).
 *
 * Uso (en ReportBuilder, después de setVisible(true)):
 *     CorreccionDocumentos.revisarAlIniciar(this, sesion);
 */
public class CorreccionDocumentos {

    private static final String SUPABASE_URL = "https://hekiisuiehxoinqqieum.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_cPrGduWF7C8Et4WOyCsJRA_4Cy2AtYt";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Paciente cuyo documento parece una cédula mal escrita. */
    public record PacientePorRevisar(String id, String nombre, String documento, int reportes) {}

    /** Resultado de corregir_documento_paciente. */
    private record Resultado(String estado, String nombreExistente) {}

    // ------------------------------------------------------------------
    // Punto de entrada
    // ------------------------------------------------------------------

    /**
     * Consulta en segundo plano los pacientes por revisar y, si hay alguno,
     * muestra el aviso. Si la consulta falla (sin red, o el script SQL aún no
     * se ha ejecutado) no muestra nada: no debe impedir trabajar.
     */
    public static void revisarAlIniciar(Component padre, SesionSupabase sesion) {
        new SwingWorker<List<PacientePorRevisar>, Void>() {
            @Override
            protected List<PacientePorRevisar> doInBackground() throws Exception {
                return listarPacientesPorRevisar(sesion);
            }

            @Override
            protected void done() {
                List<PacientePorRevisar> pacientes;
                try {
                    pacientes = get();
                } catch (Exception ex) {
                    System.err.println("No se pudo consultar pacientes por revisar: " + ex.getMessage());
                    return;
                }
                if (!pacientes.isEmpty()) {
                    mostrarAviso(padre, sesion, pacientes);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Interfaz
    // ------------------------------------------------------------------

    private static void mostrarAviso(Component padre, SesionSupabase sesion, List<PacientePorRevisar> pacientes) {
        StringBuilder lista = new StringBuilder();
        for (PacientePorRevisar p : pacientes) {
            lista.append("\n  • ").append(p.nombre()).append("  (").append(p.documento()).append(")");
        }

        String mensaje = (pacientes.size() == 1
                ? "Un paciente suyo tiene un documento que parece una cédula mal escrita:"
                : pacientes.size() + " pacientes suyos tienen un documento que parece una cédula mal escrita:")
                + lista
                + "\n\nUna cédula tiene 11 dígitos. Mientras no se corrija, el reporte"
                + "\nimprime \"Pasaporte\" en lugar de \"Cédula\"."
                + "\n\n¿Desea revisarlos ahora?";

        Object[] opciones = {"Revisar ahora", "Recordar más tarde"};
        int eleccion = JOptionPane.showOptionDialog(padre, mensaje, "Documentos por revisar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, opciones, opciones[0]);
        if (eleccion != 0) {
            return;
        }

        for (int i = 0; i < pacientes.size(); i++) {
            boolean continuar = revisarPaciente(padre, sesion, pacientes.get(i), i + 1, pacientes.size());
            if (!continuar) {
                return;
            }
        }
    }

    /**
     * Muestra el formulario de un paciente hasta que se guarde, se omita o se
     * pospongan los demás.
     *
     * @return false si el doctor eligió "Recordar más tarde" (no seguir con los demás)
     */
    private static boolean revisarPaciente(Component padre, SesionSupabase sesion, PacientePorRevisar p,
                                           int numero, int total) {
        JRadioButton opcionCedula = new JRadioButton("Cédula", true);
        JRadioButton opcionPasaporte = new JRadioButton("Pasaporte");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(opcionCedula);
        grupo.add(opcionPasaporte);

        JTextField campoDocumento = new JTextField(p.documento(), 18);
        JLabel ayuda = new JLabel(" ");
        ayuda.setFont(ayuda.getFont().deriveFont(Font.PLAIN, 11f));

        Runnable actualizarAyuda = () -> ayuda.setText(textoDeAyuda(opcionCedula.isSelected(), campoDocumento.getText()));
        campoDocumento.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarAyuda.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarAyuda.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarAyuda.run(); }
        });
        opcionCedula.addActionListener(e -> actualizarAyuda.run());
        opcionPasaporte.addActionListener(e -> actualizarAyuda.run());
        actualizarAyuda.run();

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 3, 3, 3);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        panel.add(new JLabel("<html><b>" + escapar(p.nombre()) + "</b>"
                + (p.reportes() > 0 ? "  (" + p.reportes() + " reporte(s) suyos)" : "")
                + "<br>Documento registrado: " + escapar(p.documento())
                + " (" + soloDigitos(p.documento()).length() + " dígitos)</html>"), c);
        c.gridy = 1;
        panel.add(new JLabel("Verifique con el expediente y corrija el documento:"), c);
        c.gridy = 2; c.gridwidth = 1;
        panel.add(new JLabel("Tipo:"), c);
        c.gridx = 1;
        JPanel tipos = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tipos.add(opcionCedula);
        tipos.add(Box.createHorizontalStrut(10));
        tipos.add(opcionPasaporte);
        panel.add(tipos, c);
        c.gridx = 0; c.gridy = 3;
        panel.add(new JLabel("Documento:"), c);
        c.gridx = 1;
        panel.add(campoDocumento, c);
        c.gridy = 4;
        panel.add(ayuda, c);

        Object[] opciones = {"Guardar", "Omitir este", "Recordar más tarde"};
        String titulo = "Corregir documento (" + numero + " de " + total + ")";

        while (true) {
            int eleccion = JOptionPane.showOptionDialog(padre, panel, titulo,
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
            if (eleccion == 1) return true;            // omitir este, seguir con el siguiente
            if (eleccion != 0) return false;           // recordar más tarde o cerrar la ventana

            boolean esCedula = opcionCedula.isSelected();
            String tipo = esCedula ? Documento.CEDULA : Documento.PASAPORTE;
            String documento = esCedula
                    ? soloDigitos(campoDocumento.getText())   //sin recortar, para avisar si sobran dígitos
                    : Documento.paraGuardar(Documento.PASAPORTE, campoDocumento.getText());

            // validaciones locales antes de ir al servidor
            if (esCedula && documento.length() != 11) {
                JOptionPane.showMessageDialog(padre, "La cédula debe tener 11 dígitos (tiene " + documento.length() + ").",
                        "Cédula incompleta", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (!esCedula && documento.isEmpty()) {
                JOptionPane.showMessageDialog(padre, "Escriba el número de pasaporte.",
                        "Documento vacío", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (esCedula && !cedulaConDigitoValido(documento)) {
                int seguir = JOptionPane.showConfirmDialog(padre,
                        "El dígito verificador de " + formatearCedula(documento) + " no coincide."
                                + "\nPuede ser un error de digitación. ¿Guardar de todos modos?",
                        "Verificar cédula", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (seguir != JOptionPane.YES_OPTION) continue;
            }
            if (!esCedula && documento.equals(p.documento())) {
                int seguir = JOptionPane.showConfirmDialog(padre,
                        "¿Confirma que " + documento + " es un pasaporte?\nNo se le volverá a preguntar por este paciente.",
                        "Confirmar pasaporte", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (seguir != JOptionPane.YES_OPTION) continue;
            }

            try {
                Resultado r = corregirDocumento(sesion, p.id(), documento, tipo, false);

                if ("duplicado".equals(r.estado())) {
                    int unir = JOptionPane.showConfirmDialog(padre,
                            "Ya existe una ficha con ese documento: " + r.nombreExistente() + "."
                                    + "\n\n¿Es la misma persona que " + p.nombre() + "?"
                                    + "\n\nSí: los reportes de esta ficha pasan a la ficha existente"
                                    + "\ny se elimina la ficha mal escrita."
                                    + "\nNo: revise el número e inténtelo de nuevo.",
                            "Documento ya registrado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (unir != JOptionPane.YES_OPTION) continue;
                    r = corregirDocumento(sesion, p.id(), documento, tipo, true);
                }

                String hecho = "unido".equals(r.estado())
                        ? "Los reportes de " + p.nombre() + " se unieron a la ficha existente."
                        : "Documento de " + p.nombre() + " actualizado en la ficha y en sus reportes.";
                JOptionPane.showMessageDialog(padre, hecho, "Guardado", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(padre, "No se pudo guardar:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                // vuelve a mostrar el formulario para reintentar u omitir
            }
        }
    }

    private static String textoDeAyuda(boolean esCedula, String texto) {
        if (!esCedula) {
            return "Se guardará como " + Documento.paraGuardar(Documento.PASAPORTE, texto);
        }
        String digitos = soloDigitos(texto);
        if (digitos.length() != 11) {
            return digitos.length() + " de 11 dígitos";
        }
        return "Se guardará como " + formatearCedula(digitos)
                + (cedulaConDigitoValido(digitos) ? "" : "  (dígito verificador no coincide)");
    }

    // ------------------------------------------------------------------
    // Reglas de cédula (locales a esta clase para no depender de Documento)
    // ------------------------------------------------------------------

    static String soloDigitos(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    static String formatearCedula(String d) {
        return Documento.paraMostrar(Documento.CEDULA, d);
    }

    /**
     * Dígito verificador de la cédula dominicana (Luhn sobre los primeros
     * 10 dígitos, pesos 1,2,1,2...). Algunas cédulas antiguas no lo cumplen,
     * por eso solo se usa como advertencia.
     */
    static boolean cedulaConDigitoValido(String d) {
        if (d == null || !d.matches("\\d{11}")) return false;
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            int producto = (d.charAt(i) - '0') * (i % 2 == 0 ? 1 : 2);
            suma += producto >= 10 ? producto / 10 + producto % 10 : producto;
        }
        int verificador = (10 - suma % 10) % 10;
        return verificador == d.charAt(10) - '0';
    }

    private static String escapar(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------------
    // Llamadas a Supabase
    // ------------------------------------------------------------------

    static List<PacientePorRevisar> listarPacientesPorRevisar(SesionSupabase sesion) throws IOException, InterruptedException {
        HttpResponse<String> response = llamarRpc(sesion, "pacientes_por_revisar", "{}");
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        List<PacientePorRevisar> lista = new ArrayList<>();
        for (JsonNode fila : MAPPER.readTree(response.body())) {
            lista.add(new PacientePorRevisar(
                    fila.path("id").asText(),
                    fila.path("nombre").asText(""),
                    fila.path("documento").asText(""),
                    fila.path("reportes").asInt(0)));
        }
        return lista;
    }

    private static Resultado corregirDocumento(SesionSupabase sesion, String pacienteId, String documento,
                                               String tipo, boolean unir) throws IOException, InterruptedException {
        ObjectNode cuerpo = MAPPER.createObjectNode();
        cuerpo.put("p_paciente_id", pacienteId);
        cuerpo.put("p_documento", documento);
        cuerpo.put("p_tipo", tipo);
        cuerpo.put("p_unir", unir);

        HttpResponse<String> response = llamarRpc(sesion, "corregir_documento_paciente", MAPPER.writeValueAsString(cuerpo));
        if (response.statusCode() != 200) {
            // los "raise exception" de la función llegan en el campo message
            String mensaje;
            try {
                mensaje = MAPPER.readTree(response.body()).path("message").asText(response.body());
            } catch (Exception ex) {
                mensaje = response.body();
            }
            throw new IOException(mensaje);
        }
        JsonNode json = MAPPER.readTree(response.body());
        return new Resultado(json.path("estado").asText(""), json.path("nombre_existente").asText(""));
    }

    /** POST a /rest/v1/rpc/{funcion}; si el token expiró, el helper lo renueva y reintenta una vez. */
    private static HttpResponse<String> llamarRpc(SesionSupabase sesion, String funcion, String json)
            throws IOException, InterruptedException {
        return SupabaseReportesClient.enviarConReintento(sesion, token -> construirRpc(token, funcion, json));
    }

    private static HttpRequest construirRpc(String token, String funcion, String json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/rpc/" + funcion))
                .header("apikey", SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
}
