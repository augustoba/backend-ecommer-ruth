package com.saasweb.core.arca;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.saasweb.core.order.Order;
import com.saasweb.core.order.OrderLine;
import com.saasweb.core.settings.SiteSettings;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

/**
 * PDF simple de la factura/ticket de una venta — a diferencia del
 * comprobante que ve ARCA (que no tiene ítems detallados, sólo importes
 * agregados por alícuota, ver {@code ArcaWsfeClient}), este PDF SÍ lista
 * cada línea del pedido: es donde en la práctica viven los "ítems
 * detallados" que pidió el usuario (ítem 3 de la ronda de mejoras).
 */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generate(Order order, SiteSettings settings) {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font small = new Font(Font.HELVETICA, 8, Font.NORMAL);

            doc.add(new Paragraph(settings.getStoreName(), titleFont));
            boolean esFactura = order.getInvoiceType() != null && order.getInvoiceType().startsWith("FACTURA_");
            String tipoTexto = esFactura ? order.getInvoiceType().replace("_", " ") : "Ticket interno (no fiscal)";
            doc.add(new Paragraph(tipoTexto + " — Pedido " + order.getCode(), bold));
            doc.add(new Paragraph("Fecha: " + FECHA_FMT.format(order.getCreatedAt().atZone(ZoneId.systemDefault())), small));
            doc.add(new Paragraph("Cliente: " + order.getCustomerName(), small));
            if (esFactura) {
                doc.add(new Paragraph("CAE: " + order.getInvoiceCae() + " — Vto: "
                        + formatCae(order.getInvoiceCaeVencimiento()) + " — N°: "
                        + order.getInvoicePuntoVenta() + "-" + order.getInvoiceNumber(), small));
            }
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{4, 1, 1, 2, 2});
            table.setWidthPercentage(100);
            addHeaderCell(table, "Producto", bold);
            addHeaderCell(table, "Talle", bold);
            addHeaderCell(table, "Cant.", bold);
            addHeaderCell(table, "Precio unit.", bold);
            addHeaderCell(table, "Subtotal", bold);

            for (OrderLine l : order.getLines()) {
                if (!l.isAccepted()) continue;
                BigDecimal subtotal = l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity()));
                table.addCell(new PdfPCell(new Paragraph(l.getProductName(), normal)));
                table.addCell(new PdfPCell(new Paragraph(l.getSize(), normal)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(l.getQuantity()), normal)));
                table.addCell(new PdfPCell(new Paragraph(money(l.getUnitPrice()), normal)));
                table.addCell(new PdfPCell(new Paragraph(money(subtotal), normal)));
            }
            doc.add(table);
            doc.add(new Paragraph(" "));

            Paragraph total = new Paragraph("Total: $" + money(order.getTotal()), new Font(Font.HELVETICA, 12, Font.BOLD));
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);

            if (esFactura && order.getInvoiceQrUrl() != null) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Validá este comprobante en: " + order.getInvoiceQrUrl(), small));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF de la factura: " + e.getMessage(), e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setGrayFill(0.9f);
        table.addCell(cell);
    }

    private String money(BigDecimal v) {
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatCae(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return yyyymmdd;
        return yyyymmdd.substring(6, 8) + "/" + yyyymmdd.substring(4, 6) + "/" + yyyymmdd.substring(0, 4);
    }
}
