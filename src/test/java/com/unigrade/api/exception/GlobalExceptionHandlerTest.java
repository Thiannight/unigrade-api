package com.unigrade.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void validationErrors_mapsToBadRequest() {
    var bindingResult = new BeanPropertyBindingResult(new Object(), "promotion");
    bindingResult.addError(new FieldError("promotion", "reference", "must not be blank"));
    bindingResult.addError(new FieldError("promotion", "startYear", "must be positive"));

    ProblemDetail detail =
        handler.handleValidation(new MethodArgumentNotValidException(null, bindingResult));

    assertEquals(400, detail.getStatus());
    assertEquals("Validation failed", detail.getDetail());
    assertEquals(
        List.of("reference: must not be blank", "startYear: must be positive"),
        detail.getProperties().get("errors"));
  }

  @Test
  void dataIntegrityViolation_mapsToConflict() {
    ProblemDetail detail = handler.handleConflict(new DataIntegrityViolationException("dup"));

    assertEquals(409, detail.getStatus());
  }

  @Test
  void typeMismatch_mapsToBadRequest() {
    var e = new MethodArgumentTypeMismatchException("pas-un-uuid", UUID.class, "id", null, null);

    ProblemDetail detail = handler.handleTypeMismatch(e);

    assertEquals(400, detail.getStatus());
    assertEquals("Invalid value for parameter 'id'", detail.getDetail());
  }

  @Test
  void unreadableBody_mapsToBadRequest() {
    ProblemDetail detail =
        handler.handleUnreadable(new HttpMessageNotReadableException("bad json", null, null));

    assertEquals(400, detail.getStatus());
    assertEquals("Malformed request body", detail.getDetail());
  }

  @Test
  void unreadableBody_withTargetType_mentionsType() {
    String msg =
        "JSON parse error: Cannot deserialize value of type "
            + "`com.unigrade.api.model.dto.GroupAssignRequest` from Object value";

    ProblemDetail detail =
        handler.handleUnreadable(new HttpMessageNotReadableException(msg, null, null));

    assertEquals(400, detail.getStatus());
    assertEquals("Malformed request body. Expected a valid GroupAssignRequest object", detail.getDetail());
  }

  @Test
  void notFound_mapsTo404() {
    ProblemDetail detail = handler.handleNotFound(new NotFoundException("Promotion not found: x"));

    assertEquals(404, detail.getStatus());
    assertEquals("Promotion not found: x", detail.getDetail());
  }

  @Test
  void conflict_mapsTo409() {
    ProblemDetail detail = handler.handleConflict(new ConflictException("already exists"));

    assertEquals(409, detail.getStatus());
    assertEquals("already exists", detail.getDetail());
  }

  @Test
  void badRequest_mapsTo400() {
    ProblemDetail detail = handler.handleBadRequest(new BadRequestException("bad"));

    assertEquals(400, detail.getStatus());
  }

  @Test
  void forbidden_mapsTo403() {
    ProblemDetail detail = handler.handleForbidden(new ForbiddenException("no"));

    assertEquals(403, detail.getStatus());
  }

  @Test
  void responseStatus_withReason_mapsToStatus() {
    ProblemDetail detail =
        handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "gone"));

    assertEquals(404, detail.getStatus());
    assertEquals("gone", detail.getDetail());
  }

  @Test
  void responseStatus_withoutReason_usesStatus() {
    ProblemDetail detail =
        handler.handleResponseStatus(new ResponseStatusException(HttpStatus.CONFLICT));

    assertEquals(409, detail.getStatus());
  }

  @Test
  void accessDenied_mapsTo403() {
    ProblemDetail detail = handler.handleAccessDenied(new AccessDeniedException("denied"));

    assertEquals(403, detail.getStatus());
    assertEquals("Access denied", detail.getDetail());
  }

  @Test
  void authenticationFailure_mapsTo401() {
    ProblemDetail detail =
        handler.handleAuthentication(new BadCredentialsException("bad credentials"));

    assertEquals(401, detail.getStatus());
    assertEquals("Authentication failed", detail.getDetail());
  }

  @Test
  void unexpected_mapsTo500() {
    ProblemDetail detail = handler.handleUnexpected(new RuntimeException("boom"));

    assertEquals(500, detail.getStatus());
  }
}
