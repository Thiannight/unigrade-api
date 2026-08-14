package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.Promotion;
import com.unigrade.api.service.PromotionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PromotionController {

  private final PromotionService service;

  @GetMapping
  public List<Promotion> findAll() {
    return service.findAll();
  }

  @GetMapping("/{id}")
  public Promotion findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping
  public ResponseEntity<Promotion> create(@Valid @RequestBody Promotion promotion) {
    Promotion created = service.create(promotion);
    return ResponseEntity.created(URI.create("/promotions/" + created.id())).body(created);
  }

  @PutMapping("/{id}")
  public Promotion update(@PathVariable UUID id, @Valid @RequestBody Promotion promotion) {
    return service.update(id, promotion);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
