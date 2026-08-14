package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.Exam;
import com.unigrade.api.service.ExamService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

  private final ExamService service;

  @GetMapping
  public List<Exam> findAll(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.findAll(page, size);
  }

  @GetMapping("/{id}")
  public Exam findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping
  public ResponseEntity<Exam> create(@Valid @RequestBody Exam exam) {
    Exam created = service.create(exam);
    return ResponseEntity.created(URI.create("/exams/" + created.id())).body(created);
  }

  @PutMapping("/{id}")
  public Exam update(@PathVariable UUID id, @Valid @RequestBody Exam exam) {
    return service.update(id, exam);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
