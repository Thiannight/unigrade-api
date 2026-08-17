package com.unigrade.api.endpoint.web;

import com.unigrade.api.model.Promotion;
import com.unigrade.api.service.GraduateExcelService;
import com.unigrade.api.service.PromotionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionViewController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final PromotionService promotionService;
  private final GraduateExcelService graduateExcelService;

  @GetMapping("/view")
  public String listPromotions(Model model) {
    model.addAttribute("promotions", promotionService.findAll());
    return "promotions";
  }

  @GetMapping("/{id}/graduates.xlsx")
  public ResponseEntity<byte[]> downloadGraduates(@PathVariable UUID id) {
    Promotion promotion = promotionService.findById(id);
    byte[] excel = graduateExcelService.generate(promotion);

    String filename = "diplomes-" + promotion.reference() + ".xlsx";
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(excel);
  }
}
