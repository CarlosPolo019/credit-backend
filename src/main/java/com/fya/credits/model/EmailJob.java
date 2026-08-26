package com.fya.credits.model;

import java.math.BigDecimal;
import java.time.Instant;

public class EmailJob {
  private String id;
  private String creditId;
  private String recipient;
  private String clientName;
  private BigDecimal creditAmount;
  private String salespersonName;
  private Instant registeredAt;
  private EmailJobStatus status;
  private Integer attempts;
  private String lastError;
  private Instant createdAt;
  private Instant processedAt;
  private Instant nextAttemptAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getCreditId() { return creditId; }
  public void setCreditId(String creditId) { this.creditId = creditId; }
  public String getRecipient() { return recipient; }
  public void setRecipient(String recipient) { this.recipient = recipient; }
  public String getClientName() { return clientName; }
  public void setClientName(String clientName) { this.clientName = clientName; }
  public BigDecimal getCreditAmount() { return creditAmount; }
  public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
  public String getSalespersonName() { return salespersonName; }
  public void setSalespersonName(String salespersonName) { this.salespersonName = salespersonName; }
  public Instant getRegisteredAt() { return registeredAt; }
  public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
  public EmailJobStatus getStatus() { return status; }
  public void setStatus(EmailJobStatus status) { this.status = status; }
  public Integer getAttempts() { return attempts; }
  public void setAttempts(Integer attempts) { this.attempts = attempts; }
  public String getLastError() { return lastError; }
  public void setLastError(String lastError) { this.lastError = lastError; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getProcessedAt() { return processedAt; }
  public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
  public Instant getNextAttemptAt() { return nextAttemptAt; }
  public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
}
