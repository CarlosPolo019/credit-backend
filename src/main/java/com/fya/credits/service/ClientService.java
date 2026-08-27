package com.fya.credits.service;

import com.fya.credits.dto.response.ClientResponse;
import com.fya.credits.model.Client;
import com.fya.credits.repository.ClientRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Keeps a "clients" collection in sync with the client fields embedded on
 * every credit, so a cédula that already has a credit can be looked up
 * (autocomplete in the frontend) instead of retyped — avoiding inconsistent
 * names for the same person across credits.
 */
@Service
public class ClientService {
  private static final Logger log = LoggerFactory.getLogger(ClientService.class);

  private final ClientRepository clientRepository;
  private final Clock clock;

  public ClientService(ClientRepository clientRepository, Clock clock) {
    this.clientRepository = clientRepository;
    this.clock = clock;
  }

  public List<ClientResponse> list() {
    return clientRepository.listAll().stream()
        .sorted(Comparator.comparing(Client::getFullName, Comparator.nullsLast(String::compareTo)))
        .map(ClientResponse::from)
        .toList();
  }

  /**
   * Upserts the client behind a cédula whenever a credit is created or
   * edited. Not a hard dependency of the credit flow: a failure here is
   * logged and swallowed so it never blocks saving the credit itself.
   */
  public void upsert(String document, String firstName, String secondName, String firstSurname, String secondSurname) {
    String documentNormalized = InputNormalizer.searchKey(document);
    try {
      String fullName = String.join(" ", List.of(firstName, secondName, firstSurname, secondSurname).stream()
          .filter(part -> part != null && !part.isBlank())
          .toList());
      Instant now = clock.instant();
      Client client = clientRepository.findByDocumentNormalized(documentNormalized).orElseGet(Client::new);
      client.setDocument(document);
      client.setDocumentNormalized(documentNormalized);
      client.setFirstName(firstName);
      client.setSecondName(secondName);
      client.setFirstSurname(firstSurname);
      client.setSecondSurname(secondSurname);
      client.setFullName(fullName);
      client.setFullNameNormalized(InputNormalizer.searchKey(fullName));
      client.setCreatedAt(client.getCreatedAt() == null ? now : client.getCreatedAt());
      client.setUpdatedAt(now);
      clientRepository.save(client);
    } catch (Exception ex) {
      log.warn("No se pudo sincronizar el cliente {}: {}", documentNormalized, InputNormalizer.sanitizeError(ex.getMessage()));
    }
  }
}
