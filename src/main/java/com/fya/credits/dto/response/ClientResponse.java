package com.fya.credits.dto.response;

import com.fya.credits.model.Client;

public record ClientResponse(
    String document,
    String firstName,
    String secondName,
    String firstSurname,
    String secondSurname,
    String fullName) {
  public static ClientResponse from(Client client) {
    return new ClientResponse(
        client.getDocument(),
        client.getFirstName(),
        client.getSecondName(),
        client.getFirstSurname(),
        client.getSecondSurname(),
        client.getFullName());
  }
}
