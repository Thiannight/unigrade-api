package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.endpoint.event.EventProducer;
import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.Membership;
import com.unigrade.api.model.dto.GroupTransferRequest;
import com.unigrade.api.security.SecurityUtils;
import com.unigrade.api.service.MembershipService;
import com.unigrade.api.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

  private final ReportService reportService;
  private final EventProducer<ReportEmailRequested> reportEmailEventProducer;
  private final MembershipService membershipService;

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

  @PutMapping("/{studentId}/transfer")
  public Membership transfer(
      @PathVariable String studentId, @Valid @RequestBody GroupTransferRequest request) {
    return membershipService.transfer(studentId, request);
  }

  @GetMapping("/{studentId}/memberships")
  public List<Membership> getMemberships(@PathVariable String studentId) {
    return membershipService.getStudentMemberships(studentId);
  }
}
