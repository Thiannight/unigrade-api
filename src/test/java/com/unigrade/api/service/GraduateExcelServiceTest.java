package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.unigrade.api.model.Promotion;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.Specialization;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.model.JUser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraduateExcelServiceTest {

  private static final UUID PROMOTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final Promotion PROMOTION =
      new Promotion(PROMOTION_ID, "P-2024", (short) 2024, (short) 2025);

  @Mock private MembershipRepository membershipRepository;
  @Mock private ReportService reportService;

  private GraduateExcelService service;

  @BeforeEach
  void setUp() {
    service = new GraduateExcelService(membershipRepository, reportService);
  }

  @Test
  void generate_writesBlankCellWhenAverageMissing() throws Exception {
    JUser acker = student("S1", "Alice", "Acker");
    JUser zulu = student("S2", "Bob", "Zulu");
    StudentReport noAverage =
        new StudentReport(
            "S1",
            "Alice",
            "Acker",
            Specialization.TN,
            ReportStatus.COMPLETE,
            0,
            180,
            List.of(),
            null);
    StudentReport withAverage =
        new StudentReport(
            "S2",
            "Bob",
            "Zulu",
            Specialization.EL,
            ReportStatus.COMPLETE,
            60,
            180,
            List.of(),
            new BigDecimal("15.50"));

    when(membershipRepository.findDistinctStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(List.of(acker, zulu));
    when(reportService.generate(eq("S1"), isNull())).thenReturn(noAverage);
    when(reportService.generate(eq("S2"), isNull())).thenReturn(withAverage);

    byte[] bytes = service.generate(PROMOTION);

    assertTrue(bytes.length > 0);
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row ackerRow = sheet.getRow(3);
      Row zuluRow = sheet.getRow(4);
      assertNotNull(ackerRow);
      assertNotNull(zuluRow);
      assertEquals("Acker", ackerRow.getCell(2).getStringCellValue());
      assertEquals("TN", ackerRow.getCell(3).getStringCellValue());
      assertEquals(CellType.BLANK, ackerRow.getCell(4).getCellType());
      assertEquals("Zulu", zuluRow.getCell(2).getStringCellValue());
      assertEquals("EL", zuluRow.getCell(3).getStringCellValue());
      assertEquals(CellType.NUMERIC, zuluRow.getCell(4).getCellType());
      assertEquals(15.5, zuluRow.getCell(4).getNumericCellValue());
    }
  }

  @Test
  void writeBytes_whenWriteFails_throwsRuntimeException() throws Exception {
    Workbook workbook = spy(new XSSFWorkbook());
    doThrow(new IOException("boom")).when(workbook).write(any(OutputStream.class));

    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> service.writeBytes(PROMOTION, List.of(), workbook));

    assertTrue(ex.getMessage().contains("Failed to generate graduates workbook"));
  }

  private JUser student(String id, String firstName, String lastName) {
    return JUser.builder().id(id).firstName(firstName).lastName(lastName).build();
  }
}
