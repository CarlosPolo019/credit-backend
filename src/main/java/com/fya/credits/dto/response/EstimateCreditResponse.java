package com.fya.credits.dto.response;

import java.math.BigDecimal;

public record EstimateCreditResponse(BigDecimal monthlyPayment, BigDecimal totalToPay) {
}
