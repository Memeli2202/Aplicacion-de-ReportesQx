import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.function.Supplier;

/**
 * Makes a JTextField format an identity document while it is typed (see
 * Documento): a cedula only accepts digits and shows the dashes by itself
 * (000-0000000-0), a passport accepts letters and digits in capitals. Typing,
 * pasting, deleting and setText() all go through the same rule, and the caret
 * stays next to the characters being edited instead of jumping to the end.
 */
public class FiltroDocumento extends DocumentFilter {

    private final JTextField campo;
    private final Supplier<String> tipo; //"cedula" / "pasaporte", read every time so a type change applies at once

    public FiltroDocumento(JTextField campo, Supplier<String> tipo) {
        this.campo = campo;
        this.tipo = tipo;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr) throws BadLocationException {
        reemplazar(fb, offset, 0, texto == null ? "" : texto);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int largo) throws BadLocationException {
        reemplazar(fb, offset, largo, "");
    }

    @Override
    public void replace(FilterBypass fb, int offset, int largo, String texto, AttributeSet attr) throws BadLocationException {
        reemplazar(fb, offset, largo, texto == null ? "" : texto);
    }

    private void reemplazar(FilterBypass fb, int offset, int largo, String texto) throws BadLocationException {
        Document doc = fb.getDocument();
        String actual = doc.getText(0, doc.getLength());
        String nuevo = actual.substring(0, offset) + texto + actual.substring(offset + largo);
        String tipoActual = tipo.get();

        //how many real characters sit before the caret - it is put back after the same ones
        int antesDelCursor = Documento.paraGuardar(tipoActual, nuevo.substring(0, offset + texto.length())).length();
        String formateado = Documento.paraMostrar(tipoActual, nuevo);
        if (!formateado.equals(actual)) {
            fb.replace(0, doc.getLength(), formateado, null);
        }

        int posicion = posicionTrasCaracteres(formateado, antesDelCursor);
        SwingUtilities.invokeLater(() -> {
            if (posicion <= campo.getDocument().getLength()) {
                campo.setCaretPosition(posicion);
            }
        });
    }

    /** Index right after the n-th letter/digit of the text (dashes don't count). */
    static int posicionTrasCaracteres(String texto, int n) {
        if (n <= 0) {
            return 0;
        }
        int contados = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (Character.isLetterOrDigit(texto.charAt(i))) {
                contados++;
                if (contados == n) {
                    return i + 1;
                }
            }
        }
        return texto.length();
    }
}
