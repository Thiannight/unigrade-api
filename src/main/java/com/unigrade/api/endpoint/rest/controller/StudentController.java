package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.endpoint.event.EventProducer;
import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.model.Level;
import com.unigrade.api.security.SecurityUtils;
import com.unigrade.api.service.ReportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
  private final EventProducer<ReportEmailRequested> reportEmailEventProducer;

  @GetMapping("/{studentId}/report")
  public ResponseEntity<?> getReport(
      @PathVariable String studentId, @RequestParam(required = false) Level level) {

    var report = reportService.generate(studentId, level);

    var requester = SecurityUtils.currentUser();

    var event =
        ReportEmailRequested.builder().report(report).requesterEmail(requester.getEmail()).build();

    reportEmailEventProducer.accept(List.of(event));

    return ResponseEntity.accepted().build();
  }
}
