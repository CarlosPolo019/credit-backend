package com.fya.credits.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Credit {
  private String id;
  private String clientName;
  private String clientNameNormalized;
  private String clientDocument;
  private String clientDocumentNormalized;
  private BigDecimal amount;
  private BigDecimal interestRate;
  private Integer termMonths;
  private String salespersonName;
  private String salespersonNameNormalized;
  private Boolean isActive;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getClientName() { return clientName; }
  public void setClientName(String clientName) { this.clientName = clientName; }
  public String getClientNameNormalized() { return clientNameNormalized; }
  public void setClientNameNormalized(String clientNameNormalized) { this.clientNameNormalized = clientNameNormalized; }
  public String getClientDocument() { return clientDocument; }
  public void setClientDocument(String clientDocument) { this.clientDocument = clientDocument; }
  public String getClientDocumentNormalized() { return clientDocumentNormalized; }
  public void setClientDocumentNormalized(String clientDocumentNormalized) { this.clientDocumentNormalized = clientDocumentNormalized; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public BigDecimal getInterestRate() { return interestRate; }
  public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
  public Integer getTermMonths() { return termMonths; }
  public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
  public String getSalespersonName() { return salespersonName; }
  public void setSalespersonName(String salespersonName) { this.salespersonName = salespersonName; }
  public String getSalespersonNameNormalized() { return salespersonNameNormalized; }
  public void setSalespersonNameNormalized(String salespersonNameNormalized) { this.salespersonNameNormalized = salespersonNameNormalized; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public Instant getDeletedAt() { return deletedAt; }
  public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
