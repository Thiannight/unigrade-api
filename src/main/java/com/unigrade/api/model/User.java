package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record User(
    @Pattern(regexp = "(STD|MGR|TCR)\\d{5}") String id,
    @NotBlank @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @NotNull LocalDate birthDate,
    @NotNull @Email @Size(max = 100) String email,
    @JsonIgnore @Size(max = 255) String password,
    @NotNull Boolean isActive,
    @NotNull Role role) {}
