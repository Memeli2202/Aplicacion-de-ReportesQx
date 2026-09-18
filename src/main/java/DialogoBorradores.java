import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the doctor browse their saved reports in a proper table (Nombre,
 * Cédula, Fecha, Estado columns) and pick one to reopen. Supports live
 * filtering by name, cedula, date, or all fields at once - filtering
 * happens entirely client-side against the already-fetched summary list,
 * so it's instant with no extra network calls.
 */
public class DialogoBorradores extends JDialog {
    private SupabaseReportesClient.ResumenBorrador seleccionado;
    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"Nombre", "Cédula", "Fecha", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);
    private final List<SupabaseReportesClient.ResumenBorrador> filasActuales = new ArrayList<>();
    private final List<SupabaseReportesClient.ResumenBorrador> todosLosBorradores;
    private final JTextField campoBusqueda = new JTextField(18);
    private final JComboBox<String> tipoFiltro = new JComboBox<>(new String[]{"Todos", "Nombre", "Cédula", "Fecha"});

    public DialogoBorradores(Frame parent, List<SupabaseReportesClient.ResumenBorrador> borradores) {
        super(parent, "Reportes Guardados", true);
        this.todosLosBorradores = borradores;

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowSelectionAllowed(true);
        tabla.setColumnSelectionAllowed(false);
        tabla.setAutoCreateRowSorter(true);
        tabla.setRowHeight(24);
        tabla.getTableHeader().setReorderingAllowed(false);

        JButton botonAbrir = new BotonColoreado();
        botonAbrir.setText("Abrir");
        botonAbrir.setBackground(new Color(0, 172, 193));
        botonAbrir.setForeground(Color.WHITE);
        botonAbrir.addActionListener(e -> {
            int filaSeleccionada = tabla.getSelectedRow();
            if (filaSeleccionada < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un reporte primero");
                return;
            }
            //convert from the (possibly sorted) view row to the underlying data row
            int filaModelo = tabla.convertRowIndexToModel(filaSeleccionada);
            seleccionado = filasActuales.get(filaModelo);
            dispose();
        });

        JButton botonCancelar = new BotonColoreado();
        botonCancelar.setText("Cancelar");
        botonCancelar.setBackground(new Color(120, 120, 120));
        botonCancelar.setForeground(Color.WHITE);
        botonCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout());
        botones.add(botonAbrir);
        botones.add(botonCancelar);

        setLayout(new BorderLayout());

        if (borradores.isEmpty()) {
            add(new JLabel("No tienes reportes guardados todavía.", SwingConstants.CENTER), BorderLayout.CENTER);
        } else {
            JPanel barraBusqueda = new JPanel(new BorderLayout(5, 0));
            barraBusqueda.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            barraBusqueda.add(new JLabel("Buscar:"), BorderLayout.WEST);
            barraBusqueda.add(campoBusqueda, BorderLayout.CENTER);
            barraBusqueda.add(tipoFiltro, BorderLayout.EAST);

            campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    aplicarFiltro();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    aplicarFiltro();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    aplicarFiltro();
                }
            });
            tipoFiltro.addActionListener(e -> aplicarFiltro());

            add(barraBusqueda, BorderLayout.NORTH);
            add(new JScrollPane(tabla), BorderLayout.CENTER);
            aplicarFiltro();
        }

        add(botones, BorderLayout.SOUTH);

        setSize(640, 460);
        setLocationRelativeTo(parent);
    }

    /**
     * Rebuilds the displayed table rows based on the current search text
     * and selected filter type - "Todos" checks name/cedula/date together,
     * the others restrict the match to just that one field. filasActuales
     * is kept in sync with the table's rows so a selected row can be
     * mapped back to its actual ResumenBorrador object.
     */
    private void aplicarFiltro() {
        String texto = campoBusqueda.getText().trim().toLowerCase();
        String tipo = (String) tipoFiltro.getSelectedItem();

        modeloTabla.setRowCount(0);
        filasActuales.clear();

        for (SupabaseReportesClient.ResumenBorrador b : todosLosBorradores) {
            if (coincide(b, texto, tipo)) {
                filasActuales.add(b);
                modeloTabla.addRow(new Object[]{safe(b.nombre), safe(b.cedula), safe(b.fecha), safe(b.estado)});
            }
        }
    }

    private boolean coincide(SupabaseReportesClient.ResumenBorrador b, String texto, String tipo) {
        if (texto.isEmpty()) {
            return true;
        }
        if (tipo == null) {
            tipo = "Todos";
        }
        return switch (tipo) {
            case "Nombre" -> safe(b.nombre).toLowerCase().contains(texto);
            case "Cédula" -> safe(b.cedula).toLowerCase().contains(texto);
            case "Fecha" -> safe(b.fecha).toLowerCase().contains(texto);
            default -> safe(b.nombre).toLowerCase().contains(texto)
                    || safe(b.cedula).toLowerCase().contains(texto)
                    || safe(b.fecha).toLowerCase().contains(texto);
        };
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Shows the dialog modally. Returns the picked report's summary, or
     * null if the doctor closed the dialog without picking one.
     */
    public static SupabaseReportesClient.ResumenBorrador mostrar(Frame parent, List<SupabaseReportesClient.ResumenBorrador> borradores) {
        DialogoBorradores dialogo = new DialogoBorradores(parent, borradores);
        dialogo.setVisible(true);
        return dialogo.seleccionado;
    }
}