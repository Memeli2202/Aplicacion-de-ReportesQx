import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.pdf.event.*;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class PdfReportGenerator {

    private static final float LOGO_ANCHO = 200f;
    private static final float LOGO_OFFSET = 35f;
    private static final float HEADER_ALTO_APROX = 66f;
    private static final float MARGEN_INFERIOR = 36f;
    //how many trailing lines of the section right before the signature get
    //held back to travel with it as one unit, so the signature never lands
    //alone on an otherwise near-empty final page
    private static final int LINEAS_RESERVADAS_FIRMA = 2;

    public static void generar(Reporte reporte, List<DialogoImagenes.ImagenComentario> imagenes, File destino,
                               ImageData logo, ImageData sello, ImageData marcaAgua) throws IOException {

        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(destino.getAbsolutePath()));
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new MarcasFijas(logo, marcaAgua));

        try(Document documentoFinal = new Document(pdfDoc, PageSize.LETTER)){

            documentoFinal.setMargins(40,40,MARGEN_INFERIOR,40);

            if(logo != null) {
                float altoLogo = LOGO_ANCHO * logo.getHeight() / logo.getWidth();
                documentoFinal.add(new Div().setHeight(altoLogo + LOGO_OFFSET));
                documentoFinal.add(separador());

                float xTexto = LOGO_ANCHO + LOGO_OFFSET + 30;
                float anchoTexto = 280f;
                float yTexto = (PageSize.LETTER.getHeight() - LOGO_OFFSET) - HEADER_ALTO_APROX - 10;

                Div bloqueHeader = new Div();
                bloqueHeader.setFixedPosition(1, xTexto, yTexto, anchoTexto);

                bloqueHeader.add(new Paragraph("DRA. MELANIE D. PORTER").simulateBold().setFontSize(11).setMargin(0));
                bloqueHeader.add(new Paragraph("Clínica Universitaria Unión Médica del Norte").setFontSize(9).setMargin(0));
                bloqueHeader.add(new Paragraph("Avenida Juan Pablo Duarte No. 176").setFontSize(9).setMargin(0));
                bloqueHeader.add(new Paragraph("Torre E 6to Piso • Suite 648").setFontSize(9).setMargin(0).simulateBold());
                bloqueHeader.add(new Paragraph("Citas por WhatsApp: 809-975-9183 | IG: @dramelanieporter").setFontSize(9).setMargin(0));
                bloqueHeader.add(new Paragraph("E-mail: consultoriodramelanieporter@gmail.com").setFontSize(9).setMargin(0));
                documentoFinal.add(bloqueHeader);

            }


            String procedimiento = safe(reporte.getProcedimiento());
            String titulo = procedimiento.isBlank() ? "Reporte Quirúrgico" : procedimiento;

            Div seccionInfo = new Div().setKeepTogether(true);
            seccionInfo.add(new Paragraph(titulo).simulateBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            seccionInfo.add(separador());
            seccionInfo.add(new Paragraph("Nombre: " + safe(reporte.getNombre())).simulateBold().setFontSize(12).setMarginBottom(2));

            Table datos = new Table(UnitValue.createPercentArray(new float[]{1,4,1,4})).useAllAvailableWidth();
            datos.addCell(etiquetaCompacta("Edad:").simulateBold());
            datos.addCell(celdaCompacta(safe(reporte.getEdad())));
            datos.addCell(etiquetaCompacta("Cédula:").simulateBold());
            datos.addCell(celdaCompacta(safe(reporte.getCedula())));
            datos.addCell(etiquetaCompacta("ARS:").simulateBold());
            datos.addCell(celdaCompacta(safe(reporte.getArs())));
            datos.addCell(etiquetaCompacta("Fecha:").simulateBold());
            datos.addCell(celdaCompacta(safe(reporte.getFecha())));
            seccionInfo.add(datos);
            seccionInfo.add(new Div().setHeight(6));
            seccionInfo.add(separador());
            documentoFinal.add(seccionInfo);

            //adding images
            if(imagenes != null && !imagenes.isEmpty()) {
                Table grid = new Table(UnitValue.createPercentArray(new float[]{1,1})).useAllAvailableWidth();

                int numero = 1;
                for(DialogoImagenes.ImagenComentario imagenYComentario : imagenes) {
                    BufferedImage numerada = conNumero(imagenYComentario.getImagen(), numero);
                    Image img = new Image(ImageDataFactory.create(toPngBytes(numerada)));
                    img.setWidth(UnitValue.createPercentValue(100));
                    img.setHorizontalAlignment(HorizontalAlignment.CENTER);

                    Cell celdaImagen = new Cell().add(img).setBorder(Border.NO_BORDER);
                    grid.addCell(celdaImagen);
                    numero++;
                }
                //with 2 columns, an odd number of images leaves the last row with
                //only 1 of 2 cells filled - iText warns about that incomplete row
                //("Last row is not completed") since border-collapsing on a short
                //row is ambiguous. Pad it out with an empty, borderless cell.
                if (imagenes.size() % 2 != 0) {
                    grid.addCell(new Cell().setBorder(Border.NO_BORDER));
                }
                documentoFinal.add(grid);

                //comments for images if there are any
                boolean hayComentarios = imagenes.stream().anyMatch(imagenComentario ->
                        imagenComentario.getComentario() != null && !imagenComentario.getComentario().trim().isEmpty());

                if(hayComentarios) {
                    //the divider lives on this heading's own top border now (see
                    //tituloSeccion), not as a standalone separador() before it,
                    //so it can't be stranded if this heading gets pushed to a new page
                    documentoFinal.add(tituloSeccion("Comentarios de las Imágenes: ").setKeepWithNext(true));
                    numero = 1;
                    for (DialogoImagenes.ImagenComentario imagenYComentario : imagenes) {
                        String comentario = imagenYComentario.getComentario();
                        if (comentario == null || comentario.trim().isEmpty()) {
                            numero++;
                            continue;
                        }
                        documentoFinal.add(new Paragraph("Imagen " + numero + ". ").simulateBold().setFontSize(12).setKeepWithNext(true));
                        documentoFinal.add(new Paragraph(comentario).setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(11));
                        numero++;
                    }
                }
                //no standalone trailing separator here either way - whichever
                //section comes next (resumen/enzian/post) carries its own
                //leading divider via tituloSeccion()

            }

            boolean hayEnzians = !safe(reporte.getEnzianA()).isBlank() || !safe(reporte.getEnzianB()).isBlank()
                    || !safe(reporte.getEnzianB2()).isBlank() || !safe(reporte.getEnzianC()).isBlank()
                    || !safe(reporte.getEnzianF()).isBlank() || !safe(reporte.getEnzianO()).isBlank()
                    || !safe(reporte.getEnzianO2()).isBlank() || !safe(reporte.getEnzianP()).isBlank()
                    || !safe(reporte.getEnzianT()).isBlank() || !safe(reporte.getEnzianT2()).isBlank();

            //NOTE: resumenQx/postQx are intentionally rendered as individual
            //line-paragraphs rather than one big Paragraph. resumenQx/postQx are
            //free text with no length limit, so KeepTogether on the whole block
            //made iText give up on it entirely once it got taller than a page
            //(the "KeepTogether property will be ignored" warning) and dump a
            //half-rendered chunk. Splitting into lines also lets us hold back
            //the last couple of lines of whichever section sits right before
            //the signature (see colaParaFirma below), so that text travels
            //together with the signature instead of it landing alone on an
            //otherwise near-empty final page.
            List<Paragraph> parrafosResumen = safe(reporte.getResumenQx()).isBlank() ? null : parrafosDe(reporte.getResumenQx());
            if (parrafosResumen != null && parrafosResumen.isEmpty()) parrafosResumen = null;
            List<Paragraph> parrafosPost = safe(reporte.getPostQx()).isBlank() ? null : parrafosDe(reporte.getPostQx());
            if (parrafosPost != null && parrafosPost.isEmpty()) parrafosPost = null;

            //Whichever of post/enzian/resumen is physically LAST before the
            //signature in the document's fixed order (post, then enzian, then
            //resumen - rendered in that priority since post comes after enzian
            //which comes after resumen) is the one that gets glued to it.
            //Enzian can be "cola" too - a small bounded table can be held back
            //as a whole unit just as easily as a couple of text lines can.
            boolean postEsCola = sello != null && parrafosPost != null;
            boolean enzianEsCola = sello != null && !postEsCola && hayEnzians;
            boolean resumenEsCola = sello != null && !postEsCola && !enzianEsCola && parrafosResumen != null;

            List<IBlockElement> colaParaFirma = new ArrayList<>();

            if(parrafosResumen != null) {
                renderSeccionTexto(documentoFinal, "Comentarios del Procedimiento:", parrafosResumen, resumenEsCola, colaParaFirma);
            }

            if(hayEnzians ) {
                //Enzian classification - small and bounded, so it always fits on
                //one full page. The heading's own top border (tituloSeccion) is
                //added right alongside the table, so wherever this ends up (its
                //own page, or glued to the signature below), its divider travels
                //with it instead of being left behind.
                Paragraph encabezadoEnzian = tituloSeccion("Clasificación #enzian(s):");
                Table enzian = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1, 1, 1}))
                        .useAllAvailableWidth();
                for (String columna : new String[]{"P", "O", "T", "A", "B", "C", "F"}) {
                    enzian.addHeaderCell(etiqueta(columna));
                }
                enzian.addCell(celda(safe(reporte.getEnzianP())));
                enzian.addCell(celda(combinar(reporte.getEnzianO(), reporte.getEnzianO2())));
                enzian.addCell(celda(combinar(reporte.getEnzianT(), reporte.getEnzianT2())));
                enzian.addCell(celda(safe(reporte.getEnzianA())));
                enzian.addCell(celda(combinar(reporte.getEnzianB(), reporte.getEnzianB2())));
                enzian.addCell(celda(safe(reporte.getEnzianC())));
                enzian.addCell(celda(safe(reporte.getEnzianF())));

                if (enzianEsCola) {
                    //this is the section right before the signature - don't
                    //render it on its own, queue it to be glued to the
                    //signature block instead (see colaParaFirma below)
                    colaParaFirma.add(encabezadoEnzian);
                    colaParaFirma.add(enzian);
                } else {
                    Div seccionEnzian = new Div().setKeepTogether(true);
                    seccionEnzian.add(encabezadoEnzian);
                    seccionEnzian.add(enzian);
                    documentoFinal.add(seccionEnzian);
                }
            }


            if(parrafosPost != null) {
                renderSeccionTexto(documentoFinal, "Que esperar luego de mi Procedimiento:", parrafosPost, postEsCola, colaParaFirma);
            }

            //adding sello after everything else is generated - as NORMAL
            //flowing content now (not setFixedPosition), wrapped in its own
            //KeepTogether Div just like the Enzian table above. This is what
            //lets colaParaFirma (the held-back tail of whichever text section
            //came right before it) be glued into the SAME atomic block: if it
            //doesn't fit on the current page, the whole thing - real text and
            //signature together - moves to a fresh page as one piece, so the
            //signature never lands alone on an otherwise near-empty page.
            if(sello != null) {
                //deliberately NOT width-constrained: colaParaFirma may contain
                //held-back report-text lines that need the FULL page width to
                //match the rest of the paragraph they were split from. The
                //doctor-info lines below are short, plain (non-justified)
                //paragraphs, so they hug their own natural width regardless.
                Div bloqueFirma = new Div().setKeepTogether(true);

                for (IBlockElement elemento : colaParaFirma) {
                    bloqueFirma.add(elemento);
                }
                if (!colaParaFirma.isEmpty()) {
                    bloqueFirma.add(new Paragraph(" "));
                }

                float anchoSello = 156f;
                float altoSello = anchoSello * sello.getHeight() / sello.getWidth();
                Image selloImg = new Image(sello);
                selloImg.scaleToFit(anchoSello, altoSello);
                selloImg.setMarginBottom(4f);
                bloqueFirma.add(selloImg);

                bloqueFirma.add(new Paragraph("DRA. MELANIE D. PORTER").simulateBold().setFontSize(11).setMargin(0));
                bloqueFirma.add(new Paragraph("Gineco-Obstetra").setFontSize(8).setMargin(0));
                bloqueFirma.add(new Paragraph("Reproducción Humana Asistida").setFontSize(8).setMargin(0));
                bloqueFirma.add(new Paragraph("Laparo-Histeroscopía Avanzada").setFontSize(8).setMargin(0));
                bloqueFirma.add(new Paragraph("Endometriosis Infiltrativa Profunda").setFontSize(8).setMargin(0));

                documentoFinal.add(bloqueFirma);
            }


        }
    }

    /**
     * draws the watermark on every page and the logo as a letterhead on page 1 only
     */
    private static class MarcasFijas extends AbstractPdfDocumentEventHandler{
        private final ImageData logo;
        private final ImageData marcaAgua;

        MarcasFijas(ImageData logo, ImageData marcaAgua) {
            this.logo = logo;
            this.marcaAgua = marcaAgua;
        }

        @Override
        public void onAcceptedEvent(AbstractPdfDocumentEvent event){
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDocument = docEvent.getDocument();
            PdfPage pdfPage = docEvent.getPage();
            Rectangle pageSize = pdfPage.getPageSize();
            PdfCanvas canvas = new PdfCanvas(pdfPage.newContentStreamBefore(), pdfPage.getResources(), pdfDocument);

            //background watermark
            if(marcaAgua != null) {
                canvas.saveState();
                canvas.setExtGState(new PdfExtGState().setFillOpacity(0.12f));

                //to rotate the image
                float centrox = pageSize.getWidth() / 2;
                float centroy = pageSize.getHeight() / 2;
                AffineTransform rotacion = AffineTransform.getRotateInstance(Math.toRadians(90), centrox, centroy);
                canvas.concatMatrix(rotacion);

                float ancho = pageSize.getWidth() * 0.75f;
                float alto = ancho * marcaAgua.getHeight() / marcaAgua.getWidth();
                float x = (pageSize.getWidth() - ancho) / 2;
                float y = (pageSize.getHeight() - alto) / 2;
                canvas.addImageFittedIntoRectangle(marcaAgua, new Rectangle(x, y, ancho, alto), false);
                canvas.restoreState();
            }

            if(logo != null && pdfDocument.getPageNumber(pdfPage) == 1){

                float alto = LOGO_ANCHO * logo.getHeight() / logo.getWidth();
                float x = pageSize.getLeft() + LOGO_OFFSET;
                float y = pageSize.getTop() - alto - LOGO_OFFSET;
                canvas.addImageFittedIntoRectangle(logo, new Rectangle(x, y, LOGO_ANCHO, alto), false);

                //vertical divider
                float xLinea = x + LOGO_ANCHO + 11f;
                float yLineaTop = pageSize.getTop() - LOGO_OFFSET;
                float yLineaBottom = yLineaTop - Math.max(alto, HEADER_ALTO_APROX);
                canvas.saveState();
                canvas.setStrokeColor(ColorConstants.LIGHT_GRAY);
                canvas.setLineWidth(0.75f);
                canvas.moveTo(xLinea, yLineaTop);
                canvas.lineTo(xLinea, yLineaBottom);
                canvas.stroke();
                canvas.restoreState();

            }

        }

    }

    /**
     * Returns a copy of the image with a numbered badge in the top left corner
     * so the number ends up baked into the picture itself
     * @param original is the original image
     * @param numero is the number to be assigned
     * @return the image with the number added
     */
    private static BufferedImage conNumero(BufferedImage original, int numero) {
        original = redimensionarSiEsNecesario(original);
        BufferedImage copia = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = copia.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, null);

        //brighten up the image
        RescaleOp brillo = new RescaleOp(1f, 12f, null);
        g2d.drawImage(original, brillo, 0, 0);

        String texto = String.valueOf(numero);
        int margen = 10;
        int diametro = Math.max(28, original.getWidth() / 12);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, (int) (diametro * 0.55)));
        FontMetrics fm = g2d.getFontMetrics();
        int x = margen + (diametro - fm.stringWidth(texto)) / 2;
        int y = margen + (diametro + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(texto, x, y);


        g2d.dispose();
        return copia;
    }

    private static BufferedImage redimensionarSiEsNecesario(BufferedImage original) {
        int maxLado = 1000;
        int ancho = original.getWidth();
        int alto = original.getHeight();

        if (ancho <= maxLado && alto <= maxLado) {
            return original;
        }

        double escala = Math.min((double) maxLado / ancho, (double) maxLado / alto);
        int nuevoAncho = (int) Math.round(ancho * escala);
        int nuevoAlto = (int) Math.round(alto * escala);

        BufferedImage redimensionada = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = redimensionada.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
        g2d.dispose();
        return redimensionada;
    }

    private static LineSeparator separador(){
        return new LineSeparator(new SolidLine());
    }

    /**
     * Splits free text (resumenQx/postQx) into one Paragraph per non-blank
     * line, instead of a single Paragraph with embedded \n line breaks.
     * Visually identical (each segment is still its own left-anchored,
     * justified block), but being separate elements lets the caller hold
     * back just the last few lines to travel with the signature block
     * (see renderSeccionTexto) instead of either forcing the WHOLE section
     * to stay together (breaks on long text) or leaving the signature to
     * land alone wherever the text happens to end.
     */
    private static List<Paragraph> parrafosDe(String texto) {
        List<Paragraph> parrafos = new ArrayList<>();
        boolean lineaAnteriorEnBlanco = false;
        boolean esPrimeraLinea = true;
        for (String linea : safe(texto).split("\n")) {
            if (linea.isBlank()) {
                lineaAnteriorEnBlanco = true;
                continue;
            }
            Paragraph p = new Paragraph(linea).setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(12).setMargin(0);
            if (lineaAnteriorEnBlanco && !esPrimeraLinea) {
                //the doctor left a blank line here on purpose to mark a new
                //paragraph - preserve that as real spacing instead of
                //silently collapsing it, which made every paragraph look
                //like it was stuck to the next one
                p.setMarginTop(8f);
            }
            parrafos.add(p);
            lineaAnteriorEnBlanco = false;
            esPrimeraLinea = false;
        }
        return parrafos;
    }

    /**
     * Renders a free-text section's heading + line-paragraphs. When
     * esCola is true (this section sits right before the signature),
     * the last LINEAS_RESERVADAS_FIRMA lines are NOT rendered here -
     * they're appended to colaParaFirma instead, to be glued to the
     * signature as one KeepTogether unit by the caller. If the section
     * is short enough that holding back that many lines would leave
     * nothing meaningful in normal flow, the heading itself is deferred
     * along with the whole body instead of being stranded alone.
     */
    private static void renderSeccionTexto(Document documentoFinal, String encabezado, List<Paragraph> parrafos,
                                           boolean esCola, List<IBlockElement> colaParaFirma) {
        int limite = esCola ? Math.max(0, parrafos.size() - LINEAS_RESERVADAS_FIRMA) : parrafos.size();

        if (limite > 0) {
            documentoFinal.add(tituloSeccion(encabezado).setKeepWithNext(true));
            for (int i = 0; i < limite; i++) {
                documentoFinal.add(parrafos.get(i));
            }
            documentoFinal.add(new Paragraph(" "));
        }

        if (esCola) {
            if (limite == 0) {
                colaParaFirma.add(tituloSeccion(encabezado).setKeepWithNext(true));
            }
            for (int i = limite; i < parrafos.size(); i++) {
                colaParaFirma.add(parrafos.get(i));
            }
        }
    }

    /**
     * A section heading with its divider line attached as a top border,
     * instead of a separate standalone LineSeparator element placed before it.
     *
     * Why: a standalone separador() added at the END of the previous section
     * is a completely independent element. If the section that follows it
     * can't fit on the remaining space of the page and gets pushed whole to
     * the next page (KeepTogether) or held back by KeepWithNext, the
     * separator - which isn't part of that relocated group - stays behind,
     * stranded on the previous page with nothing under it but blank space.
     * By making the line part of the heading's own box (a border), the line
     * can never be separated from its heading: wherever the heading ends up,
     * the divider goes with it.
     */
    private static Paragraph tituloSeccion(String texto) {
        return new Paragraph(texto)
                .simulateBold()
                .setFontSize(14)
                .setBorderTop(new SolidBorder(ColorConstants.BLACK, 0.75f))
                .setPaddingTop(10f)
                .setMarginTop(14f)
                .setMarginBottom(6f);
    }

    private static Cell celda(String texto){
        return new Cell().add(new Paragraph(texto)).setBorder(Border.NO_BORDER);
    }

    private static Cell celdaCompacta(String texto){
        return new Cell().add(new Paragraph(texto).setMargin(0)).setBorder(Border.NO_BORDER).setPadding(1);
    }

    private static String combinar(String principal, String secundario){
        String p = safe(principal);
        String s = safe(secundario);
        return s.isBlank() ? p : p + "/" + s;
    }

    private static Cell etiqueta(String texto){
        return new Cell().add(new Paragraph(texto)).setBorder(Border.NO_BORDER);
    }

    private static Cell etiquetaCompacta(String texto){
        return new Cell().add(new Paragraph(texto).setMargin(0)).setBorder(Border.NO_BORDER).setPadding(1);
    }

    private static String safe(String texto){
        return texto == null ? "" : texto;
    }

    private static byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        return os.toByteArray();
    }

}