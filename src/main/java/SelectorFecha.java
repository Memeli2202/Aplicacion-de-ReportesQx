import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * A small calendar popup that attaches to a JTextField, making it behave
 * like a real date picker instead of free text - clicking the field opens
 * a month-grid popup, and picking a day fills the field as dd/mm/yyyy.
 *
 * Deliberately doesn't replace the field or touch the .form file - it just
 * adds behavior to whatever JTextField already exists, so it works
 * regardless of how that field was originally laid out in the Designer.
 */
public class SelectorFecha {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ESPANOL = new Locale("es");

    /**
     * Call once per field, usually right after the form is set up - makes
     * the field read-only (so the only way to set a date is through the
     * picker, keeping the stored format consistent) and wires the click.
     */
    public static void adjuntarA(JTextField campo) {
        campo.setEditable(false);
        campo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        campo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mostrarCalendario(campo);
            }
        });
    }

    private static void mostrarCalendario(JTextField campo) {
        JDialog popup = new JDialog(SwingUtilities.getWindowAncestor(campo));
        popup.setUndecorated(true);
        popup.setModal(false);

        LocalDate inicial = parsear(campo.getText());
        YearMonth[] mesActual = {YearMonth.from(inicial != null ? inicial : LocalDate.now())};

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setBackground(Color.WHITE);

        JLabel etiquetaMes = new JLabel("", SwingConstants.CENTER);
        etiquetaMes.setFont(etiquetaMes.getFont().deriveFont(Font.BOLD));
        JButton anterior = new JButton("<");
        JButton siguiente = new JButton(">");
        anterior.setMargin(new Insets(2, 6, 2, 6));
        siguiente.setMargin(new Insets(2, 6, 2, 6));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.add(anterior, BorderLayout.WEST);
        cabecera.add(etiquetaMes, BorderLayout.CENTER);
        cabecera.add(siguiente, BorderLayout.EAST);
        panel.add(cabecera, BorderLayout.NORTH);

        JPanel grilla = new JPanel(new GridLayout(0, 7, 2, 2));
        grilla.setBackground(Color.WHITE);
        panel.add(grilla, BorderLayout.CENTER);

        Runnable[] actualizar = new Runnable[1];
        actualizar[0] = () -> {
            grilla.removeAll();

            String nombreMes = mesActual[0].getMonth().getDisplayName(TextStyle.FULL, ESPANOL);
            etiquetaMes.setText(capitalizar(nombreMes) + " " + mesActual[0].getYear());

            String[] diasSemana = {"L", "M", "M", "J", "V", "S", "D"};
            for (String d : diasSemana) {
                JLabel l = new JLabel(d, SwingConstants.CENTER);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                grilla.add(l);
            }

            LocalDate primerDia = mesActual[0].atDay(1);
            int diaSemanaInicio = primerDia.getDayOfWeek().getValue(); // 1=lunes..7=domingo
            for (int i = 1; i < diaSemanaInicio; i++) {
                grilla.add(new JLabel(""));
            }

            int diasEnMes = mesActual[0].lengthOfMonth();
            for (int dia = 1; dia <= diasEnMes; dia++) {
                int diaFinal = dia;
                JButton boton = new JButton(String.valueOf(dia));
                boton.setMargin(new Insets(2, 2, 2, 2));
                boton.setFocusPainted(false);
                boton.addActionListener(e -> {
                    LocalDate seleccionada = mesActual[0].atDay(diaFinal);
                    campo.setText(seleccionada.format(FORMATO));
                    popup.dispose();
                });
                grilla.add(boton);
            }

            panel.revalidate();
            panel.repaint();
            popup.pack();
        };

        anterior.addActionListener(e -> {
            mesActual[0] = mesActual[0].minusMonths(1);
            actualizar[0].run();
        });
        siguiente.addActionListener(e -> {
            mesActual[0] = mesActual[0].plusMonths(1);
            actualizar[0].run();
        });

        actualizar[0].run();

        popup.setContentPane(panel);
        popup.pack();

        Point ubicacion = campo.getLocationOnScreen();
        popup.setLocation(ubicacion.x, ubicacion.y + campo.getHeight());

        popup.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                popup.dispose();
            }
        });

        popup.setVisible(true);
    }

    /**
     * Parses a dd/mm/yyyy string, returning null if it doesn't match
     * (e.g. an old free-text report, or an empty field) rather than
     * throwing - callers should treat null as "no valid date yet".
     */
    public static LocalDate parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), FORMATO);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String capitalizar(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}