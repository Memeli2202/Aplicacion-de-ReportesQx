import javax.swing.*;
import java.awt.*;

public class DialogoProgreso {


    public static JDialog mostrar(Component parent, String mensaje) {
        Window ventana = SwingUtilities.getWindowAncestor(parent);
        JDialog dialogo = new JDialog(ventana, Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setUndecorated(true);
        dialogo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        JLabel etiqueta = new JLabel(mensaje);
        etiqueta.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(etiqueta, BorderLayout.NORTH);

        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);
        barra.setPreferredSize(new Dimension(220, 18));
        panel.add(barra, BorderLayout.CENTER);
        dialogo.setContentPane(panel);
        dialogo.pack();
        dialogo.setLocationRelativeTo(parent);
        return dialogo;
    }

}
