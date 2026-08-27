package com.fya.credits.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EstimateCreditRequest(
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "El valor debe ser mayor que cero")
    @DecimalMax(value = CreditLimits.MAX_AMOUNT, message = "El valor del crédito no puede superar $" + CreditLimits.MAX_AMOUNT)
    BigDecimal amount,
    @NotNull
    @DecimalMin(value = CreditLimits.MIN_INTEREST_RATE, message = "La tasa de interés mensual debe ser de al menos " + CreditLimits.MIN_INTEREST_RATE + "%")
    @DecimalMax(value = CreditLimits.MAX_INTEREST_RATE, message = "La tasa de interés mensual no puede superar " + CreditLimits.MAX_INTEREST_RATE + "%")
    BigDecimal interestRate,
    @NotNull
    @Min(value = CreditLimits.MIN_TERM_MONTHS, message = "El plazo debe ser de al menos " + CreditLimits.MIN_TERM_MONTHS + " mes")
    @Max(value = CreditLimits.MAX_TERM_MONTHS, message = "El plazo no puede superar " + CreditLimits.MAX_TERM_MONTHS + " meses")
    Integer termMonths) {
}
