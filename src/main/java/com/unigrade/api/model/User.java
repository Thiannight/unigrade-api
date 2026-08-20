package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record User(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Pattern(
            regexp = "(STD|MGR|TCR)\\d{5}",
            message = "id must be STD, MGR or TCR followed by 5 digits")
        String id,
    @NotBlank(message = "firstName is required")
        @Size(max = 100, message = "firstName must be at most 100 characters")
        String firstName,
    @Size(max = 100, message = "lastName must be at most 100 characters") String lastName,
    @NotNull(message = "birthDate is required") LocalDate birthDate,
    @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 100, message = "email must be at most 100 characters")
        String email,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Size(min = 4, max = 255, message = "password must be between 4 and 255 characters")
        @NotBlank(message = "password is required")
        String password,
    @NotNull(message = "isActive is required") Boolean isActive,
    @NotNull(message = "role is required") Role role,
    Specialization specialization) {}
