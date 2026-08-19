package com.unigrade.api.service.event;

import static java.io.File.createTempFile;

import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.file.bucket.BucketComponent;
import com.unigrade.api.mail.Email;
import com.unigrade.api.mail.Mailer;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.PdfReportService;
import com.unigrade.api.service.ReportService;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ReportEmailRequestedService implements Consumer<ReportEmailRequested> {

  private static final Duration LINK_DURATION = Duration.ofHours(24);

  private final ReportService reportService;
  private final PdfReportService pdfReportService;
  private final BucketComponent bucketComponent;
  private final UserRepository userRepository;
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(ReportEmailRequested event) {
    StudentReport report = reportService.generateForSystem(event.getStudentId(), event.getLevel());
    JUser student = userRepository
        .findById(event.getStudentId())
        .orElseThrow(
            () -> new NotFoundException("Student not found: " + event.getStudentId()));

    byte[] pdfBytes = pdfReportService.generate(report);
    File tempFile = createTempFile("report-" + event.getStudentId(), ".pdf");
    try (var out = new FileOutputStream(tempFile)) {
      out.write(pdfBytes);
    }

    String bucketKey = "reports-" + event.getStudentId() + "-" + UUID.randomUUID() + ".pdf";
    bucketComponent.upload(tempFile, bucketKey);
    String downloadUrl = bucketComponent.presign(bucketKey, LINK_DURATION).toString();

    mailer.accept(buildEmail(student, downloadUrl));
  }

  private Email buildEmail(JUser student, String downloadUrl) throws Exception {
    var to = new InternetAddress(student.getEmail());
    String html = "<p>Hello "
        + student.getFirstName()
        + ",</p>"
        + "<p>Your transcript is ready. You can download it using the link below:</p>"
        + "<p><a href=\""
        + downloadUrl
        + "\">Download your transcript</a></p>"
        + "<p>This link expires in 24 hours.</p>";
    return new Email(to, List.of(), List.of(), "Your Unigrade transcript", html, List.of());
  }
}
