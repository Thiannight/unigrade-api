package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.User;
import com.unigrade.api.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService service;

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public List<User> findAll(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.findAll(page, size);
  }

  @PreAuthorize("hasAnyRole('ADMIN') or #id == authentication.principal.id")
  @GetMapping("/{id}")
  public User findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<User> create(@Valid @RequestBody User user) {
    User created = service.create(user);
    return ResponseEntity.created(URI.create("/users/" + created.id())).body(created);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public User update(@PathVariable String id, @Valid @RequestBody User user) {
    return service.update(id, user);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(@PathVariable String id) {
    service.deactivate(id);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}/hard")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
