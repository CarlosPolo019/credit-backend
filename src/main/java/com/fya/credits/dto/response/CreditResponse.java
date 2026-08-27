package com.fya.credits.dto.response;

import com.fya.credits.model.Credit;
import com.fya.credits.service.CreditPaymentEstimator;
import java.math.BigDecimal;
import java.time.Instant;

public record CreditResponse(
    String id,
    String clientFirstName,
    String clientSecondName,
    String clientFirstSurname,
    String clientSecondSurname,
    String clientName,
    String clientDocument,
    BigDecimal amount,
    BigDecimal interestRate,
    Integer termMonths,
    String registeredByUserId,
    String salespersonDocument,
    String salespersonName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt,
    BigDecimal estimatedMonthlyPayment,
    BigDecimal estimatedTotalToPay) {
  public static CreditResponse from(Credit credit) {
    CreditPaymentEstimator.Estimate estimate =
        CreditPaymentEstimator.estimate(credit.getAmount(), credit.getInterestRate(), credit.getTermMonths());
    return new CreditResponse(
        credit.getId(),
        credit.getClientFirstName(),
        credit.getClientSecondName(),
        credit.getClientFirstSurname(),
        credit.getClientSecondSurname(),
        credit.getClientName(),
        credit.getClientDocument(),
        credit.getAmount(),
        credit.getInterestRate(),
        credit.getTermMonths(),
        credit.getRegisteredByUserId(),
        credit.getSalespersonDocument(),
        credit.getSalespersonName(),
        credit.getIsActive(),
        credit.getCreatedAt(),
        credit.getUpdatedAt(),
        credit.getDeletedAt(),
        estimate.monthlyPayment(),
        estimate.totalToPay());
  }
}
