package com.fya.credits.dto.response;

import com.fya.credits.model.Credit;
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
    String salespersonName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt) {
  public static CreditResponse from(Credit credit) {
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
        credit.getSalespersonName(),
        credit.getIsActive(),
        credit.getCreatedAt(),
        credit.getUpdatedAt(),
        credit.getDeletedAt());
  }
}
