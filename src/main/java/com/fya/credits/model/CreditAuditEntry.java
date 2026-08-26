package com.fya.credits.model;

import java.time.Instant;
import java.util.Map;

public class CreditAuditEntry {
  private String id;
  private String creditId;
  private String action;
  private String changedByUserId;
  private String changedByDocument;
  private String changedByName;
  private Instant changedAt;
  private Map<String, FieldChange> changes;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getCreditId() { return creditId; }
  public void setCreditId(String creditId) { this.creditId = creditId; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getChangedByUserId() { return changedByUserId; }
  public void setChangedByUserId(String changedByUserId) { this.changedByUserId = changedByUserId; }
  public String getChangedByDocument() { return changedByDocument; }
  public void setChangedByDocument(String changedByDocument) { this.changedByDocument = changedByDocument; }
  public String getChangedByName() { return changedByName; }
  public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
  public Instant getChangedAt() { return changedAt; }
  public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
  public Map<String, FieldChange> getChanges() { return changes; }
  public void setChanges(Map<String, FieldChange> changes) { this.changes = changes; }

  public record FieldChange(String before, String after) {
  }
}
