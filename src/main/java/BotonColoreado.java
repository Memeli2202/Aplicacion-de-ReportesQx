import javax.swing.*;
import java.awt.*;

public class BotonColoreado extends JButton {
    public BotonColoreado() {
        super();
        setContentAreaFilled(false);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color base = getBackground();
        Color relleno = base;
        if (getModel().isPressed()) {
            relleno = base.darker();
        } else if (getModel().isRollover()) {
            relleno = base.brighter();
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(relleno);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.dispose();

        super.paintComponent(g);
    }
}
