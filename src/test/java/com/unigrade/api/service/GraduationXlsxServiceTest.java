package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unigrade.api.model.GraduationListEntry;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class GraduationXlsxServiceTest {

  private final GraduationXlsxService service = new GraduationXlsxService();

  @Test
  void generate_withGraduates_producesValidXlsx() {
    var entries =
        List.of(
            new GraduationListEntry(1, "STD001", "Alice", "Dupont", new BigDecimal("15.50")),
            new GraduationListEntry(2, "STD002", "Bob", "Martin", new BigDecimal("14.25")));

    byte[] xlsx = service.generate(entries);

    assertNotNull(xlsx);
    assertTrue(xlsx.length > 0);
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
      var sheet = workbook.getSheetAt(0);
      assertNotNull(sheet);
      assertTrue(sheet.getLastRowNum() >= 2);
      assertTrue(sheet.getRow(0).getCell(0).getStringCellValue().equals("Rank"));
      assertTrue(sheet.getRow(1).getCell(0).getNumericCellValue() == 1);
      assertTrue(sheet.getRow(1).getCell(1).getStringCellValue().equals("STD001"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void generate_emptyList_producesValidXlsx() {
    byte[] xlsx = service.generate(List.of());

    assertNotNull(xlsx);
    assertTrue(xlsx.length > 0);
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
      var sheet = workbook.getSheetAt(0);
      assertNotNull(sheet);
      assertTrue(sheet.getRow(0).getCell(0).getStringCellValue().equals("Rank"));
      assertTrue(sheet.getLastRowNum() == 0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
