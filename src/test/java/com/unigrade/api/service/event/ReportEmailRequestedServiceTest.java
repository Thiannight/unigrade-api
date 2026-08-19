package com.unigrade.api.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.file.bucket.BucketComponent;
import com.unigrade.api.mail.Email;
import com.unigrade.api.mail.Mailer;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.PdfReportService;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportEmailRequestedServiceTest {

  @Mock private PdfReportService pdfReportService;
  @Mock private BucketComponent bucketComponent;
  @Mock private UserRepository userRepository;
  @Mock private Mailer mailer;

  private ReportEmailRequestedService service;

  @BeforeEach
  void setUp() {
    service =
        new ReportEmailRequestedService(pdfReportService, bucketComponent, userRepository, mailer);
  }

  @Test
  void accept_uploadsPdfAndSendsPresignedLinkEmail() throws Exception {
    var report =
        new StudentReport(
            "STD00001", "Ada", "Lovelace", ReportStatus.COMPLETE, 180, 180, List.of(), null);
    var event = ReportEmailRequested.builder().report(report).build();
    var student = new JUser();
    student.setId("STD00001");
    student.setFirstName("Ada");
    student.setEmail("ada@unigrade.com");

    when(userRepository.findById("STD00001")).thenReturn(Optional.of(student));
    when(pdfReportService.generate(report)).thenReturn("pdf-bytes".getBytes());
    when(bucketComponent.presign(any(), any(Duration.class)))
        .thenReturn(URI.create("https://bucket.s3.amazonaws.com/link").toURL());

    service.accept(event);

    verify(bucketComponent).upload(any(File.class), any(String.class));
    ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(captor.capture());
    assertEquals("ada@unigrade.com", captor.getValue().to().getAddress());
    assertTrue(captor.getValue().htmlBody().contains("https://bucket.s3.amazonaws.com/link"));
  }
}
