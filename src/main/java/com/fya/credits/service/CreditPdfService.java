package com.fya.credits.service;

import com.fya.credits.dto.response.CreditResponse;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Generates the same "credit certificate" PDF as credit-web's client-side
 * jsPDF export, but server-side — mobile downloads the finished file instead
 * of needing a PDF-rendering library on-device.
 */
@Service
public class CreditPdfService {
  private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
  private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, h:mm a", ES_CO);
  private static final Color INK = new Color(0x05, 0x22, 0x24);
  private static final Color MUTED = new Color(0x6b, 0x72, 0x80);
  private static final Color PANEL = new Color(0xf5, 0xfa, 0xf8);
  private static final Color BORDER = new Color(0xe3, 0xef, 0xeb);
  private static final Color GREEN = new Color(0x03, 0xa5, 0x65);
  private static final String LOGO_URL = "https://fyatest.cmescorcia.com/fya-mark.png";

  private final RestClient.Builder restClientBuilder;

  public CreditPdfService(RestClient.Builder restClientBuilder) {
    this.restClientBuilder = restClientBuilder;
  }

  public byte[] generate(CreditResponse credit) {
    try {
      Document document = new Document(PageSize.A4, 40, 40, 0, 40);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfWriter.getInstance(document, out);
      document.open();

      document.add(header());
      document.add(spacer(18));
      document.add(clientBlock(credit));
      document.add(spacer(14));
      document.add(amountPanel(credit));
      document.add(spacer(16));
      document.add(detailsGrid(credit));
      document.add(spacer(16));
      document.add(footnote());
      document.add(spacer(24));
      document.add(footer(credit));

      document.close();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo generar el PDF del crédito", ex);
    }
  }

  private Paragraph spacer(float height) {
    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(0);
    spacer.setLeading(height);
    return spacer;
  }

