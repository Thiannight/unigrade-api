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
    JUser student =
        userRepository
            .findById(event.getStudentId())
            .orElseThrow(() -> new NotFoundException("Student not found: " + event.getStudentId()));

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
    String html = buildHtmlBody(student.getFirstName(), downloadUrl);
    return new Email(to, List.of(), List.of(), "Your Unigrade transcript", html, List.of());
  }

  private String buildHtmlBody(String firstName, String downloadUrl) {
    long expiryHours = LINK_DURATION.toHours();
    return """
<!DOCTYPE html>
<html lang="en">
  <body style="margin:0; padding:0; background-color:#f4f5f7; font-family:Helvetica, Arial, sans-serif;">
    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7; padding:32px 0;">
      <tr>
        <td align="center">
          <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden;">
            <tr>
              <td style="background-color:#1f2937; padding:24px 32px;">
                <span style="color:#ffffff; font-size:20px; font-weight:bold;">Unigrade</span>
              </td>
            </tr>
            <tr>
              <td style="padding:32px;">
                <p style="margin:0 0 16px; font-size:16px; color:#111827;"> Hello %s,</p>
                <p style="margin:0 0 24px; font-size:15px; line-height:1.5; color:#374151;">
                  Your academic transcript is ready. Click the button below to download your PDF report.
                </p>
                <table role="presentation" cellpadding="0" cellspacing="0">
                  <tr>
                    <td style="border-radius:6px; background-color:#2563eb;">
                      <a href="%s" target="_blank" style="display:inline-block; padding:12px 24px; font-size:15px; font-weight:bold; color:#ffffff; text-decoration:none; border-radius:6px;">
                        Download your transcript
                      </a>
                    </td>
                  </tr>
                </table>
                <p style="margin:24px 0 0; font-size:13px; color:#6b7280;">
                  This link expires in %d hours.
                </p>
              </td>
            </tr>
            <tr>
              <td style="padding:16px 32px; background-color:#f9fafb; border-top:1px solid #e5e7eb;">
                <p style="margin:0; font-size:12px; color:#9ca3af;">
                  This is an automated message from Unigrade. Please do not reply to this email.
                </p>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>
"""
        .formatted(escapeHtml(firstName), downloadUrl, expiryHours);
  }

  private String escapeHtml(String value) {
    return value == null
        ? ""
        : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
  }
}
