package com.fya.credits.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCreditRequest(
    @NotBlank @Size(max = 60) String clientFirstName,
    @Size(max = 60) String clientSecondName,
    @NotBlank @Size(max = 60) String clientFirstSurname,
    @Size(max = 60) String clientSecondSurname,
    @NotBlank @Pattern(regexp = "\\d+", message = "La cédula o ID debe ser numérica") @Size(max = 20) String clientDocument,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
    @NotNull @DecimalMin(value = "0.0") BigDecimal interestRate,
    @NotNull @Positive Integer termMonths) {
}
