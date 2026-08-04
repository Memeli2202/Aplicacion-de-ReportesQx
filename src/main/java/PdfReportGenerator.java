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
import java.util.Iterator;
import java.util.List;


public class PdfReportGenerator {

    private static final float LOGO_ANCHO = 200f;
    private static final float LOGO_OFFSET = 35f;
    private static final float HEADER_ALTO_APROX = 66f;

    public static void generar(Reporte reporte, List<DialogoImagenes.ImagenComentario> imagenes, File destino,
                               ImageData logo, ImageData sello, ImageData marcaAgua) throws IOException {

        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(destino.getAbsolutePath()));
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new MarcasFijas(logo, marcaAgua));

        try(Document documentoFinal = new Document(pdfDoc, PageSize.LETTER)){

            documentoFinal.setMargins(40,40,36,40);

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
                Table grid = new Table(UnitValue.createPercentArray(new float[]{1,1,1})).useAllAvailableWidth();

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
                documentoFinal.add(grid);
                documentoFinal.add(new Paragraph(" "));
                documentoFinal.add(separador());

                //comments for images if there are any
                boolean hayComentarios = imagenes.stream().anyMatch(imagenComentario ->
                        imagenComentario.getComentario() != null && !imagenComentario.getComentario().trim().isEmpty());

                if(hayComentarios) {
                    documentoFinal.add(new Paragraph("Comentarios de las Imágenes: ").simulateBold().setFontSize(14));
                    numero = 1;
                    for (DialogoImagenes.ImagenComentario imagenYComentario : imagenes) {
                        String comentario = imagenYComentario.getComentario();
                        if (comentario == null || comentario.trim().isEmpty()) {
                            numero++;
                            continue;
                        }
                        documentoFinal.add(new Paragraph("Imagen " + numero + ". ").simulateBold().setFontSize(12));
                        documentoFinal.add(new Paragraph(comentario).setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(11));
                        numero++;
                    }
                    documentoFinal.add(new Paragraph(" "));
                    documentoFinal.add(separador());
                }

            }

            if(!safe(reporte.getResumenQx()).isBlank()) {
                Div seccionResumen = new Div().setKeepTogether(true);
                seccionResumen.add(new Paragraph("Comentarios del Procedimiento:").setFontSize(14).simulateBold());
                seccionResumen.add(new Paragraph(safe(reporte.getResumenQx())).setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(12));
                seccionResumen.add(new Paragraph(" "));
                seccionResumen.add(separador());
                documentoFinal.add(seccionResumen);
            }


            boolean hayEnzians = !safe(reporte.getEnzianA()).isBlank() || !safe(reporte.getEnzianB()).isBlank()
                    || !safe(reporte.getEnzianB2()).isBlank() || !safe(reporte.getEnzianC()).isBlank()
                    || !safe(reporte.getEnzianF()).isBlank() || !safe(reporte.getEnzianO()).isBlank()
                    || !safe(reporte.getEnzianO2()).isBlank() || !safe(reporte.getEnzianP()).isBlank()
                    || !safe(reporte.getEnzianT()).isBlank() || !safe(reporte.getEnzianT2()).isBlank();
            if(hayEnzians ) {
                //Enzian classification
                Div seccionEnzian = new Div().setKeepTogether(true);
                seccionEnzian.add(new Paragraph("Clasificación #enzian(s):").setFontSize(14).simulateBold());
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
                seccionEnzian.add(enzian);
                seccionEnzian.add(new Paragraph(" "));
                seccionEnzian.add(separador());
                documentoFinal.add(seccionEnzian);
            }


            if(!safe(reporte.getPostQx()).isBlank()) {
                Div seccionPost = new Div().setKeepTogether(true);
                seccionPost.add(new Paragraph("Que esperar luego de mi Procedimiento:").setFontSize(14).simulateBold());
                seccionPost.add(new Paragraph(safe(reporte.getPostQx())).setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(12));
                seccionPost.add(new Paragraph(" "));
                documentoFinal.add(seccionPost);
            }

            //adding sello after everything else is generated
            if(sello != null) {
                float xIzquierda = 36f;
                int totalPaginas = pdfDoc.getNumberOfPages();


                //doctor info
                float anchoTexto = 210f;
                float yTexto = 40f;
                Div bloqueTexto = new Div();
                bloqueTexto.setFixedPosition(totalPaginas, xIzquierda, yTexto, anchoTexto);
                bloqueTexto.add(new Paragraph("DRA. MELANIE D. PORTER").simulateBold().setFontSize(11).setMargin(0));
                bloqueTexto.add(new Paragraph("Gineco-Obstetra").setFontSize(8).setMargin(0));
                bloqueTexto.add(new Paragraph("Reproducción Humana Asistida").setFontSize(8).setMargin(0));
                bloqueTexto.add(new Paragraph("Laparo-Histeroscopía Avanzada").setFontSize(8).setMargin(0));
                bloqueTexto.add(new Paragraph("Endometriosis Infiltrativa Profunda").setFontSize(8).setMargin(0));
                documentoFinal.add(bloqueTexto);

                float anchoSello = 130f;
                float altoSello = anchoSello * sello.getHeight() / sello.getWidth();
                float alturaTextoAprox = 70f;

                float ySello = yTexto + alturaTextoAprox + 8;
                Image selloImg = new Image(sello);
                selloImg.scaleToFit(anchoSello, altoSello);
                selloImg.setFixedPosition(totalPaginas, xIzquierda, ySello);
                documentoFinal.add(selloImg);


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
     * loads an image bundled as a classpath resource
     * @param ruta where the file is located
     * @return null if the file isn't there, the image if it is
     */
//    private static ImageData cargarImagenDeRecursos(String ruta){
//        try(InputStream in = PdfReportGenerator.class.getResourceAsStream(ruta)){
//            if(in == null){
//                return null;
//            }
//            return ImageDataFactory.create(in.readAllBytes());
//        } catch (IOException e) {
//            return null;
//        }
//    }

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
