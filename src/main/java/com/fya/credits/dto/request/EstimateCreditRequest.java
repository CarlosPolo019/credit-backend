package com.fya.credits.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record EstimateCreditRequest(
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
    @NotNull @DecimalMin(value = "0.0") BigDecimal interestRate,
    @NotNull @Positive Integer termMonths) {
}
