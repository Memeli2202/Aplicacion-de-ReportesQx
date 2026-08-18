import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class LogInForm extends JDialog {
    private JTextField correoUsuario;
    private JPasswordField contrasenaUsuario;
    private JCheckBox mostrarContrasenaCheckBox;
    private JButton botonIniciarSesion;
    private JButton botonRegistrarse;
    private JPanel logInForm;
    private JTextField campoNombre;

    private SesionSupabase sesion;
    private boolean modoRegistro = false;
    private JLabel etiquetaNombre;

    /**
     * Log In screen. Lets a user either register a new account or log in to an existing one
     *
     * @param parent the Frame from which the dialog is displayed
     */
    public LogInForm(Frame parent) {
        super(parent, "Inicio de Sesión", true);
        $$$setupUI$$$();
        setContentPane(logInForm);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);


        //name + label hidden, only show when registering
        etiquetaNombre = buscarEtiquetaNombre();
        campoNombre.setVisible(false);
        if (etiquetaNombre != null) {
            etiquetaNombre.setVisible(false);
        }

        //show password toggle
        mostrarContrasenaCheckBox.addActionListener(e -> {
            contrasenaUsuario.setEchoChar(mostrarContrasenaCheckBox.isSelected() ? (char) 0 : '•');

        });

        botonIniciarSesion.addActionListener(e -> integrarLogin());
        botonRegistrarse.addActionListener(e -> {
            if (!modoRegistro) {
                modoRegistro = true;
                campoNombre.setVisible(true);
                if (etiquetaNombre != null) {
                    etiquetaNombre.setVisible(true);
                }
                botonRegistrarse.setText("Confirmar Registro");
                pack();
            } else {
                intentarRegistro();
            }
        });
        getRootPane().setDefaultButton(botonIniciarSesion);

        pack();
        setLocationRelativeTo(parent);


    }

    /**
     * Searches for the 'Nombre' label to hide it until the modoRegistro is activated
     *
     * @return the label for the Nombre field
     */
    private JLabel buscarEtiquetaNombre() {
        for (Component c : logInForm.getComponents()) {
            if (c instanceof JLabel && "Nombre".equals(((JLabel) c).getText())) {
                return (JLabel) c;
            }
        }
        return null;
    }

    /**
     * Connects to Supabase to verify log in credentials. If the user does not fill in both fields a warning is displayed.
     */
    private void integrarLogin() {
        String email = correoUsuario.getText().trim();
        String password = new String(contrasenaUsuario.getPassword());
        if (email.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(logInForm, "Ingrese correo y contraseña", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog progreso = DialogoProgreso.mostrar(logInForm, "Conectando...");

        SwingWorker<SesionSupabase, Void> worker = new SwingWorker<>() {
            private String error;

            @Override
            protected SesionSupabase doInBackground() {
                try {
                    return SupabaseAuthClient.iniciarSesion(email, password);
                } catch (Exception ex) {
                    error = ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                progreso.dispose();
                SesionSupabase resultado = null;
                try {
                    resultado = get();
                } catch (Exception ignored) {
                }
                if (resultado != null) {
                    sesion = resultado;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(logInForm, error != null ? error : "No se pudo iniciar sesión", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

        };

        worker.execute();
        progreso.setVisible(true);

    }

    /**
     * Attempts to create a new User with email verification. Displays a warning message if there is an empty field.
     */
    private void intentarRegistro() {
        String email = correoUsuario.getText().trim();
        String password = new String(contrasenaUsuario.getPassword());
        String nombre = campoNombre.getText().trim();

        if (email.isBlank() || password.isBlank() || nombre.isBlank()) {
            JOptionPane.showMessageDialog(logInForm, "Completa nombre, correo y contraseña para registrarte", "Datos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog progreso = DialogoProgreso.mostrar(logInForm, "Registrando...");

        SwingWorker<SesionSupabase, Void> worker = new SwingWorker<>() {
            private String error;
            private boolean requiereConfirmacion;

            @Override
            protected SesionSupabase doInBackground() {
                try {
                    SesionSupabase resultado = SupabaseAuthClient.registrarse(email, password, nombre);
                    requiereConfirmacion = (resultado == null);
                    return resultado;
                } catch (Exception ex) {
                    error = ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                progreso.dispose();
                SesionSupabase resultado = null;
                try {
                    resultado = get();
                } catch (Exception ignored) {
                }

                if (resultado != null) {
                    sesion = resultado;
                    dispose();
                } else if (requiereConfirmacion) {
                    JOptionPane.showMessageDialog(logInForm, "Revisa tu correo para confirmar la cuenta, luego inicia sesión", "Confirma tu cuenta", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(logInForm, error != null ? error : "No se pudo registrar su cuenta", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

        };

        worker.execute();
        progreso.setVisible(true);
    }


    public static SesionSupabase mostrar(Frame parent) {
        LogInForm dialogo = new LogInForm(parent);
        dialogo.setVisible(true);
        return dialogo.sesion;
    }

    /**
     * Section for customizable UI components
     */
    private void createUIComponents() {
        botonIniciarSesion = new BotonColoreado();
        botonIniciarSesion.setBackground(new Color(0, 172, 193));
        botonIniciarSesion.setForeground(Color.WHITE);

        botonRegistrarse = new BotonColoreado();
        botonRegistrarse.setBackground(new Color(120, 120, 120));
        botonRegistrarse.setForeground(Color.WHITE);

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
        logInForm = new JPanel();
        logInForm.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 4, new Insets(0, 0, 0, 0), -1, -1));
        logInForm.setName("");
        logInForm.setPreferredSize(new Dimension(400, 250));
        logInForm.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, new Color(-16777216)));
        correoUsuario = new JTextField();
        correoUsuario.setText("");
        logInForm.add(correoUsuario, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        botonIniciarSesion.setBackground(new Color(-15171304));
        botonIniciarSesion.setBorderPainted(false);
        botonIniciarSesion.setContentAreaFilled(false);
        botonIniciarSesion.setText("Iniciar Sesión");
        logInForm.add(botonIniciarSesion, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mostrarContrasenaCheckBox = new JCheckBox();
        mostrarContrasenaCheckBox.setText("Mostrar Contraseña");
        logInForm.add(mostrarContrasenaCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        botonRegistrarse.setBackground(new Color(-16732991));
        botonRegistrarse.setBorderPainted(false);
        botonRegistrarse.setContentAreaFilled(false);
        botonRegistrarse.setText("Registrarme");
        logInForm.add(botonRegistrarse, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Contraseña:");
        logInForm.add(label1, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(212, 17), null, 0, false));
        contrasenaUsuario = new JPasswordField();
        logInForm.add(contrasenaUsuario, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Correo:");
        logInForm.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(212, 17), null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        logInForm.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        logInForm.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Nombre");
        logInForm.add(label3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        campoNombre = new JTextField();
        campoNombre.setText("");
        logInForm.add(campoNombre, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return logInForm;
    }

}
