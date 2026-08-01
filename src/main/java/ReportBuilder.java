import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
    //private JTextField enzianF2;

    private Reporte reporte;
    private SaveButtonListener saveButtonListener;
    private List<DialogoImagenes.ImagenComentario> imagenesComentarios = new ArrayList<>();

    public ReportBuilder() {
        reporte = new Reporte();

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

                //List<DialogoImagenes.ImagenComentario> seleccion = dialogoImagenes.getResultado();

                if (dialogoImagenes.isAceptado()) {
                    imagenesComentarios = dialogoImagenes.getResultado();
                    JOptionPane.showMessageDialog(panelDeContenido, imagenesComentarios.size() + " imagen(es) en el reporte");
                }

            }
        });

        //set the frame visible
        setVisible(true);
    }

    public void setSaveButtonListener(SaveButtonListener listener) {
        this.saveButtonListener = listener;
    }


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
        //reporte.setEnzianF2(enzianF2.getText());
        reporte.setResumenQx(resumenQx.getText());
        reporte.setPostQx(postCirugia.getText());


        try {
            JFileChooser guardarComo = new JFileChooser();
            guardarComo.setDialogTitle("Guardar Como");
            guardarComo.setSelectedFile(new File(nombreArchivo(reporte.getNombre())));
            int resultado = guardarComo.showSaveDialog(panelDeContenido);
            if (resultado != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File destino = guardarComo.getSelectedFile();
            if (!destino.getName().toLowerCase().endsWith(".pdf")) {
                destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
            }

            PdfReportGenerator.generar(reporte, imagenesComentarios, destino);
            if (saveButtonListener != null) {
                saveButtonListener.onSaveClicked(reporte);
            }

            JOptionPane.showMessageDialog(panelDeContenido, "Reporte guardado correctamente");

        } catch (Exception e) {

            JOptionPane.showMessageDialog(panelDeContenido, "Error al generar el PDF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

        }
    }

    private void cancelarReporte() {

    }

    private String nombreArchivo(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "reporte.pdf";
        }
        String archivo = nombre.trim().replaceAll("[\\\\/:*?\"<>|]", "");
        return archivo.isBlank() ? "reporte.pdf" : archivo + ".pdf";
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panelDeContenido = new JPanel();
        panelDeContenido.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(21, 6, new Insets(10, 0, 0, 0), -1, -1));
        panelDeContenido.setMinimumSize(new Dimension(500, 550));
        panelDeContenido.setPreferredSize(new Dimension(600, 700));
        final JLabel label1 = new JLabel();
        label1.setText("Fecha (dd/mm/aaaa)");
        panelDeContenido.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        nombrePaciente = new JTextField();
        panelDeContenido.add(nombrePaciente, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Nombre");
        panelDeContenido.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        cedula = new JTextField();
        panelDeContenido.add(cedula, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Cédula");
        panelDeContenido.add(label3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        ars = new JTextField();
        panelDeContenido.add(ars, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("ARS");
        panelDeContenido.add(label4, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        fecha = new JTextField();
        panelDeContenido.add(fecha, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelDeContenido.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(15, 1, 2, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        resumenQx = new JTextPane();
        resumenQx.setMinimumSize(new Dimension(7, 15));
        resumenQx.setText("");
        scrollPane1.setViewportView(resumenQx);
        final JScrollPane scrollPane2 = new JScrollPane();
        panelDeContenido.add(scrollPane2, new com.intellij.uiDesigner.core.GridConstraints(18, 1, 2, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        postCirugia = new JTextPane();
        postCirugia.setMinimumSize(new Dimension(7, 15));
        scrollPane2.setViewportView(postCirugia);
        final JLabel label5 = new JLabel();
        label5.setText("Resumen de la cirugía:");
        panelDeContenido.add(label5, new com.intellij.uiDesigner.core.GridConstraints(14, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Que esperar luego de mi cirugía:");
        panelDeContenido.add(label6, new com.intellij.uiDesigner.core.GridConstraints(17, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nombreProcedimiento = new JTextField();
        panelDeContenido.add(nombreProcedimiento, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Procedimiento");
        panelDeContenido.add(label7, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("#ENZIAN(s)");
        panelDeContenido.add(label8, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 0, false));
        botonCrearReporte = new JButton();
        botonCrearReporte.setBackground(new Color(-15171304));
        botonCrearReporte.setForeground(new Color(-1));
        botonCrearReporte.setMargin(new Insets(0, 0, 0, 0));
        botonCrearReporte.setText("Generar Reporte");
        panelDeContenido.add(botonCrearReporte, new com.intellij.uiDesigner.core.GridConstraints(20, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panelDeContenido.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        panelDeContenido.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("P");
        panelDeContenido.add(label9, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label10 = new JLabel();
        label10.setText("O");
        panelDeContenido.add(label10, new com.intellij.uiDesigner.core.GridConstraints(8, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label11 = new JLabel();
        label11.setText("T");
        panelDeContenido.add(label11, new com.intellij.uiDesigner.core.GridConstraints(9, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label12 = new JLabel();
        label12.setText("A");
        panelDeContenido.add(label12, new com.intellij.uiDesigner.core.GridConstraints(10, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label13 = new JLabel();
        label13.setText("B");
        panelDeContenido.add(label13, new com.intellij.uiDesigner.core.GridConstraints(11, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label14 = new JLabel();
        label14.setText("C");
        panelDeContenido.add(label14, new com.intellij.uiDesigner.core.GridConstraints(12, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        final JLabel label15 = new JLabel();
        label15.setText("F");
        panelDeContenido.add(label15, new com.intellij.uiDesigner.core.GridConstraints(13, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(138, 17), null, 2, false));
        enzianP = new JTextField();
        panelDeContenido.add(enzianP, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianO = new JTextField();
        panelDeContenido.add(enzianO, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianT = new JTextField();
        panelDeContenido.add(enzianT, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianA = new JTextField();
        panelDeContenido.add(enzianA, new com.intellij.uiDesigner.core.GridConstraints(10, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianB = new JTextField();
        panelDeContenido.add(enzianB, new com.intellij.uiDesigner.core.GridConstraints(11, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianC = new JTextField();
        panelDeContenido.add(enzianC, new com.intellij.uiDesigner.core.GridConstraints(12, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianF = new JTextField();
        panelDeContenido.add(enzianF, new com.intellij.uiDesigner.core.GridConstraints(13, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        botonCancelar = new JButton();
        botonCancelar.setAutoscrolls(false);
        botonCancelar.setBackground(new Color(-9494761));
        botonCancelar.setForeground(new Color(-1));
        botonCancelar.setHideActionText(true);
        botonCancelar.setOpaque(true);
        botonCancelar.setSelected(false);
        botonCancelar.setText("Cancelar");
        panelDeContenido.add(botonCancelar, new com.intellij.uiDesigner.core.GridConstraints(20, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        panelDeContenido.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 6, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        botonAgregarImagenes = new JButton();
        botonAgregarImagenes.setHideActionText(false);
        botonAgregarImagenes.setText("Agregar Imágenes");
        panelDeContenido.add(botonAgregarImagenes, new com.intellij.uiDesigner.core.GridConstraints(13, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enzianT2 = new JTextField();
        enzianT2.setText("");
        panelDeContenido.add(enzianT2, new com.intellij.uiDesigner.core.GridConstraints(9, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianB2 = new JTextField();
        enzianB2.setText("");
        panelDeContenido.add(enzianB2, new com.intellij.uiDesigner.core.GridConstraints(11, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        enzianO2 = new JTextField();
        panelDeContenido.add(enzianO2, new com.intellij.uiDesigner.core.GridConstraints(8, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setText("Edad");
        panelDeContenido.add(label16, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edadPaciente = new JTextField();
        panelDeContenido.add(edadPaciente, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelDeContenido;
    }

}
