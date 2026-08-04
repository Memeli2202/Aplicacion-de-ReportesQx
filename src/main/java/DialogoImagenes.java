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
import java.util.*;
import java.util.List;

public class DialogoImagenes extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel imageContainer;
    private JPanel optionFrame;


    //control panel for buttons
    private JButton agregarImagen = new JButton();
    private JButton cancelarImagen = new JButton();
    private JScrollPane scrollPane;

    private final ArrayList<BufferedImage> imagenes = new ArrayList<>();
    private final ArrayList<JTextArea> comentarios = new ArrayList<>();
    private final List<ImagenComentario> resultado = new ArrayList<>();
    private boolean aceptado = false;

    /**
     * Constructor for DialogoImagenes if there are no images loaded
     */
    public DialogoImagenes() {
        this(Collections.emptyList());
    }

    /**
     * Constructor for the images dialog
     *
     * @param existentes if there are images already in the report, it saves them to be edited or deleted.
     */
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
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                File escritorio = new File(System.getProperty("user.home"), "Desktop");
                if (escritorio.exists()) {
                    selector.setCurrentDirectory(escritorio);
                }

                File volumes = new File("/Volumes");
                if (volumes.exists()) {
                    JPanel accesorio = new JPanel();
                    accesorio.setLayout(new BoxLayout(accesorio, BoxLayout.Y_AXIS));
                    accesorio.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                    JButton irAUnidades = new JButton("Memorias Externas");
                    irAUnidades.addActionListener(ev -> selector.setCurrentDirectory(volumes));
                    accesorio.add(irAUnidades);
                    selector.setAccessory(accesorio);
                }
            }

            int result = selector.showOpenDialog(contentPane);
            if (result == JFileChooser.APPROVE_OPTION) {

                File[] archivosSeleccionados = selector.getSelectedFiles();
                Arrays.sort(archivosSeleccionados, Comparator.comparingLong(File::lastModified));
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
                                boolean duplicada = yaExistente(image) || nuevasImagenes.stream().anyMatch(existente -> sonIguales(existente, image));
                                if (duplicada) {
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

                        if (mensajeError != null) {
                            JOptionPane.showMessageDialog(contentPane, mensajeError, "Error cargando: " + errorArchivo, JOptionPane.ERROR_MESSAGE);
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

    /**
     * Shows a preview of the image in bigger size for better visibility
     *
     * @param imagen the image to be previewed
     */
    private void mostrarVistaPrevia(BufferedImage imagen) {
        int maxAncho = 900;
        int maxAlto = 700;
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        double escala = Math.min(1.0, Math.min((double) maxAncho / ancho, (double) maxAlto / alto));
        int anchoFinal = (int) (ancho * escala);
        int altoFinal = (int) (alto * escala);

        Image escalada = imagen.getScaledInstance(anchoFinal, altoFinal, Image.SCALE_SMOOTH);

        JLabel etiquetaImagen = new JLabel(new ImageIcon(escalada));
        JScrollPane scroll = new JScrollPane(etiquetaImagen);

        JDialog previa = new JDialog(this, "Vista Previa", ModalityType.MODELESS);
        previa.setContentPane(scroll);
        previa.setSize(anchoFinal + 40, altoFinal + 60);
        previa.setLocationRelativeTo(this);
        previa.setVisible(true);
        previa.toFront();
        previa.requestFocus();
    }

    /**
     * Makes sure that an image is not added twice
     *
     * @param nueva the new image to be added to the report
     * @return whether the image is already in the report or not
     */
    private boolean yaExistente(BufferedImage nueva) {
        for (BufferedImage existente : imagenes) {
            if (sonIguales(existente, nueva)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares two images pixel by pixel to determine if they are the same
     *
     * @param im1 first image to be compared
     * @param im2 second image to be compared
     * @return whether the images are the same
     */
    private static boolean sonIguales(BufferedImage im1, BufferedImage im2) {
        if (im1.getWidth() != im2.getWidth() || im1.getHeight() != im2.getHeight()) {
            return false;
        }
        int[] pixelesA = im1.getRGB(0, 0, im1.getWidth(), im1.getHeight(), null, 0, im1.getWidth());
        int[] pixelesB = im2.getRGB(0, 0, im2.getWidth(), im2.getHeight(), null, 0, im2.getWidth());

        return Arrays.equals(pixelesA, pixelesB);
    }

    /**
     * Adds the selected image to the DialogoImagenes. Initial comment left blank
     *
     * @param image the image to be added
     */
    private void addImageToUI(BufferedImage image) {
        addImageToUI(image, " ");
    }

    /**
     * Adds the selected image to the DialogoImagenes
     *
     * @param image             the image to be added
     * @param comentarioInicial the comment generated by the user
     */
    private void addImageToUI(BufferedImage image, String comentarioInicial) {
        final int thumbSize = 200;
        Image scaledImage = image.getScaledInstance(thumbSize, thumbSize, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
        imageLabel.setPreferredSize(new Dimension(thumbSize, thumbSize));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        //click the thumbnail to see a preview of the actual image
        imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        imageLabel.setToolTipText("Click para ver en tamaño completo");
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mostrarVistaPrevia(image);
            }
        });

        //caption showing this image's current position and the number on the report
        JLabel captionLabel = new JLabel("Imagen");
        captionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        captionLabel.setFont(captionLabel.getFont().deriveFont(Font.BOLD));
        JPanel columnaImagen = new JPanel(new BorderLayout());
        columnaImagen.add(captionLabel, BorderLayout.NORTH);
        columnaImagen.add(imageLabel, BorderLayout.CENTER);

        //comment box to the right
        JTextArea comentario = new JTextArea();
        comentario.setLineWrap(true);
        comentario.setWrapStyleWord(true);
        comentario.setRows(4);
        comentario.setText(comentarioInicial == null ? "" : comentarioInicial);
        JScrollPane comentarioPane = new JScrollPane(comentario);
        comentarioPane.setBorder(BorderFactory.createTitledBorder("Comentarios"));
        comentarios.add(comentario);

        // move up/down, delete for just this image
        JButton subir = new JButton("Subir");
        JButton bajar = new JButton("Bajar");
        JButton eliminar = new JButton("Eliminar");
        JPanel controlesWrapper = new JPanel(new GridLayout(3, 1, 0, 8));
        //controlesWrapper.setLayout(new BoxLayout(controlesWrapper, BoxLayout.Y_AXIS));
        controlesWrapper.add(subir);
        controlesWrapper.add(bajar);
        controlesWrapper.add(eliminar);

        //keep the buttons in line to the comentarios box
        JPanel controlesContenedor = new JPanel(new BorderLayout());
        controlesContenedor.add(controlesWrapper, BorderLayout.NORTH);

        //pairing the image to the comment
        JPanel entry = new JPanel(new BorderLayout(10, 0));
        entry.add(columnaImagen, BorderLayout.WEST);
        entry.add(comentarioPane, BorderLayout.CENTER);
        entry.add(controlesContenedor, BorderLayout.EAST);
        entry.setBorder(BorderFactory.createEmptyBorder(5, 5, 15, 5));
        entry.setAlignmentX(Component.LEFT_ALIGNMENT);

        //set max size so the box layout doesn't stretch to fit the scroll pane
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, thumbSize + 30));

        //swap image with the one above
        subir.addActionListener(e -> {
            int idx = imagenes.indexOf(image);
            if (idx > 0) {
                Collections.swap(imagenes, idx, idx - 1);
                Collections.swap(comentarios, idx, idx - 1);
                int z = imageContainer.getComponentZOrder(entry);
                imageContainer.setComponentZOrder(entry, z - 1);
                imageContainer.revalidate();
                imageContainer.repaint();
                actualizarNumerosDeCaption();
            }
        });

        bajar.addActionListener(e -> {
            int idx = imagenes.indexOf(image);
            if (idx >= 0 && idx < imagenes.size() - 1) {
                Collections.swap(imagenes, idx, idx + 1);
                Collections.swap(comentarios, idx, idx + 1);
                int z = imageContainer.getComponentZOrder(entry);
                imageContainer.setComponentZOrder(entry, z + 1);
                imageContainer.revalidate();
                imageContainer.repaint();
                actualizarNumerosDeCaption();
            }
        });

        //remove just this image and comment when clicked
        eliminar.addActionListener(e -> {
            imagenes.remove(image);
            comentarios.remove(comentario);
            imageContainer.remove(entry);
            imageContainer.revalidate();
            imageContainer.repaint();
            actualizarNumerosDeCaption();
        });

        //store the caption label on the row so actualizar numeros de captions can find it
        entry.putClientProperty("captionLabel", captionLabel);

        imageContainer.add(entry);

        //refresh dynamically
        imageContainer.revalidate();
        imageContainer.repaint();
        actualizarNumerosDeCaption();
    }

    /**
     * Keeps the numbers of the caption updated for when the images are moved
     */
    private void actualizarNumerosDeCaption() {
        Component[] filas = imageContainer.getComponents();
        for (int i = 0; i < filas.length; i++) {
            if (filas[i] instanceof JPanel fila) {
                Object etiqueta = fila.getClientProperty("captionLabel");
                if (etiqueta instanceof JLabel captionLabel) {
                    captionLabel.setText("Imagen " + (i + 1));
                }
            }
        }
    }

    /**
     * Custom colored buttons for the UI
     */
    private void createUIComponents() {
        buttonOK = new BotonColoreado();
        buttonCancel = new BotonColoreado();
        agregarImagen = new BotonColoreado();
        cancelarImagen = new BotonColoreado();
    }

    /**
     * Object for the images and their comments.
     */
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

    /**
     * Pair each image with whatever its comment box holds
     */
    private void onOK() {
        resultado.clear();
        for (int i = 0; i < imagenes.size(); i++) {
            resultado.add(new ImagenComentario(imagenes.get(i), comentarios.get(i).getText()));
        }
        aceptado = true;
        dispose();
    }

    /**
     * Cancels any changes made
     */
    private void onCancel() {
        aceptado = false;
        dispose();
    }

    /**
     * Checks whether there were images and comments added or not
     *
     * @return either the images tied to their comments or an empty List
     */
    public List<ImagenComentario> getResultado() {
        return aceptado ? resultado : Collections.emptyList();
    }

    /**
     * Checks if the images are to be added to the report
     *
     * @return true if there were images and comments added, false if not
     */
    public boolean isAceptado() {
        return aceptado;
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setAlignmentX(1.0f);
        contentPane.setAlignmentY(1.0f);
        contentPane.setMinimumSize(new Dimension(350, 200));
        contentPane.setPreferredSize(new Dimension(900, 700));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
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
        panel1.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        optionFrame = new JPanel();
        optionFrame.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(optionFrame, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        agregarImagen.setBackground(new Color(-16732991));
        agregarImagen.setBorderPainted(false);
        agregarImagen.setContentAreaFilled(false);
        agregarImagen.setText("Agregar Imagen(es)");
        optionFrame.add(agregarImagen, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelarImagen.setBackground(new Color(-9494761));
        cancelarImagen.setBorderPainted(false);
        cancelarImagen.setContentAreaFilled(false);
        cancelarImagen.setText("Eliminar Contenido(s)");
        optionFrame.add(cancelarImagen, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        optionFrame.add(scrollPane, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageContainer = new JPanel();
        imageContainer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane.setViewportView(imageContainer);
        final Spacer spacer2 = new Spacer();
        optionFrame.add(spacer2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
