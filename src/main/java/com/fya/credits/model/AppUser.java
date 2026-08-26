package com.fya.credits.model;

import java.time.Instant;

public class AppUser {
  private String id;
  private String fullName;
  private String document;
  private String documentNormalized;
  private String passwordHash;
  private String role;
  private Boolean isActive;
  private Instant createdAt;
  private Instant updatedAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }
  public String getDocument() { return document; }
  public void setDocument(String document) { this.document = document; }
  public String getDocumentNormalized() { return documentNormalized; }
  public void setDocumentNormalized(String documentNormalized) { this.documentNormalized = documentNormalized; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
