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
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) @Pattern(regexp = "(STD|MGR|TCR)\\d{5}")
        String id,
    @NotBlank @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @NotNull LocalDate birthDate,
    @NotBlank @Email @Size(max = 100) String email,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Size(min = 4, max = 255) @NotBlank
        String password,
    @NotNull Boolean isActive,
    @NotNull Role role,
    Specialization specialization) {}
