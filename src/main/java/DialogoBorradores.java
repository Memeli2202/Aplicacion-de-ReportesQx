import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lets the doctor browse their saved reports in a proper table (Nombre,
 * Cédula, Fecha, Estado columns, plus a Doctor column for admins) and pick
 * one to reopen. Supports live filtering by name, cedula, date text, or an
 * actual date range - filtering happens entirely client-side against the
 * already-fetched summary list, so it's instant with no extra network
 * calls.
 */
public class DialogoBorradores extends JDialog {
    private SupabaseReportesClient.ResumenBorrador seleccionado;
    private final boolean mostrarColumnaDoctor;
    private final Map<String, String> nombresPorDoctorId;
    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final List<SupabaseReportesClient.ResumenBorrador> filasActuales = new ArrayList<>();
    private final List<SupabaseReportesClient.ResumenBorrador> todosLosBorradores;
    private final JTextField campoBusqueda = new JTextField(18);
    private final JComboBox<String> tipoFiltro = new JComboBox<>(new String[]{"Todos", "Nombre", "Cédula", "Fecha"});
    private final JTextField campoDesde = new JTextField(10);
    private final JTextField campoHasta = new JTextField(10);
    private final JButton botonLimpiarRango = new JButton("Limpiar rango");

    public DialogoBorradores(Frame parent, List<SupabaseReportesClient.ResumenBorrador> borradores) {
        this(parent, borradores, null);
    }

    /**
     * nombresPorDoctorId maps doctor_id -> display name, for showing who
     * each report is assigned to. Pass null (or an empty map) to hide the
     * Doctor column entirely - used for regular (non-admin) doctors, who
     * only ever see their own reports anyway and don't need it.
     */
    public DialogoBorradores(Frame parent, List<SupabaseReportesClient.ResumenBorrador> borradores, Map<String, String> nombresPorDoctorId) {
        super(parent, "Reportes Guardados", true);
        this.todosLosBorradores = borradores;
        this.nombresPorDoctorId = nombresPorDoctorId;
        this.mostrarColumnaDoctor = nombresPorDoctorId != null && !nombresPorDoctorId.isEmpty();

        Object[] columnas = mostrarColumnaDoctor
                ? new Object[]{"Nombre", "Cédula", "Fecha", "Estado", "Doctor"}
                : new Object[]{"Nombre", "Cédula", "Fecha", "Estado"};

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modeloTabla);

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
            barraBusqueda.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
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

            //date-range filter, using the same calendar picker as the report form
            SelectorFecha.adjuntarA(campoDesde);
            SelectorFecha.adjuntarA(campoHasta);
            campoDesde.setPreferredSize(new Dimension(90, campoDesde.getPreferredSize().height));
            campoHasta.setPreferredSize(new Dimension(90, campoHasta.getPreferredSize().height));

            JPanel barraRango = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            barraRango.add(new JLabel("Rango de fechas - Desde:"));
            barraRango.add(campoDesde);
            barraRango.add(new JLabel("Hasta:"));
            barraRango.add(campoHasta);
            barraRango.add(botonLimpiarRango);

            //picking a date from either calendar re-filters immediately - DocumentListener
            //correctly fires even for the picker's programmatic setText() calls
            DocumentListener refiltrarAlCambiarFecha = new DocumentListener() {
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
            };
            campoDesde.getDocument().addDocumentListener(refiltrarAlCambiarFecha);
            campoHasta.getDocument().addDocumentListener(refiltrarAlCambiarFecha);

            botonLimpiarRango.addActionListener(e -> {
                campoDesde.setText("");
                campoHasta.setText("");
                aplicarFiltro();
            });

            JPanel barrasSuperiores = new JPanel();
            barrasSuperiores.setLayout(new BoxLayout(barrasSuperiores, BoxLayout.Y_AXIS));
            barrasSuperiores.add(barraBusqueda);
            barrasSuperiores.add(barraRango);

            add(barrasSuperiores, BorderLayout.NORTH);
            add(new JScrollPane(tabla), BorderLayout.CENTER);
            aplicarFiltro();
        }

        add(botones, BorderLayout.SOUTH);

        setSize(680, 500);
        setLocationRelativeTo(parent);
    }

    /**
     * Rebuilds the displayed table rows based on the current search text,
     * selected filter type, and date range (if set) - "Todos" checks
     * name/cedula/date together, the others restrict the text match to
     * just that one field. The date range is independent of the text
     * filter and applies on top of it. filasActuales is kept in sync with
     * the table's rows so a selected row can be mapped back to its actual
     * ResumenBorrador object.
     */
    private void aplicarFiltro() {
        String texto = campoBusqueda.getText().trim().toLowerCase();
        String tipo = (String) tipoFiltro.getSelectedItem();
        LocalDate desde = SelectorFecha.parsear(campoDesde.getText());
        LocalDate hasta = SelectorFecha.parsear(campoHasta.getText());

        modeloTabla.setRowCount(0);
        filasActuales.clear();

        for (SupabaseReportesClient.ResumenBorrador b : todosLosBorradores) {
            if (coincide(b, texto, tipo) && dentroDelRango(b, desde, hasta)) {
                filasActuales.add(b);
                if (mostrarColumnaDoctor) {
                    String nombreDoctor = nombresPorDoctorId.getOrDefault(b.doctorId, "Desconocido");
                    modeloTabla.addRow(new Object[]{safe(b.nombre), safe(b.cedula), safe(b.fecha), safe(b.estado), nombreDoctor});
                } else {
                    modeloTabla.addRow(new Object[]{safe(b.nombre), safe(b.cedula), safe(b.fecha), safe(b.estado)});
                }
            }
        }
    }

    /**
     * A report with no parseable fecha_date (old reports never corrected
     * through the picker) is excluded whenever an actual range is set,
     * since there's nothing reliable to compare - it still shows up
     * normally when no range filter is active.
     */
    private boolean dentroDelRango(SupabaseReportesClient.ResumenBorrador b, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return true;
        }
        if (b.fechaDate == null) {
            return false;
        }
        if (desde != null && b.fechaDate.isBefore(desde)) {
            return false;
        }
        if (hasta != null && b.fechaDate.isAfter(hasta)) {
            return false;
        }
        return true;
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
        return mostrar(parent, borradores, null);
    }

    public static SupabaseReportesClient.ResumenBorrador mostrar(Frame parent, List<SupabaseReportesClient.ResumenBorrador> borradores, Map<String, String> nombresPorDoctorId) {
        DialogoBorradores dialogo = new DialogoBorradores(parent, borradores, nombresPorDoctorId);
        dialogo.setVisible(true);
        return dialogo.seleccionado;
    }
}