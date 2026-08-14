package com.unigrade.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    assertThat(detail.getStatus()).isEqualTo(400);
    assertThat(detail.getDetail()).isEqualTo("Validation failed");
    assertThat(detail.getProperties().get("errors"))
        .isEqualTo(List.of("reference: must not be blank", "startYear: must be positive"));
  }

  @Test
  void dataIntegrityViolation_mapsToConflict() {
    ProblemDetail detail = handler.handleConflict(new DataIntegrityViolationException("dup"));

    assertThat(detail.getStatus()).isEqualTo(409);
  }

  @Test
  void typeMismatch_mapsToBadRequest() {
    var e = new MethodArgumentTypeMismatchException("pas-un-uuid", UUID.class, "id", null, null);

    ProblemDetail detail = handler.handleTypeMismatch(e);

    assertThat(detail.getStatus()).isEqualTo(400);
    assertThat(detail.getDetail()).isEqualTo("Invalid value for parameter 'id'");
  }

  @Test
  void unreadableBody_mapsToBadRequest() {
    ProblemDetail detail =
        handler.handleUnreadable(new HttpMessageNotReadableException("bad json", null, null));

    assertThat(detail.getStatus()).isEqualTo(400);
    assertThat(detail.getDetail()).isEqualTo("Malformed request body");
  }

  @Test
  void notFound_mapsTo404() {
    ProblemDetail detail = handler.handleNotFound(new NotFoundException("Promotion not found: x"));

    assertThat(detail.getStatus()).isEqualTo(404);
    assertThat(detail.getDetail()).isEqualTo("Promotion not found: x");
  }

  @Test
  void conflict_mapsTo409() {
    ProblemDetail detail = handler.handleConflict(new ConflictException("already exists"));

    assertThat(detail.getStatus()).isEqualTo(409);
    assertThat(detail.getDetail()).isEqualTo("already exists");
  }

  @Test
  void badRequest_mapsTo400() {
    ProblemDetail detail = handler.handleBadRequest(new BadRequestException("bad"));

    assertThat(detail.getStatus()).isEqualTo(400);
  }

  @Test
  void forbidden_mapsTo403() {
    ProblemDetail detail = handler.handleForbidden(new ForbiddenException("no"));

    assertThat(detail.getStatus()).isEqualTo(403);
  }

  @Test
  void responseStatus_withReason_mapsToStatus() {
    ProblemDetail detail =
        handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "gone"));

    assertThat(detail.getStatus()).isEqualTo(404);
    assertThat(detail.getDetail()).isEqualTo("gone");
  }

  @Test
  void responseStatus_withoutReason_usesStatus() {
    ProblemDetail detail =
        handler.handleResponseStatus(new ResponseStatusException(HttpStatus.CONFLICT));

    assertThat(detail.getStatus()).isEqualTo(409);
  }

  @Test
  void unexpected_mapsTo500() {
    ProblemDetail detail = handler.handleUnexpected(new RuntimeException("boom"));

    assertThat(detail.getStatus()).isEqualTo(500);
  }
}
