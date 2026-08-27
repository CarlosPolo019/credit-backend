package com.fya.credits.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of truth for the estimated monthly installment and total
 * payoff (French amortization, fixed monthly rate) — computed once here so
 * credit-web, credit-mobile and the PDF export all show the same numbers
 * instead of each recomputing it client-side.
 */
public final class CreditPaymentEstimator {
  private CreditPaymentEstimator() {
  }

  public record Estimate(BigDecimal monthlyPayment, BigDecimal totalToPay) {
  }

  public static Estimate estimate(BigDecimal amount, BigDecimal interestRatePercent, Integer termMonths) {
    BigDecimal principal = amount == null ? BigDecimal.ZERO : amount;
    int months = termMonths == null ? 0 : termMonths;
    BigDecimal monthlyRate = (interestRatePercent == null ? BigDecimal.ZERO : interestRatePercent)
        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

    if (principal.signum() <= 0 || months <= 0) {
      return new Estimate(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    BigDecimal monthlyPayment;
    if (monthlyRate.signum() == 0) {
      monthlyPayment = principal.divide(BigDecimal.valueOf(months), 10, RoundingMode.HALF_UP);
    } else {
      double p = principal.doubleValue();
      double r = monthlyRate.doubleValue();
      double n = months;
      double payment = (p * r) / (1 - Math.pow(1 + r, -n));
      monthlyPayment = BigDecimal.valueOf(payment);
    }

    BigDecimal total = monthlyPayment.multiply(BigDecimal.valueOf(months));
    return new Estimate(
        monthlyPayment.setScale(0, RoundingMode.HALF_UP),
        total.setScale(0, RoundingMode.HALF_UP));
  }
}
