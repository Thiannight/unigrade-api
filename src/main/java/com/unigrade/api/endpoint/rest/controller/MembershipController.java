package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.Membership;
import com.unigrade.api.model.dto.GroupAssignRequest;
import com.unigrade.api.model.dto.GroupTransferRequest;
import com.unigrade.api.service.MembershipService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/members")
@RequiredArgsConstructor
public class MembershipController {

  private final MembershipService service;

  @GetMapping
  public List<Membership> getMembersAt(
      @PathVariable UUID groupId,
      @RequestParam(required = false) LocalDate at,
      @RequestParam(defaultValue = "false") boolean includeInactive,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.getMembersAt(
        groupId, at != null ? at : LocalDate.now(), includeInactive, page, size);
  }

  @PostMapping
  public ResponseEntity<Membership> assign(
      @PathVariable UUID groupId, @Valid @RequestBody GroupAssignRequest request) {
    Membership created = service.assign(groupId, request);
    return ResponseEntity.created(
            URI.create("/groups/" + groupId + "/members/" + created.studentId()))
        .body(created);
  }

  @PutMapping("/{studentId}")
  public ResponseEntity<Void> transfer(
      @PathVariable UUID groupId,
      @PathVariable String studentId,
      @Valid @RequestBody GroupTransferRequest request) {
    service.transfer(groupId, studentId, request);
    return ResponseEntity.noContent().build();
  }
}