  private PdfPTable header() throws Exception {
    PdfPTable band = new PdfPTable(1);
    band.setWidthPercentage(100);
    PdfPCell cell = new PdfPCell();
    cell.setBackgroundColor(INK);
    cell.setBorder(0);
    cell.setPadding(16);

    PdfPTable inner = new PdfPTable(2);
    inner.setWidthPercentage(100);
    inner.setWidths(new float[] {1, 6});

    PdfPCell logoCell = new PdfPCell();
    logoCell.setBorder(0);
    logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    Image logo = fetchLogo();
    if (logo != null) {
      logo.scaleToFit(34, 34);
      logoCell.addElement(logo);
    }
    inner.addCell(logoCell);

    PdfPCell textCell = new PdfPCell();
    textCell.setBorder(0);
    textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    Paragraph title = new Paragraph("Fya Social Capital", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE));
    Paragraph subtitle = new Paragraph("Certificado de crédito", FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0xb7, 0xec, 0xe0)));
    textCell.addElement(title);
    textCell.addElement(subtitle);
    inner.addCell(textCell);

    cell.addElement(inner);
    band.addCell(cell);
    return band;
  }

  private Image fetchLogo() {
    try {
      byte[] bytes = restClientBuilder.build().get().uri(LOGO_URL).retrieve().body(byte[].class);
      return bytes == null ? null : Image.getInstance(bytes);
    } catch (Exception ex) {
      return null;
    }
  }

  private Paragraph clientBlock(CreditResponse credit) {
    Paragraph block = new Paragraph();
    block.add(new Chunk(clientFullName(credit), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, INK)));
    block.add(Chunk.NEWLINE);
    block.add(new Chunk("Cédula o ID: " + nullToDash(credit.clientDocument()), FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED)));
    return block;
  }

  private PdfPTable amountPanel(CreditResponse credit) {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    PdfPCell left = panelCell("VALOR DEL CRÉDITO", formatCurrency(credit.amount()), Element.ALIGN_LEFT, 18, GREEN);
    PdfPCell right = panelCell("ESTADO", Boolean.FALSE.equals(credit.isActive()) ? "Inactivo" : "Activo", Element.ALIGN_RIGHT, 12, INK);
    table.addCell(left);
    table.addCell(right);
    return table;
  }

  private PdfPCell panelCell(String label, String value, int align, float valueSize, Color valueColor) {
    PdfPCell cell = new PdfPCell();
    cell.setBackgroundColor(PANEL);
    cell.setBorderColor(BORDER);
    cell.setPadding(12);
    cell.setHorizontalAlignment(align);
    Paragraph labelP = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, MUTED));
    labelP.setAlignment(align);
    Paragraph valueP = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, valueSize, valueColor));
    valueP.setAlignment(align);
    valueP.setSpacingBefore(4);
    cell.addElement(labelP);
    cell.addElement(valueP);
    return cell;
  }

  private PdfPTable detailsGrid(CreditResponse credit) {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);

    BigDecimal[] estimate = estimatePayment(credit);

    PdfPTable left = new PdfPTable(1);
    left.addCell(plainCell("Comercial", nullToDash(credit.salespersonName())));
    left.addCell(plainCell("Tasa de interés mensual", credit.interestRate() + "%"));
    left.addCell(plainCell("Plazo", credit.termMonths() + " meses"));

    PdfPTable right = new PdfPTable(1);
    right.addCell(plainCell("Fecha de registro", formatDate(credit.createdAt())));
    right.addCell(plainCell("Cuota mensual estimada", formatCurrency(estimate[0])));
    right.addCell(plainCell("Total estimado a pagar", formatCurrency(estimate[1])));

    PdfPCell leftCell = new PdfPCell(left);
    leftCell.setBorder(0);
    leftCell.setPadding(0);
    PdfPCell rightCell = new PdfPCell(right);
    rightCell.setBorder(0);
    rightCell.setPadding(0);

    table.addCell(leftCell);
    table.addCell(rightCell);
    return table;
  }

  private PdfPCell plainCell(String label, String value) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(0);
    cell.setPaddingBottom(10);
    Paragraph labelP = new Paragraph(label.toUpperCase(ES_CO), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED));
    Paragraph valueP = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA, 11, INK));
    valueP.setSpacingBefore(2);
    cell.addElement(labelP);
    cell.addElement(valueP);
    return cell;
  }

  private Paragraph footnote() {
    Paragraph p = new Paragraph(
        "Cálculo estimado (amortización francesa, tasa mensual fija). Documento informativo, no constituye un pagaré.",
        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, MUTED));
    return p;
  }

  private PdfPTable footer(CreditResponse credit) {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

    PdfPCell left = new PdfPCell(new Paragraph("ID del crédito: " + credit.id(), font));
    left.setBorder(0);
    PdfPCell right = new PdfPCell(new Paragraph("Fya Social Capital · Prueba técnica de créditos", font));
    right.setBorder(0);
    right.setHorizontalAlignment(Element.ALIGN_RIGHT);

    table.addCell(left);
    table.addCell(right);
    return table;
  }

  private BigDecimal[] estimatePayment(CreditResponse credit) {
    BigDecimal principal = credit.amount() == null ? BigDecimal.ZERO : credit.amount();
    BigDecimal months = BigDecimal.valueOf(credit.termMonths() == null ? 0 : credit.termMonths());
    BigDecimal monthlyRate = (credit.interestRate() == null ? BigDecimal.ZERO : credit.interestRate())
        .divide(BigDecimal.valueOf(100), MathContext.DECIMAL64);

    if (principal.signum() <= 0 || months.signum() <= 0) {
      return new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO};
    }

    BigDecimal monthlyPayment;
    if (monthlyRate.signum() == 0) {
      monthlyPayment = principal.divide(months, MathContext.DECIMAL64);
    } else {
      double p = principal.doubleValue();
      double r = monthlyRate.doubleValue();
      double n = months.doubleValue();
      double payment = (p * r) / (1 - Math.pow(1 + r, -n));
      monthlyPayment = BigDecimal.valueOf(payment);
    }
    BigDecimal total = monthlyPayment.multiply(months);
    return new BigDecimal[] {
        monthlyPayment.setScale(0, RoundingMode.HALF_UP),
        total.setScale(0, RoundingMode.HALF_UP),
    };
  }

  private String clientFullName(CreditResponse credit) {
    if (credit.clientName() != null && !credit.clientName().isBlank()) {
      return credit.clientName();
    }
    StringBuilder builder = new StringBuilder();
    for (String part : new String[] {
        credit.clientFirstName(), credit.clientSecondName(),
        credit.clientFirstSurname(), credit.clientSecondSurname()}) {
      if (part != null && !part.isBlank()) {
        if (builder.length() > 0) {
          builder.append(' ');
        }
        builder.append(part);
      }
    }
    return builder.toString();
  }

  private String formatCurrency(BigDecimal amount) {
    NumberFormat currency = NumberFormat.getCurrencyInstance(ES_CO);
    currency.setMaximumFractionDigits(0);
    return currency.format(amount == null ? BigDecimal.ZERO : amount);
  }

  private String formatDate(Instant instant) {
    if (instant == null) {
      return "-";
    }
    return DATE_FORMAT.format(instant.atZone(BOGOTA));
  }

  private String nullToDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
