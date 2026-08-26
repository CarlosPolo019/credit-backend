package com.fya.credits.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCreditRequest(
    @NotBlank @Size(max = 120) String clientName,
    @NotBlank @Size(max = 40) String clientDocument,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
    @NotNull @DecimalMin(value = "0.0") BigDecimal interestRate,
    @NotNull @Positive Integer termMonths,
    @NotBlank @Size(max = 120) String salespersonName) {
}
