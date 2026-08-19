package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.endpoint.event.EventProducer;
import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.ReportService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

  @Mock private ReportService reportService;

  @Mock private EventProducer<ReportEmailRequested> reportEmailEventProducer;

  private StudentController controller;

  @BeforeEach
  void setUp() {
    controller = new StudentController(reportService, reportEmailEventProducer);
    loginAs("ADMIN001", "admin@unigrade.com", Role.ADMIN);
  }

  @Test
  void getReport_producesEventAndReturnsAccepted() {
    StudentReport report =
        new StudentReport(
            "STD00001", "Ada", "Lovelace", ReportStatus.COMPLETE, 180, 180, List.of(), null);
    when(reportService.generate("STD00001", Level.L1)).thenReturn(report);

    ResponseEntity<?> response = controller.getReport("STD00001", Level.L1);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ReportEmailRequested>> captor = ArgumentCaptor.forClass(List.class);

    verify(reportEmailEventProducer).accept(captor.capture());

    ReportEmailRequested event = captor.getValue().get(0);

    assertEquals("STD00001", event.getReport().studentId());
    assertEquals("admin@unigrade.com", event.getRequesterEmail());
  }

  @Test
  void getReport_withoutLevel_producesEvent() {
    StudentReport report =
        new StudentReport(
            "STD00002", "Grace", "Hopper", ReportStatus.COMPLETE, 180, 180, List.of(), null);
    when(reportService.generate("STD00002", null)).thenReturn(report);

    ResponseEntity<?> response = controller.getReport("STD00002", null);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ReportEmailRequested>> captor = ArgumentCaptor.forClass(List.class);

    verify(reportEmailEventProducer).accept(captor.capture());

    ReportEmailRequested event = captor.getValue().get(0);

    assertEquals("STD00002", event.getReport().studentId());
    assertEquals(null, event.getReport().overallAverage());
  }

  @Test
  void getReport_performsAuthorizationCheckBeforePublishing() {
    StudentReport report =
        new StudentReport(
            "STD00003", "Linus", "Torvalds", ReportStatus.COMPLETE, 180, 180, List.of(), null);
    when(reportService.generate("STD00003", null)).thenReturn(report);

    controller.getReport("STD00003", null);

    verify(reportService).generate("STD00003", null);
  }

  private void loginAs(String id, String email, Role role) {
    var principal = new JUser();
    principal.setId(id);
    principal.setEmail(email);
    principal.setRole(role);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
