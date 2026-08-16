package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.StudentReport;
import com.unigrade.api.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

  private final ReportService reportService;

  @GetMapping("/{studentId}/report")
  public StudentReport getReport(@PathVariable String studentId) {
    return reportService.generate(studentId);
  }
}
