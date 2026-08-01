import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DialogoImagenes extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel imageContainer;
    private JPanel optionFrame;


    //control panel for buttons
    //private JPanel controlPanel = new JPanel(new FlowLayout());
    private JButton agregarImagen = new JButton();
    private JButton cancelarImagen = new JButton();
    private JScrollPane scrollPane;

    private final ArrayList<BufferedImage> imagenes = new ArrayList<>();
    private final ArrayList<JTextArea> comentarios = new ArrayList<>();
    private final List<ImagenComentario> resultado = new ArrayList<>();
    private boolean aceptado = false;

    public DialogoImagenes() {
        this(Collections.emptyList());
    }

    public DialogoImagenes(List<ImagenComentario> existentes) {
        $$$setupUI$$$();
        setTitle("Agregar Imagenes al Reporte");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        //preview the images added
        imageContainer.setLayout(new BoxLayout(imageContainer, BoxLayout.Y_AXIS));
        scrollPane.setViewportView(imageContainer);


        //add an image to the master list
        agregarImagen.addActionListener(e -> {
            JFileChooser selector = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Imagenes", "jpg", "png", "jpeg");
            selector.setFileFilter(filter);
            selector.setMultiSelectionEnabled(true);

            //added visibility to external drives on macOS
            if(System.getProperty("os.name").toLowerCase().contains("mac")){
                File volumes = new File("/Volumes");
                if(volumes.exists()){
                    selector.setCurrentDirectory(volumes);
                }
            }

            int result = selector.showOpenDialog(contentPane);
            if (result == JFileChooser.APPROVE_OPTION) {

                File[] archivosSeleccionados = selector.getSelectedFiles();
                JDialog progreso = DialogoProgreso.mostrar(contentPane, "Cargando imágenes...");

                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    private final List<BufferedImage> nuevasImagenes = new ArrayList<>();

                    int duplicadas = 0;
                    private String mensajeError;
                    private String errorArchivo;

                    @Override
                    protected Void doInBackground() {
                        for (File selectedFile : archivosSeleccionados) {
                            try {
                                BufferedImage image = ImageIO.read(selectedFile);
                                if (image == null) {
                                    continue;
                                }
                                boolean duplicada =yaExistente(image) || nuevasImagenes.stream().anyMatch(existente -> sonIguales(existente, image));
                                if(duplicada){
                                    duplicadas++;
                                } else {
                                    nuevasImagenes.add(image);
                                }

                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(contentPane, ex.getMessage(), "Error loading:" + selectedFile.getName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        progreso.dispose();

                        for (BufferedImage imagen : nuevasImagenes) {
                            imagenes.add(imagen);
                            addImageToUI(imagen);
                        }

                        if(mensajeError != null){
                            JOptionPane.showMessageDialog(contentPane, mensajeError, "Error cargando: " + errorArchivo , JOptionPane.ERROR_MESSAGE);
                        }

                        if (duplicadas > 0) {
                            JOptionPane.showMessageDialog(contentPane, duplicadas + " imagen(es) se encontraban ya en el reporte y fueron omitidas",
                                    "Imagen Duplicada", JOptionPane.ERROR_MESSAGE);
                        }

                    }
                };

                worker.execute();
                progreso.setVisible(true);


            }
        });

        cancelarImagen.addActionListener(e -> {
            imagenes.clear();
            comentarios.clear();
            imageContainer.removeAll();
            imageContainer.revalidate();
            imageContainer.repaint();
        });

        for (ImagenComentario existente : existentes) {
            imagenes.add(existente.getImagen());
            addImageToUI(existente.getImagen(), existente.getComentario());
        }

        contentPane.setVisible(true);
        pack();
        setLocationRelativeTo(null);


    }

    private boolean yaExistente(BufferedImage nueva) {
        for (BufferedImage existente : imagenes) {
            if (sonIguales(existente, nueva)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sonIguales(BufferedImage im1, BufferedImage im2) {
        if (im1.getWidth() != im2.getWidth() || im1.getHeight() != im2.getHeight()) {
            return false;
        }
        int[] pixelesA = im1.getRGB(0, 0, im1.getWidth(), im1.getHeight(), null, 0, im1.getWidth());
        int[] pixelesB = im2.getRGB(0, 0, im2.getWidth(), im2.getHeight(), null, 0, im2.getWidth());

        return Arrays.equals(pixelesA, pixelesB);
    }

    private void addImageToUI(BufferedImage image) {
        addImageToUI(image, " ");
    }

    private void addImageToUI(BufferedImage image, String comentarioInicial) {
        final int thumbSize = 200;
        Image scaledImage = image.getScaledInstance(thumbSize, thumbSize, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setPreferredSize(new Dimension(thumbSize, thumbSize));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        //comment box to the right
        JTextArea comentario = new JTextArea();
        comentario.setLineWrap(true);
        comentario.setWrapStyleWord(true);
        comentario.setRows(4);
        comentario.setText(comentarioInicial == null ? "" : comentarioInicial);
        JScrollPane comentarioPane = new JScrollPane(comentario);
        comentarioPane.setBorder(BorderFactory.createTitledBorder("Comentarios"));
        comentarios.add(comentario);

        //delete button for just one image
        JButton eliminar = new JButton("Eliminar");
        JPanel eliminarWrapper = new JPanel(new BorderLayout());
        eliminarWrapper.add(eliminar, BorderLayout.NORTH);

        //pairing the image to the comment
        JPanel entry = new JPanel(new BorderLayout(10, 0));
        entry.add(imageLabel, BorderLayout.WEST);
        entry.add(comentarioPane, BorderLayout.CENTER);
        entry.add(eliminarWrapper, BorderLayout.EAST);
        entry.setBorder(BorderFactory.createEmptyBorder(5, 5, 15, 5));
        entry.setAlignmentX(Component.LEFT_ALIGNMENT);

        //set max size so the box layout doesn't stretch to fit the scroll pane
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, thumbSize + 30));

        //remove just this image and comment when clicked
        eliminar.addActionListener(e -> {
            imagenes.remove(image);
            comentarios.remove(comentario);
            imageContainer.remove(entry);
            imageContainer.revalidate();
            imageContainer.repaint();
        });

        imageContainer.add(entry);

        //refresh dynamically
        imageContainer.revalidate();
        imageContainer.repaint();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        buttonOK = new BotonColoreado();
        buttonCancel = new BotonColoreado();
        agregarImagen = new BotonColoreado();
        cancelarImagen = new BotonColoreado();
    }

    public static class ImagenComentario {
        private final BufferedImage imagen;
        private final String comentario;

        public ImagenComentario(BufferedImage imagen, String comentario) {
            this.imagen = imagen;
            this.comentario = comentario;
        }

        public BufferedImage getImagen() {
            return imagen;
        }

        public String getComentario() {
            return comentario;
        }
    }

    private void onOK() {
        //pair each image with whatever its comment box holds
        resultado.clear();
        for (int i = 0; i < imagenes.size(); i++) {
            resultado.add(new ImagenComentario(imagenes.get(i), comentarios.get(i).getText()));
        }
        aceptado = true;
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        aceptado = false;
        dispose();
    }

    public List<ImagenComentario> getResultado() {
        return aceptado ? resultado : Collections.emptyList();
    }

    public boolean isAceptado() {
        return aceptado;
    }

//    public static void main(String[] args) {
//        DialogoImagenes dialog = new DialogoImagenes();
//        //SwingUtilities.invokeLater(() -> new DialogoImagenes().createAndShowGUI());
//        //dialog.pack();
//        dialog.setVisible(true);
//
//        for (ImagenComentario comentario : dialog.resultado) {
//            System.out.println(comentario.getComentario());
//        }
//
//        System.exit(0);
//    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setAlignmentX(1.0f);
        contentPane.setAlignmentY(1.0f);
        contentPane.setMinimumSize(new Dimension(350, 200));
        contentPane.setPreferredSize(new Dimension(900, 700));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK.setBackground(new Color(-15171304));
        buttonOK.setBorderPainted(false);
        buttonOK.setContentAreaFilled(false);
        buttonOK.setForeground(new Color(-1));
        buttonOK.setOpaque(false);
        buttonOK.setSelected(true);
        buttonOK.setText("Agregar al Reporte");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel.setBackground(new Color(-11070710));
        buttonCancel.setBorderPainted(false);
        buttonCancel.setContentAreaFilled(false);
        buttonCancel.setForeground(new Color(-1));
        buttonCancel.setOpaque(false);
        buttonCancel.setText("Cancelar");
        panel1.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        optionFrame = new JPanel();
        optionFrame.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(optionFrame, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        agregarImagen.setBackground(new Color(-327681));
        agregarImagen.setBorderPainted(false);
        agregarImagen.setContentAreaFilled(false);
        agregarImagen.setText("Agregar Imagen(es)");
        optionFrame.add(agregarImagen, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelarImagen.setBackground(new Color(-3748128));
        cancelarImagen.setBorderPainted(false);
        cancelarImagen.setContentAreaFilled(false);
        cancelarImagen.setText("Eliminar Contenido(s)");
        optionFrame.add(cancelarImagen, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        optionFrame.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        optionFrame.add(scrollPane, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageContainer = new JPanel();
        imageContainer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane.setViewportView(imageContainer);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
