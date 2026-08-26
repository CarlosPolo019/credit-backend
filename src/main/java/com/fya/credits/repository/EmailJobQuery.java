package com.fya.credits.repository;

public record EmailJobQuery(
    String status,
    String search,
    String sortBy,
    String direction) {
}
