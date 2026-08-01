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

            int result = selector.showOpenDialog(contentPane);
            if (result == JFileChooser.APPROVE_OPTION) {
                int duplicadas = 0;
                for (File selectedFile : selector.getSelectedFiles()) {
                    try {
                        BufferedImage image = ImageIO.read(selectedFile);
                        if (image != null) {

                            if (yaExistente(image)) {
                                duplicadas++;
                                continue;
                            }

                            //append the single image to the master arrayList
                            imagenes.add(image);

                            //update UI to show thumbnail
                            addImageToUI(image);
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(contentPane, ex.getMessage(), "Error loading:" + selectedFile.getName(), JOptionPane.ERROR_MESSAGE);
                    }
                }
                if (duplicadas > 0) {
                    JOptionPane.showMessageDialog(contentPane, duplicadas + " imagen(es) se encontraban ya en el reporte y fueron omitidas",
                            "Imagen Duplicada", JOptionPane.ERROR_MESSAGE);
                }
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
        contentPane = new JPanel();
        contentPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setAlignmentX(1.0f);
        contentPane.setAlignmentY(1.0f);
        contentPane.setMinimumSize(new Dimension(350, 200));
        contentPane.setPreferredSize(new Dimension(900, 700));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel1.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setBackground(new Color(-15171304));
        buttonOK.setForeground(new Color(-1));
        buttonOK.setSelected(true);
        buttonOK.setText("Agregar al Reporte");
        panel2.add(buttonOK, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setBackground(new Color(-11070710));
        buttonCancel.setForeground(new Color(-1));
        buttonCancel.setText("Cancelar");
        panel1.add(buttonCancel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        optionFrame = new JPanel();
        optionFrame.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(optionFrame, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        agregarImagen = new JButton();
        agregarImagen.setBackground(new Color(-327681));
        agregarImagen.setText("Agregar Imagen(es)");
        optionFrame.add(agregarImagen, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelarImagen = new JButton();
        cancelarImagen.setBackground(new Color(-3748128));
        cancelarImagen.setText("Eliminar Contenido(s)");
        optionFrame.add(cancelarImagen, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        optionFrame.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        optionFrame.add(scrollPane, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageContainer = new JPanel();
        imageContainer.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane.setViewportView(imageContainer);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
