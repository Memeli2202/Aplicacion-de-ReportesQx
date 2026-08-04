import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ReportBuilder extends JFrame {
    private JPanel panelDeContenido;
    private JTextField fecha;
    private JTextField nombrePaciente;
    private JTextField cedula;
    private JTextField ars;
    private JTextPane resumenQx;
    private JTextPane postCirugia;
    private JTextField nombreProcedimiento;
    private JButton botonCrearReporte;
    private JTextField enzianP;
    private JTextField enzianO;
    private JTextField enzianT;
    private JTextField enzianA;
    private JTextField enzianB;
    private JTextField enzianC;
    private JTextField enzianF;
    private JButton botonCancelar;
    private JButton botonAgregarImagenes;
    private JTextField enzianO2;
    private JTextField enzianT2;
    private JTextField enzianB2;
    private JTextField edadPaciente;
    private JButton botonNuevoReporte;
    private JToolBar mainToolbar;
    private JButton botonReportesGuradados;
    private JButton botonCerrarSesion;
    private JButton botonVistaPreviaPDF;

    private Reporte reporte;
    private SaveButtonListener saveButtonListener;
    private List<DialogoImagenes.ImagenComentario> imagenesComentarios = new ArrayList<>();

    private final SesionSupabase sesion;
    private ActivosAppClient.Activos activosCache;

    private ActivosAppClient.Activos obtenerActivos() throws IOException, InterruptedException {
        if (activosCache == null) {
            activosCache = ActivosAppClient.cargarActivos(sesion);
        }
        return activosCache;
    }

    /**
     * Constructor for the ReportBuilder Form
     *
     * @param sesion the session of the logged-in user
     */
    public ReportBuilder(SesionSupabase sesion) {
        this.sesion = sesion;
        reporte = new Reporte();

        $$$setupUI$$$();
        setTitle("Crear Nuevo Reporte");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(panelDeContenido);
        pack();

        //sets the frame location to the center of the screen
        setLocationRelativeTo(null);

        //save button event listener
        botonCrearReporte.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generarReporte();
            }
        });

        //cancel button event listener
        botonCancelar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelarReporte();
            }
        });

        //add images button event action listener
        botonAgregarImagenes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DialogoImagenes dialogoImagenes = new DialogoImagenes(imagenesComentarios);
                dialogoImagenes.setVisible(true);

                if (dialogoImagenes.isAceptado()) {
                    imagenesComentarios = dialogoImagenes.getResultado();
                    JOptionPane.showMessageDialog(panelDeContenido, imagenesComentarios.size() + " imagen(es) en el reporte");
                }

            }
        });

        //crearNuevoReporte action listener
        botonNuevoReporte.addActionListener(e -> nuevoReporte());

        //open previous report
        botonReportesGuradados.addActionListener(e -> abrirReporteGuardado());

        //log out
        botonCerrarSesion.addActionListener(e -> cerrarSesion());

        //preview report before generating
        botonVistaPreviaPDF.addActionListener(e -> vistaPreviaReporte());

        //set the frame visible
        setVisible(true);

        //prevents the text being highlighted when switching back to the app
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                SwingUtilities.invokeLater(() -> {

                    Component enfocado = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                    if (enfocado instanceof JTextComponent campo) {
                        campo.setCaretPosition(campo.getCaretPosition());
                    }
                });
            }
        });

        //check for new version in background
        VersionChecker.verificarActualizacion(this);

    }

    public void setSaveButtonListener(SaveButtonListener listener) {
        this.saveButtonListener = listener;
    }

    public void vistaPreviaReporte() {
        reporte.setFecha(fecha.getText());
        reporte.setNombre(nombrePaciente.getText());
        reporte.setEdad(edadPaciente.getText());
        reporte.setCedula(cedula.getText());
        reporte.setArs(ars.getText());
        reporte.setProcedimiento(nombreProcedimiento.getText());
        reporte.setEnzianP(enzianP.getText());
        reporte.setEnzianO(enzianO.getText());
        reporte.setEnzianO2(enzianO2.getText());
        reporte.setEnzianT(enzianT.getText());
        reporte.setEnzianT2(enzianT2.getText());
        reporte.setEnzianA(enzianA.getText());
        reporte.setEnzianB(enzianB.getText());
        reporte.setEnzianB2(enzianB2.getText());
        reporte.setEnzianC(enzianC.getText());
        reporte.setEnzianF(enzianF.getText());
        reporte.setResumenQx(resumenQx.getText());
        reporte.setPostQx(postCirugia.getText());

        JDialog progreso = DialogoProgreso.mostrar(panelDeContenido, "Generando vista previa");

        SwingWorker<File, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected File doInBackground() {
                try {
                    File temporal = File.createTempFile("vista_previa_", ".pdf");
                    temporal.deleteOnExit();
                    ActivosAppClient.Activos activos = obtenerActivos();
                    PdfReportGenerator.generar(reporte, imagenesComentarios, temporal,
                            activos.logo, activos.sello, activos.marcaAgua);
                    return temporal;
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                progreso.dispose();

                if (error != null) {
                    JOptionPane.showMessageDialog(panelDeContenido, "Error al generar la vista previa: " + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    File temporal = get();
                    if (temporal != null) {
                        Desktop.getDesktop().open(temporal);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(panelDeContenido, "No se pudo abrir la vista previa: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        progreso.setVisible(true);
    }


    /**
     * Generates a report with the information filled in by the user
     */
    private void generarReporte() {

        reporte.setFecha(fecha.getText());
        reporte.setNombre(nombrePaciente.getText());
        reporte.setEdad(edadPaciente.getText());
        reporte.setCedula(cedula.getText());
        reporte.setArs(ars.getText());
        reporte.setProcedimiento(nombreProcedimiento.getText());
        reporte.setEnzianP(enzianP.getText());
        reporte.setEnzianO(enzianO.getText());
        reporte.setEnzianO2(enzianO2.getText());
        reporte.setEnzianT(enzianT.getText());
        reporte.setEnzianT2(enzianT2.getText());
        reporte.setEnzianA(enzianA.getText());
        reporte.setEnzianB(enzianB.getText());
        reporte.setEnzianB2(enzianB2.getText());
        reporte.setEnzianC(enzianC.getText());
        reporte.setEnzianF(enzianF.getText());
        reporte.setResumenQx(resumenQx.getText());
        reporte.setPostQx(postCirugia.getText());


        JFileChooser guardarComo = new JFileChooser();
        guardarComo.setDialogTitle("Guardar Como");
        guardarComo.setSelectedFile(new File(nombreArchivo(reporte.getNombre())));

        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            File escritorio = new File(System.getProperty("user.home"), "Desktop");
            if (escritorio.exists()) {
                guardarComo.setCurrentDirectory(escritorio);
            }

            File volumes = new File("/Volumes");
            if (volumes.exists()) {
                JPanel accesorio = new JPanel();
                accesorio.setLayout(new BoxLayout(accesorio, BoxLayout.Y_AXIS));
                accesorio.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                JButton irAUnidades = new JButton("Unidades Externas");
                irAUnidades.addActionListener(ev -> guardarComo.setCurrentDirectory(volumes));
                accesorio.add(irAUnidades);
                guardarComo.setAccessory(accesorio);
            }
        }

        int resultado = guardarComo.showSaveDialog(panelDeContenido);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = guardarComo.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }

        final File destinoArchivo = destino;

        JDialog progreso = DialogoProgreso.mostrar(panelDeContenido, "Generando reporte");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error;
            private Exception errorGuardado;

            @Override
            protected Void doInBackground() {
                try {
                    ActivosAppClient.Activos activos = obtenerActivos();
                    PdfReportGenerator.generar(reporte, imagenesComentarios, destinoArchivo,
                            activos.logo, activos.sello, activos.marcaAgua);
                } catch (Exception e) {
                    error = e;
                }
                try {
                    SupabaseReportesClient.guardarReporte(sesion, reporte, "finalizado", imagenesComentarios);
                } catch (Exception e) {
                    errorGuardado = e;
                }
                return null;
            }

            @Override
            protected void done() {
                progreso.dispose();

                if (error != null) {
                    JOptionPane.showMessageDialog(panelDeContenido, "Error al generar el PDF: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (saveButtonListener != null) {
                    saveButtonListener.onSaveClicked(reporte);
                }

                if (errorGuardado != null) {
                    JOptionPane.showMessageDialog(panelDeContenido, "El PDF se generó correctamente, pero no se pudo guardar en la nube: " + errorGuardado.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
                }

                Object[] opciones = {"Abrir", "Cerrar"};
                int opcion = JOptionPane.showOptionDialog(panelDeContenido, "Reporte guardado correctamente.", "Reporte Generado",
                        JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);

                if (opcion == 0) {
                    try {
                        Desktop.getDesktop().open(destinoArchivo);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(panelDeContenido, "No se pudo abrir el archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

            }
        };

        worker.execute();
        progreso.setVisible(true);

    }

    /**
     * Opens a report that has been saved to be edited
     */
    private void abrirReporteGuardado() {
        JDialog progreso = DialogoProgreso.mostrar(panelDeContenido, "Cargando reportes guardados...");

        SwingWorker<List<SupabaseReportesClient.ResumenBorrador>, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected List<SupabaseReportesClient.ResumenBorrador> doInBackground() {
                try {
                    return SupabaseReportesClient.listarReportes(sesion);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                progreso.dispose();

                if (error != null) {
                    JOptionPane.showMessageDialog(panelDeContenido, "No se pudo cargar la lista de reportes: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                List<SupabaseReportesClient.ResumenBorrador> lista;
                try {
                    lista = get();
                } catch (Exception e) {
                    return;
                }

                SupabaseReportesClient.ResumenBorrador elegido = DialogoBorradores.mostrar(ReportBuilder.this, lista);
                if (elegido != null) {
                    cargarReportePorId(elegido.id);
                }

            }

        };

        worker.execute();
        progreso.setVisible(true);

    }

    /**
     * Searches for the report to be loaded by id
     *
     * @param id the id of the report generated by Supabase
     */
    private void cargarReportePorId(String id) {
        JDialog progreso = DialogoProgreso.mostrar(panelDeContenido, "Abriendo reporte...");

        SwingWorker<SupabaseReportesClient.ResultadoCarga, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected SupabaseReportesClient.ResultadoCarga doInBackground() {
                try {
                    return SupabaseReportesClient.cargarReporte(sesion, id);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                progreso.dispose();

                if (error != null) {
                    JOptionPane.showMessageDialog(panelDeContenido, "No se pudo abrir el reporte: " + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    SupabaseReportesClient.ResultadoCarga resultado = get();
                    cargarDatosEnFormulario(resultado.reporte, resultado.imagenes);
                } catch (Exception ignored) {
                }
            }
        };

        worker.execute();
        progreso.setVisible(true);

    }

    /**
     * Loads the information of the saved report onto the form
     *
     * @param cargado  the saved report information
     * @param imagenes the saved images from the report
     */
    private void cargarDatosEnFormulario(Reporte cargado, List<DialogoImagenes.ImagenComentario> imagenes) {
        reporte = cargado;
        imagenesComentarios = imagenes;

        fecha.setText(safe(cargado.getFecha()));
        nombrePaciente.setText(safe(cargado.getNombre()));
        edadPaciente.setText(safe(cargado.getEdad()));
        cedula.setText(safe(cargado.getCedula()));
        ars.setText(safe(cargado.getArs()));
        nombreProcedimiento.setText(safe(cargado.getProcedimiento()));
        enzianP.setText(safe(cargado.getEnzianP()));
        enzianO.setText(safe(cargado.getEnzianO()));
        enzianO2.setText(safe(cargado.getEnzianO2()));
        enzianT.setText(safe(cargado.getEnzianT()));
        enzianT2.setText(safe(cargado.getEnzianT2()));
        enzianA.setText(safe(cargado.getEnzianA()));
        enzianB.setText(safe(cargado.getEnzianB()));
        enzianB2.setText(safe(cargado.getEnzianB2()));
        enzianC.setText(safe(cargado.getEnzianC()));
        enzianF.setText(safe(cargado.getEnzianF()));
        resumenQx.setText(safe(cargado.getResumenQx()));
        postCirugia.setText(safe(cargado.getPostQx()));

        JOptionPane.showMessageDialog(panelDeContenido,
                "Reporte cargado con " + imagenesComentarios.size() + " imagen(es).");
    }

    /**
     * Checks if a string is null
     *
     * @param texto the string to be checked
     * @return a blank string if the string is null, the text otherwise
     */
    private static String safe(String texto) {
        return texto == null ? "" : texto;
    }

    private void cancelarReporte() {

    }

    /**
     * Logs out the active user
     */
    private void cerrarSesion() {
        int confirmacion = JOptionPane.showConfirmDialog(panelDeContenido, "¿Deseas cerrar sesión? Se perderán los datos no guardados.", "Cerrar Sesión", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        SupabaseAuthClient.cerrarSesion();
        dispose();

        SesionSupabase nuevaSesion = LogInForm.mostrar(null);
        if (nuevaSesion != null) {
            new ReportBuilder(nuevaSesion);
        } else {
            System.exit(0);
        }

    }

    /**
     * Clears all the information that has been filled so that a new report can be created
     */
    private void nuevoReporte() {
        int confirmacion = JOptionPane.showConfirmDialog(panelDeContenido,
                "¿Desea crear un nuevo reporte? Se perderán los datos sin guardar.",
                "Nuevo Reporte", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        fecha.setText("");
        nombreProcedimiento.setText("");
        nombrePaciente.setText("");
        edadPaciente.setText("");
        cedula.setText("");
        ars.setText("");
        enzianP.setText("");
        enzianO.setText("");
        enzianO2.setText("");
        enzianT.setText("");
        enzianT2.setText("");
        enzianA.setText("");
        enzianB.setText("");
        enzianB2.setText("");
        enzianC.setText("");
        enzianF.setText("");
        resumenQx.setText("");
        postCirugia.setText("");

        reporte = new Reporte();
        imagenesComentarios = new ArrayList<>();

    }

    /**
     * Generates the file name for the PDF based on the patients name
     *
     * @param nombre the name of the patient
     * @return the file name based on the patients name, free of any special characters not allowed in file names
     */
    private String nombreArchivo(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "reporte.pdf";
        }
        String archivo = nombre.trim().replaceAll("[\\\\/:*?\"<>|]", "");
        return archivo.isBlank() ? "reporte.pdf" : archivo + ".pdf";
    }

    /**
     * Custom designs for UI buttons
     */
    private void createUIComponents() {
        botonCrearReporte = new BotonColoreado();
        botonCancelar = new BotonColoreado();
        botonNuevoReporte = new BotonColoreado();
        botonReportesGuradados = new BotonColoreado();
        botonCerrarSesion = new BotonColoreado();
        botonVistaPreviaPDF = new BotonColoreado();
        botonAgregarImagenes = new BotonColoreado();
    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        panelDeContenido = new JPanel();
        panelDeContenido.setLayout(new GridLayoutManager(22, 6, new Insets(10, 0, 0, 0), -1, -1));
        panelDeContenido.setMinimumSize(new Dimension(500, 550));
        panelDeContenido.setPreferredSize(new Dimension(600, 700));
        final JLabel label1 = new JLabel();
        label1.setText("Fecha (dd/mm/aaaa)");
        panelDeContenido.add(label1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        nombrePaciente = new JTextField();
        panelDeContenido.add(nombrePaciente, new GridConstraints(2, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Nombre");
        panelDeContenido.add(label2, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        cedula = new JTextField();
        panelDeContenido.add(cedula, new GridConstraints(4, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Cédula");
        panelDeContenido.add(label3, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        ars = new JTextField();
        panelDeContenido.add(ars, new GridConstraints(5, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("ARS");
        panelDeContenido.add(label4, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        fecha = new JTextField();
        panelDeContenido.add(fecha, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelDeContenido.add(scrollPane1, new GridConstraints(16, 1, 2, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        resumenQx = new JTextPane();
        resumenQx.setMinimumSize(new Dimension(7, 15));
        resumenQx.setText("");
        scrollPane1.setViewportView(resumenQx);
        final JScrollPane scrollPane2 = new JScrollPane();
        panelDeContenido.add(scrollPane2, new GridConstraints(19, 1, 2, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        postCirugia = new JTextPane();
        postCirugia.setMinimumSize(new Dimension(7, 15));
        scrollPane2.setViewportView(postCirugia);
        final JLabel label5 = new JLabel();
        label5.setText("Resumen de la cirugía:");
        panelDeContenido.add(label5, new GridConstraints(15, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Que esperar luego de mi cirugía:");
        panelDeContenido.add(label6, new GridConstraints(18, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nombreProcedimiento = new JTextField();
        panelDeContenido.add(nombreProcedimiento, new GridConstraints(6, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Procedimiento");
        panelDeContenido.add(label7, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("#ENZIAN(s)");
        panelDeContenido.add(label8, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        botonCrearReporte.setBackground(new Color(-15171304));
        botonCrearReporte.setBorderPainted(false);
        botonCrearReporte.setContentAreaFilled(false);
        botonCrearReporte.setForeground(new Color(-1));
        botonCrearReporte.setMargin(new Insets(0, 0, 0, 0));
        botonCrearReporte.setText("Generar Reporte");
        panelDeContenido.add(botonCrearReporte, new GridConstraints(21, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panelDeContenido.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panelDeContenido.add(spacer2, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("P");
        panelDeContenido.add(label9, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label10 = new JLabel();
        label10.setText("O");
        panelDeContenido.add(label10, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label11 = new JLabel();
        label11.setText("T");
        panelDeContenido.add(label11, new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label12 = new JLabel();
        label12.setText("A");
        panelDeContenido.add(label12, new GridConstraints(11, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label13 = new JLabel();
        label13.setText("B");
        panelDeContenido.add(label13, new GridConstraints(12, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label14 = new JLabel();
        label14.setText("C");
        panelDeContenido.add(label14, new GridConstraints(13, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label15 = new JLabel();
        label15.setText("F");
        panelDeContenido.add(label15, new GridConstraints(14, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        enzianP = new JTextField();
        panelDeContenido.add(enzianP, new GridConstraints(8, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianO = new JTextField();
        panelDeContenido.add(enzianO, new GridConstraints(9, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianT = new JTextField();
        panelDeContenido.add(enzianT, new GridConstraints(10, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianA = new JTextField();
        panelDeContenido.add(enzianA, new GridConstraints(11, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianB = new JTextField();
        panelDeContenido.add(enzianB, new GridConstraints(12, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianC = new JTextField();
        panelDeContenido.add(enzianC, new GridConstraints(13, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianF = new JTextField();
        panelDeContenido.add(enzianF, new GridConstraints(14, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        botonCancelar.setAutoscrolls(false);
        botonCancelar.setBackground(new Color(-9494761));
        botonCancelar.setBorderPainted(false);
        botonCancelar.setContentAreaFilled(false);
        botonCancelar.setForeground(new Color(-1));
        botonCancelar.setHideActionText(true);
        botonCancelar.setOpaque(false);
        botonCancelar.setSelected(false);
        botonCancelar.setText("Cancelar");
        panelDeContenido.add(botonCancelar, new GridConstraints(21, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panelDeContenido.add(spacer3, new GridConstraints(1, 4, 6, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        botonAgregarImagenes.setBackground(new Color(-16732991));
        botonAgregarImagenes.setBorderPainted(false);
        botonAgregarImagenes.setHideActionText(false);
        botonAgregarImagenes.setText("Agregar Imágenes");
        panelDeContenido.add(botonAgregarImagenes, new GridConstraints(14, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enzianT2 = new JTextField();
        enzianT2.setText("");
        panelDeContenido.add(enzianT2, new GridConstraints(10, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianB2 = new JTextField();
        enzianB2.setText("");
        panelDeContenido.add(enzianB2, new GridConstraints(12, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianO2 = new JTextField();
        panelDeContenido.add(enzianO2, new GridConstraints(9, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setText("Edad");
        panelDeContenido.add(label16, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edadPaciente = new JTextField();
        panelDeContenido.add(edadPaciente, new GridConstraints(3, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panelDeContenido.add(panel1, new GridConstraints(0, 0, 1, 6, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        mainToolbar = new JToolBar();
        panel1.add(mainToolbar, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        botonReportesGuradados.setBackground(new Color(-16732991));
        botonReportesGuradados.setBorderPainted(false);
        botonReportesGuradados.setContentAreaFilled(false);
        botonReportesGuradados.setText("Acceder Reportes Guardados");
        mainToolbar.add(botonReportesGuradados);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        mainToolbar.add(toolBar$Separator1);
        botonNuevoReporte.setBackground(new Color(-16732991));
        botonNuevoReporte.setBorderPainted(false);
        botonNuevoReporte.setContentAreaFilled(false);
        botonNuevoReporte.setText("Nuevo Reporte");
        mainToolbar.add(botonNuevoReporte);
        final JToolBar.Separator toolBar$Separator2 = new JToolBar.Separator();
        mainToolbar.add(toolBar$Separator2);
        botonVistaPreviaPDF.setBackground(new Color(-16732991));
        botonVistaPreviaPDF.setBorderPainted(false);
        botonVistaPreviaPDF.setContentAreaFilled(false);
        botonVistaPreviaPDF.setText("Vista Previa");
        mainToolbar.add(botonVistaPreviaPDF);
        final JToolBar.Separator toolBar$Separator3 = new JToolBar.Separator();
        mainToolbar.add(toolBar$Separator3);
        botonCerrarSesion.setBackground(new Color(-9494761));
        botonCerrarSesion.setBorderPainted(false);
        botonCerrarSesion.setContentAreaFilled(false);
        botonCerrarSesion.setText("Cerrar Sesión");
        mainToolbar.add(botonCerrarSesion);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelDeContenido;
    }

}
