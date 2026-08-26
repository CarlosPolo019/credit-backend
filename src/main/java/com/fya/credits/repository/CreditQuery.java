package com.fya.credits.repository;

public record CreditQuery(
    String clientName,
    String clientDocument,
    String salesperson,
    String sortBy,
    String direction) {
}
