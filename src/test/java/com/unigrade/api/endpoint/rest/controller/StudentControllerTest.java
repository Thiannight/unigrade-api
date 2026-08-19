package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.endpoint.event.EventProducer;
import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.service.PdfReportService;
import com.unigrade.api.service.ReportService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

  @Mock private ReportService reportService;

  @Mock private PdfReportService pdfReportService;

  @Mock private EventProducer<ReportEmailRequested> reportEmailEventProducer;

  private StudentController controller;

  @BeforeEach
  void setUp() {
    controller = new StudentController(reportService, pdfReportService, reportEmailEventProducer);
  }

  @Test
  void getReport_json_returnsReportBody() {
    StudentReport report =
        new StudentReport(
            "STD00001", "Ada", "Lovelace", ReportStatus.COMPLETE, 180, 180, List.of(), null);
    when(reportService.generate("STD00001", Level.L1)).thenReturn(report);

    ResponseEntity<?> response = controller.getReport("STD00001", true, Level.L1);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(report, response.getBody());
  }

  @Test
  void getReport_pdf_returnsPdfBody() {
    StudentReport report =
        new StudentReport(
            "STD00001", "Ada", "Lovelace", ReportStatus.COMPLETE, 180, 180, List.of(), null);
    byte[] pdf = "pdf-bytes".getBytes();
    when(reportService.generate("STD00001", null)).thenReturn(report);
    when(pdfReportService.generate(report)).thenReturn(pdf);

    ResponseEntity<?> response = controller.getReport("STD00001", false, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
    assertEquals(pdf, response.getBody());
  }

  @Test
  void emailReport_producesEventAndReturnsAccepted() {
    ResponseEntity<Void> response = controller.emailReport("STD00001", Level.L1);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ReportEmailRequested>> captor = ArgumentCaptor.forClass(List.class);

    verify(reportEmailEventProducer).accept(captor.capture());

    ReportEmailRequested event = captor.getValue().get(0);

    assertEquals("STD00001", event.getStudentId());
    assertEquals(Level.L1, event.getLevel());
  }

  @Test
  void emailReport_withoutLevel_producesEvent() {
    ResponseEntity<Void> response = controller.emailReport("STD00002", null);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ReportEmailRequested>> captor = ArgumentCaptor.forClass(List.class);

    verify(reportEmailEventProducer).accept(captor.capture());

    ReportEmailRequested event = captor.getValue().get(0);

    assertEquals("STD00002", event.getStudentId());
    assertEquals(null, event.getLevel());
  }

  @Test
  void emailReport_performsAuthorizationCheckBeforePublishing() {
    controller.emailReport("STD00003", null);

    verify(reportService).generate("STD00003", null);
  }
}
