package com.fya.credits.model;

import java.time.Instant;

public class Client {
  private String id;
  private String document;
  private String documentNormalized;
  private String firstName;
  private String secondName;
  private String firstSurname;
  private String secondSurname;
  private String fullName;
  private String fullNameNormalized;
  private Instant createdAt;
  private Instant updatedAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getDocument() { return document; }
  public void setDocument(String document) { this.document = document; }
  public String getDocumentNormalized() { return documentNormalized; }
  public void setDocumentNormalized(String documentNormalized) { this.documentNormalized = documentNormalized; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getSecondName() { return secondName; }
  public void setSecondName(String secondName) { this.secondName = secondName; }
  public String getFirstSurname() { return firstSurname; }
  public void setFirstSurname(String firstSurname) { this.firstSurname = firstSurname; }
  public String getSecondSurname() { return secondSurname; }
  public void setSecondSurname(String secondSurname) { this.secondSurname = secondSurname; }
  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }
  public String getFullNameNormalized() { return fullNameNormalized; }
  public void setFullNameNormalized(String fullNameNormalized) { this.fullNameNormalized = fullNameNormalized; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
