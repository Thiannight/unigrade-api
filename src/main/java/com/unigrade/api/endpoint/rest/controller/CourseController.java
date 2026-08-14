package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.model.Course;
import com.unigrade.api.service.CourseService;
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
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

  private final CourseService service;

  @GetMapping
  public List<Course> findAll() {
    return service.findAll();
  }

  @GetMapping("/{id}")
  public Course findById(@PathVariable UUID id) throws NotFoundException {
    return service.findById(id);
  }

  @PostMapping
  public ResponseEntity<Course> create(@Valid @RequestBody Course course) throws ConflictException {
    Course created = service.create(course);
    return ResponseEntity.created(URI.create("/courses/" + created.id())).body(created);
  }

  @PutMapping("/{id}")
  public Course update(@PathVariable UUID id, @Valid @RequestBody Course course)
      throws NotFoundException, ConflictException {
    return service.update(id, course);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) throws NotFoundException {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
