package com.unigrade.api.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.unigrade.api.model.CourseReportEntry;
import com.unigrade.api.model.LevelReport;
import com.unigrade.api.model.StudentReport;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PdfReportService {

  private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
  private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
  private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

  public byte[] generate(StudentReport report) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 36, 36, 36, 36);
    try {
      PdfWriter.getInstance(document, out);
      document.open();

      document.add(new Paragraph(report.firstName() + " " + report.lastName(), HEADER_FONT));
      document.add(new Paragraph("ID: " + report.studentId(), NORMAL_FONT));
      document.add(
          new Paragraph(
              "Status: "
                  + report.status().name()
                  + " ("
                  + report.totalCredits()
                  + "/"
                  + report.requiredCredits()
                  + " credits)",
              NORMAL_FONT));
      document.add(new Paragraph(" ", NORMAL_FONT));

      for (LevelReport levelReport : report.levels()) {
        document.add(
            new Paragraph(
                levelReport.level().name()
                    + " - Status: "
                    + levelReport.status().name()
                    + " ("
                    + levelReport.totalCredits()
                    + "/"
                    + levelReport.requiredCredits()
                    + " credits)",
                HEADER_FONT));
        addCourseTable(document, levelReport);
        if (levelReport.overallAverage() != null) {
          document.add(
              new Paragraph(
                  "Level Average: " + formatAverage(levelReport.overallAverage()), BOLD_FONT));
        }
        document.add(new Paragraph(" ", NORMAL_FONT));
      }

      document.add(new Paragraph(" ", NORMAL_FONT));
      String overallAvg =
          report.overallAverage() != null ? formatAverage(report.overallAverage()) : "N/A";
      document.add(new Paragraph("Overall Average: " + overallAvg, HEADER_FONT));

      document.close();
    } catch (DocumentException e) {
      throw new RuntimeException("Failed to generate PDF report", e);
    }
    return out.toByteArray();
  }

  private void addCourseTable(Document document, LevelReport levelReport) throws DocumentException {
    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);
    table.setWidths(new float[] {3f, 1.5f, 1f, 1.2f, 1.3f});

    addHeaderCell(table, "Course");
    addHeaderCell(table, "Ref");
    addHeaderCell(table, "Credits");
    addHeaderCell(table, "Average");
    addHeaderCell(table, "Completed");
    table.setHeaderRows(1);

    for (CourseReportEntry course : levelReport.courses()) {
      addCell(table, course.title());
      addCell(table, course.reference());
      addCell(table, String.valueOf(course.credits()));
      addCell(table, course.average() != null ? formatAverage(course.average()) : "-");
      addCell(table, course.completed() ? "Yes" : "No");
    }

    document.add(table);
  }

  private void addHeaderCell(PdfPTable table, String text) {
    PdfPCell cell = new PdfPCell(new Phrase(text, BOLD_FONT));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(cell);
  }

  private void addCell(PdfPTable table, String text) {
    PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(cell);
  }

  private String formatAverage(BigDecimal average) {
    return average.setScale(2, java.math.RoundingMode.HALF_UP).toString();
  }
}
