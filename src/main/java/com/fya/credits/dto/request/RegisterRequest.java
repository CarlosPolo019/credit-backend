package com.fya.credits.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Pattern(regexp = "\\d+", message = "La cédula debe ser numérica") @Size(max = 20) String document,
    @NotBlank @Size(min = 8, max = 200) String password) {
}
