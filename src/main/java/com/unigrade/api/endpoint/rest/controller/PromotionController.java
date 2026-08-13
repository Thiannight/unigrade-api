package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.Promotion;
import com.unigrade.api.service.PromotionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
@AllArgsConstructor
public class PromotionController {

  private final PromotionService promotionService;

  @GetMapping
  public List<Promotion> findAll() {
    return promotionService.findAll();
  }

  @GetMapping("/{id}")
  public Promotion findById(@PathVariable UUID id) {
    return promotionService.findById(id);
  }

  @PostMapping
  public ResponseEntity<Promotion> create(@Valid @RequestBody Promotion promotion) {
    Promotion created = promotionService.create(promotion);
    return ResponseEntity.created(URI.create("/promotions/" + created.id())).body(created);
  }

  @PutMapping("/{id}")
  public Promotion update(@PathVariable UUID id, @Valid @RequestBody Promotion promotion) {
    return promotionService.update(id, promotion);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    promotionService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
