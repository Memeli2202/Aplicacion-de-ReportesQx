import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;
import java.util.function.Consumer;

/**
 * Search-as-you-type for the patient's name and document fields: while the
 * doctor types in either one, existing patients that match show up in a small
 * popup under the field. Picking one fills both fields and reports the
 * choice to the form, which links the report to that patient's UID.
 *
 * Like SelectorFecha, it only adds behavior to fields that already exist,
 * so the .form file is untouched.
 */
public class BuscadorPacientes {

    private final SesionSupabase sesion;
    private final JTextField campoNombre;
    private final JTextField campoDocumento;
    private final Consumer<PacientesClient.Paciente> alElegir;
    private final Runnable alEditarDocumento;

    private final JPopupMenu popup = new JPopupMenu();
    private final Timer retraso;
    private JTextField campoActivo;
    private boolean programatico = false;
    private int secuencia = 0;

    /**
     * @param alElegir          called when the doctor picks a patient from the suggestions
     * @param alEditarDocumento called when the doctor types in the document field - the
     *                          report is then no longer the picked patient's, so the form
     *                          drops the link and it is resolved again from the new document
     */
    public BuscadorPacientes(SesionSupabase sesion, JTextField campoNombre, JTextField campoDocumento,
                             Consumer<PacientesClient.Paciente> alElegir, Runnable alEditarDocumento) {
        this.sesion = sesion;
        this.campoNombre = campoNombre;
        this.campoDocumento = campoDocumento;
        this.alElegir = alElegir;
        this.alEditarDocumento = alEditarDocumento;

        popup.setFocusable(false); //keep typing in the field while suggestions are open

        //wait a moment after the last keystroke so a fast typist doesn't fire one query per letter
        retraso = new Timer(300, e -> buscar());
        retraso.setRepeats(false);

        escuchar(campoNombre);
        escuchar(campoDocumento);
    }

    /**
     * Runs code that sets these fields from the program (loading a report,
     * clearing the form) without it counting as the doctor typing - no search
     * popup, and no dropping of the patient link.
     */
    public void sinBusqueda(Runnable accion) {
        boolean anterior = programatico;
        programatico = true;
        try {
            accion.run();
        } finally {
            programatico = anterior;
        }
        retraso.stop();
        popup.setVisible(false);
    }

    private void escuchar(JTextField campo) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cambio(campo); }
            @Override public void removeUpdate(DocumentEvent e) { cambio(campo); }
            @Override public void changedUpdate(DocumentEvent e) { cambio(campo); }
        });
    }

    private void cambio(JTextField campo) {
        if (programatico) {
            return;
        }
        if (campo == campoDocumento) {
            alEditarDocumento.run();
        }
        campoActivo = campo;
        retraso.restart();
    }

    private void buscar() {
        JTextField campo = campoActivo;
        if (campo == null || !campo.isShowing()) {
            return;
        }
        String texto = campo.getText();
        if (texto.trim().length() < 2) {
            popup.setVisible(false);
            return;
        }

        int miSecuencia = ++secuencia;
        new SwingWorker<List<PacientesClient.Paciente>, Void>() {
            @Override
            protected List<PacientesClient.Paciente> doInBackground() throws Exception {
                return PacientesClient.buscar(sesion, texto);
            }

            @Override
            protected void done() {
                if (miSecuencia != secuencia) {
                    return; //the doctor kept typing - a newer search will answer
                }
                try {
                    mostrar(campo, get());
                } catch (Exception ex) {
                    popup.setVisible(false); //suggestions are a convenience, never block the form
                }
            }
        }.execute();
    }

    private void mostrar(JTextField campo, List<PacientesClient.Paciente> pacientes) {
        popup.setVisible(false);
        popup.removeAll();
        if (pacientes.isEmpty() || !campo.isShowing()) {
            return;
        }
        for (PacientesClient.Paciente p : pacientes) {
            JMenuItem item = new JMenuItem(p.toString());
            item.addActionListener(e -> elegir(p));
            popup.add(item);
        }
        popup.show(campo, 0, campo.getHeight());
    }

    private void elegir(PacientesClient.Paciente paciente) {
        sinBusqueda(() -> {
            alElegir.accept(paciente); //first: the form switches to this patient's document type
            campoNombre.setText(paciente.nombre);
            campoDocumento.setText(paciente.documento);
        });
    }
}
