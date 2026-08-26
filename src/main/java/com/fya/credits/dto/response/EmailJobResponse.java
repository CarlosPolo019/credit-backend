package com.fya.credits.dto.response;

import com.fya.credits.model.EmailJob;
import java.math.BigDecimal;
import java.time.Instant;

public record EmailJobResponse(
    String id,
    String creditId,
    String recipient,
    String clientName,
    BigDecimal creditAmount,
    String salespersonName,
    Instant registeredAt,
    String status,
    Integer attempts,
    String lastError,
    Instant createdAt,
    Instant processedAt,
    Instant nextAttemptAt) {
  public static EmailJobResponse from(EmailJob job) {
    return new EmailJobResponse(
        job.getId(),
        job.getCreditId(),
        job.getRecipient(),
        job.getClientName(),
        job.getCreditAmount(),
        job.getSalespersonName(),
        job.getRegisteredAt(),
        job.getStatus().name(),
        job.getAttempts(),
        job.getLastError(),
        job.getCreatedAt(),
        job.getProcessedAt(),
        job.getNextAttemptAt());
  }
}
