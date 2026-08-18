package com.unigrade.api.service;

import com.unigrade.api.model.GraduationListEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class GraduationXlsxService {

  public byte[] generate(List<GraduationListEntry> entries) {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Graduates");

      CellStyle headerStyle = workbook.createCellStyle();
      Font boldFont = workbook.createFont();
      boldFont.setBold(true);
      headerStyle.setFont(boldFont);

      Row header = sheet.createRow(0);
      String[] headers = {"Rank", "Student ID", "First Name", "Last Name", "All-Time Average"};
      for (int i = 0; i < headers.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIndex = 1;
      for (GraduationListEntry g : entries) {
        writeRow(sheet, rowIndex++, g);
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to generate XLSX", e);
    }
  }

  private void writeRow(Sheet sheet, int rowIndex, GraduationListEntry g) {
    Row row = sheet.createRow(rowIndex);
    row.createCell(0).setCellValue(g.rank());
    row.createCell(1).setCellValue(g.studentId());
    row.createCell(2).setCellValue(g.firstName());
    row.createCell(3).setCellValue(g.lastName());
    if (g.allTimeAverage() != null) {
      row.createCell(4).setCellValue(g.allTimeAverage().doubleValue());
    }
  }
}
