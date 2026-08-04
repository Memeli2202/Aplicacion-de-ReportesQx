import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

/**
 * A JButton that paints its own colored background directly, instead of
 * relying on the native Look and Feel to respect setBackground(). macOS's
 * Aqua L&F in particular can ignore setOpaque/setContentAreaFilled/
 * setBorderPainted entirely for solid custom fills - this sidesteps that
 * by doing the fill ourselves, so it looks identical on every OS.
 */
public class BotonColoreado extends JButton {

    public BotonColoreado() {
        super();
        setContentAreaFilled(false);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);

        addMouseListener(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent e) {
                get.Model().setRollover(true);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e){
                getModel().setRollover(false);
                repaint();
            }
        });
        
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color base = getBackground();
        boolean activo = getModel().isRollover() || getModel().isPressed();

        Color relleno;
        Color borde;
        Color textoColor;

        if (activo) {
            relleno = getModel().isPressed() ? base.darker() : base;
            borde = relleno.darker();
            textoColor = Color.WHITE;
        } else {
            relleno = new Color(230, 230, 230);
            borde = new Color(180, 180, 180);
            textoColor = new Color(60, 60, 60);
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(relleno);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        g2.setColor(borde);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        g2.dispose();

        setForeground(textoColor);

        super.paintComponent(g);
    }
}
