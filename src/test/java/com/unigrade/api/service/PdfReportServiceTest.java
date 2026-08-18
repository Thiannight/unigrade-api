package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import com.unigrade.api.model.CourseReportEntry;
import com.unigrade.api.model.ExamScore;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.LevelReport;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.Specialization;
import com.unigrade.api.model.StudentReport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class PdfReportServiceTest {

  private final PdfReportService service = new PdfReportService();

  @Test
  void generate_withFullReport_returnsPdf() {
    StudentReport report = buildFullReport();

    byte[] pdf = service.generate(report);

    assertTrue(pdf.length > 0);
    assertTrue(startsWithPdf(pdf));
  }

  @Test
  void generate_withEmptyLevels_returnsPdf() {
    StudentReport report =
        new StudentReport(
            "STD001",
            "Alice",
            "Dupont",
            Specialization.TN,
            ReportStatus.TEMPORARY,
            0,
            0,
            List.of(),
            null);

    byte[] pdf = service.generate(report);

    assertTrue(pdf.length > 0);
    assertTrue(startsWithPdf(pdf));
  }

  @Test
  void generate_withNullAverages_returnsPdf() {
    CourseReportEntry course =
        new CourseReportEntry(
            UUID.randomUUID(), "P-2024", "C-101", "Math", (short) 6, false, null, List.of());
    LevelReport level =
        new LevelReport(Level.L1, ReportStatus.TEMPORARY, 6, 60, null, List.of(course));
    StudentReport report =
        new StudentReport(
            "STD002",
            "Bob",
            "Martin",
            Specialization.EL,
            ReportStatus.TEMPORARY,
            6,
            60,
            List.of(level),
            null);

    byte[] pdf = service.generate(report);

    assertTrue(pdf.length > 0);
    assertTrue(startsWithPdf(pdf));
  }

  @Test
  void generate_whenDocumentFails_throwsRuntimeException() {
    try (MockedStatic<PdfWriter> mocked = mockStatic(PdfWriter.class)) {
      mocked
          .when(() -> PdfWriter.getInstance(any(), any()))
          .thenThrow(new DocumentException("mocked failure"));

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> service.generate(buildFullReport()));

      assertEquals("Failed to generate PDF report", ex.getMessage());
    }
  }

  private StudentReport buildFullReport() {
    ExamScore exam1 =
        new ExamScore(
            UUID.randomUUID(),
            Instant.parse("2024-05-01T09:00:00Z"),
            new BigDecimal("0.4"),
            new BigDecimal("12.0"));
    ExamScore exam2 =
        new ExamScore(
            UUID.randomUUID(),
            Instant.parse("2024-06-01T09:00:00Z"),
            new BigDecimal("0.6"),
            new BigDecimal("16.0"));
    CourseReportEntry course =
        new CourseReportEntry(
            UUID.randomUUID(),
            "P-2024",
            "C-101",
            "Algebra",
            (short) 6,
            true,
            new BigDecimal("14.40"),
            List.of(exam1, exam2));
    LevelReport l2 =
        new LevelReport(
            Level.L2, ReportStatus.TEMPORARY, 6, 60, new BigDecimal("14.40"), List.of(course));
    return new StudentReport(
        "STD001",
        "Alice",
        "Dupont",
        Specialization.TN,
        ReportStatus.TEMPORARY,
        6,
        60,
        List.of(l2),
        new BigDecimal("14.40"));
  }

  private boolean startsWithPdf(byte[] bytes) {
    byte[] header = new byte[] {'%', 'P', 'D', 'F'};
    if (bytes.length < header.length) return false;
    for (int i = 0; i < header.length; i++) {
      if (bytes[i] != header[i]) return false;
    }
    return true;
  }
}
