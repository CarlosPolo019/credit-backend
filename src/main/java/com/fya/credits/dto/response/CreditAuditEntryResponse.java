package com.fya.credits.dto.response;

import com.fya.credits.model.CreditAuditEntry;
import java.time.Instant;
import java.util.Map;

public record CreditAuditEntryResponse(
    String id,
    String creditId,
    String action,
    String changedByUserId,
    String changedByDocument,
    String changedByName,
    Instant changedAt,
    Map<String, CreditAuditEntry.FieldChange> changes) {
  public static CreditAuditEntryResponse from(CreditAuditEntry entry) {
    return new CreditAuditEntryResponse(
        entry.getId(),
        entry.getCreditId(),
        entry.getAction(),
        entry.getChangedByUserId(),
        entry.getChangedByDocument(),
        entry.getChangedByName(),
        entry.getChangedAt(),
        entry.getChanges());
  }
}
