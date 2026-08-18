package com.unigrade.api.service;

import com.unigrade.api.model.Promotion;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.model.JUser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraduateExcelService {

  private static final String[] HEADERS = {
    "Matricule", "Prénom", "Nom", "Spécialisation", "Moyenne générale", "Crédits obtenus"
  };

  private final MembershipRepository membershipRepository;
  private final ReportService reportService;

  public byte[] generate(Promotion promotion) {
    List<JUser> students = membershipRepository.findDistinctStudentsByPromotionId(promotion.id());

    List<StudentReport> graduates =
        students.stream()
            .map(student -> reportService.generate(student.getId(), null))
            .filter(report -> report.status() == ReportStatus.COMPLETE)
            .sorted(
                Comparator.comparing(StudentReport::lastName)
                    .thenComparing(StudentReport::firstName))
            .toList();

    return writeBytes(promotion, graduates, new XSSFWorkbook());
  }

  byte[] writeBytes(Promotion promotion, List<StudentReport> graduates, Workbook workbook) {
    try (Workbook wb = workbook) {
      Sheet sheet = wb.createSheet("Diplomes");

      Font headerFont = wb.createFont();
      headerFont.setBold(true);
      var headerStyle = workbook.createCellStyle();
      headerStyle.setFont(headerFont);

      Row title = sheet.createRow(0);
      title
          .createCell(0)
          .setCellValue(
              "Diplômés - "
                  + promotion.reference()
                  + " ("
                  + promotion.startYear()
                  + "-"
                  + promotion.endYear()
                  + ")");

      Row headerRow = sheet.createRow(2);
      for (int i = 0; i < HEADERS.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(HEADERS[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIndex = 3;
      for (StudentReport report : graduates) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(report.studentId());
        row.createCell(1).setCellValue(report.firstName());
        row.createCell(2).setCellValue(report.lastName());

        Cell specializationCell = row.createCell(3);
        if (report.specialization() != null) {
          specializationCell.setCellValue(report.specialization().name());
        } else {
          specializationCell.setCellType(CellType.BLANK);
        }

        Cell averageCell = row.createCell(4);
        BigDecimal average = report.overallAverage();
        if (average != null) {
          averageCell.setCellValue(average.doubleValue());
        } else {
          averageCell.setCellType(CellType.BLANK);
        }

        row.createCell(5).setCellValue(report.totalCredits());
      }

      for (int i = 0; i < HEADERS.length; i++) {
        sheet.autoSizeColumn(i);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to generate graduates workbook", e);
    }
  }
}
