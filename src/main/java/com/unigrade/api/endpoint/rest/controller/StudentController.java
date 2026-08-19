package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.endpoint.event.EventProducer;
import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.service.PdfReportService;
import com.unigrade.api.service.ReportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

  private final ReportService reportService;
  private final PdfReportService pdfReportService;
  private final EventProducer<ReportEmailRequested> reportEmailEventProducer;

  @GetMapping("/{studentId}/report")
  public ResponseEntity<?> getReport(
      @PathVariable String studentId,
      @RequestParam(defaultValue = "false") boolean json,
      @RequestParam(required = false) Level level) {

    StudentReport report = reportService.generate(studentId, level);

    if (json) {
      return ResponseEntity.ok(report);
    }

    byte[] pdf = pdfReportService.generate(report);

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
  }

  @GetMapping("/{studentId}/report/email")
  public ResponseEntity<Void> emailReport(
      @PathVariable String studentId, @RequestParam(required = false) Level level) {

    // This performs the authorization check before publishing the event.
    reportService.generate(studentId, level);

    var event = ReportEmailRequested.builder().studentId(studentId).level(level).build();

    reportEmailEventProducer.accept(List.of(event));

    return ResponseEntity.accepted().build();
  }
}
