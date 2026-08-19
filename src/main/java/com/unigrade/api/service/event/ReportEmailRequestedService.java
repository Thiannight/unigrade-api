package com.unigrade.api.service.event;

import static java.io.File.createTempFile;

import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.file.bucket.BucketComponent;
import com.unigrade.api.mail.Email;
import com.unigrade.api.mail.Mailer;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.service.PdfReportService;
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

  private final PdfReportService pdfReportService;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(ReportEmailRequested event) {
    StudentReport report = event.getReport();

    byte[] pdfBytes = pdfReportService.generate(report);
    File tempFile = createTempFile("report-" + report.studentId(), ".pdf");
    try (var out = new FileOutputStream(tempFile)) {
      out.write(pdfBytes);
    }

    String bucketKey = "reports-" + report.studentId() + "-" + UUID.randomUUID() + ".pdf";
    bucketComponent.upload(tempFile, bucketKey);
    String downloadUrl = bucketComponent.presign(bucketKey, LINK_DURATION).toString();

    mailer.accept(buildEmail(report, event.getRequesterEmail(), downloadUrl));
  }

  private Email buildEmail(StudentReport report, String requesterEmail, String downloadUrl)
      throws Exception {
    var to = new InternetAddress(requesterEmail);
    String fullName = report.firstName() + " " + report.lastName();
    String html = buildHtmlBody(fullName, report.studentId(), downloadUrl);
    return new Email(to, List.of(), List.of(), "Requested Report Ready", html, List.of());
  }

  private String buildHtmlBody(String studentFullName, String studentId, String downloadUrl) {
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
                <span style="color:#ffffff; font-size:20px; font-weight:bold;">HEI Admin--</span>
              </td>
            </tr>
            <tr>
              <td style="padding:32px;">
                <p style="margin:0 0 16px; font-size:16px; color:#111827;"> Hello,</p>
                <p style="margin:0 0 24px; font-size:15px; line-height:1.5; color:#374151;">
                  The report for %s (%s) you requested is ready. Click the button below to download the PDF report.
                </p>
                <table role="presentation" cellpadding="0" cellspacing="0">
                  <tr>
                    <td style="border-radius:6px; background-color:#2563eb;">
                      <a href="%s" target="_blank" style="display:inline-block; padding:12px 24px; font-size:15px; font-weight:bold; color:#ffffff; text-decoration:none; border-radius:6px;">
                        Download report
                      </a>
                    </td>
                  </tr>
                </table>
                <p style="margin:24px 0 0; font-size:13px; color:#6b7280;">
                  This link expires in %d hours.
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
        .formatted(escapeHtml(studentFullName), studentId, downloadUrl, expiryHours);
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
