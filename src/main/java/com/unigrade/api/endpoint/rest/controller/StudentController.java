package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.service.PdfReportService;
import com.unigrade.api.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

  @GetMapping("/{studentId}/report")
  public ResponseEntity<?> getReport(
      @PathVariable String studentId, @RequestParam(defaultValue = "false") boolean json) {
    var report = reportService.generate(studentId);
    return (json)
        ? ResponseEntity.ok(report)
        : ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                HttpHeaders.CONTENT_DISPOSITION, "inline; filename=report-" + studentId + ".pdf")
            .body(pdfReportService.generate(report));
  }
}
