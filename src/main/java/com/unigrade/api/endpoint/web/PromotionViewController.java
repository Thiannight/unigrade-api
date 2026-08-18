package com.unigrade.api.endpoint.web;

import com.unigrade.api.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionViewController {

  private final PromotionService promotionService;

  @GetMapping("/view")
  public String listPromotions(Model model) {
    model.addAttribute("promotions", promotionService.findAll());
    return "promotions";
  }
}
