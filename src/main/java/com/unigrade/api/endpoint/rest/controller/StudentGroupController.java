package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.StudentGroup;
import com.unigrade.api.service.StudentGroupService;
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
@RequestMapping("/groups")
@RequiredArgsConstructor
public class StudentGroupController {

  private final StudentGroupService service;

  @GetMapping
  public List<StudentGroup> findAll(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return service.findAll(page, size);
  }

  @GetMapping("/{id}")
  public StudentGroup findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping
  public ResponseEntity<StudentGroup> create(@Valid @RequestBody StudentGroup group) {
    StudentGroup created = service.create(group);
    return ResponseEntity.created(URI.create("/groups/" + created.id())).body(created);
  }

  @PutMapping("/{id}")
  public StudentGroup update(@PathVariable UUID id, @Valid @RequestBody StudentGroup group) {
    return service.update(id, group);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
