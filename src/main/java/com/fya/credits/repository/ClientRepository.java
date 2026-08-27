package com.fya.credits.repository;

import com.fya.credits.exception.DependencyUnavailableException;
import com.fya.credits.model.Client;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ClientRepository {
  private static final String COLLECTION = "clients";
  private final Firestore firestore;

  public ClientRepository(Firestore firestore) {
    this.firestore = firestore;
  }

  public Client save(Client client) {
    try {
      client.setId(client.getDocumentNormalized());
      firestore.collection(COLLECTION)
          .document(client.getDocumentNormalized())
          .set(toFirestore(client))
          .get();
      return client;
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo guardar el cliente");
    }
  }

  public Optional<Client> findByDocumentNormalized(String documentNormalized) {
    try {
      DocumentSnapshot snapshot = firestore.collection(COLLECTION)
          .document(documentNormalized)
          .get()
          .get();
      if (!snapshot.exists()) {
        return Optional.empty();
      }
      return Optional.of(fromSnapshot(snapshot));
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo consultar el cliente");
    }
  }

  public List<Client> listAll() {
    try {
      return firestore.collection(COLLECTION)
          .get()
          .get()
          .getDocuments()
          .stream()
          .map(this::fromSnapshot)
          .toList();
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudieron consultar los clientes");
    }
  }

  private Map<String, Object> toFirestore(Client client) {
    Map<String, Object> values = new HashMap<>();
    values.put("id", client.getId());
    values.put("document", client.getDocument());
    values.put("documentNormalized", client.getDocumentNormalized());
    values.put("firstName", client.getFirstName());
    values.put("secondName", client.getSecondName());
    values.put("firstSurname", client.getFirstSurname());
    values.put("secondSurname", client.getSecondSurname());
    values.put("fullName", client.getFullName());
    values.put("fullNameNormalized", client.getFullNameNormalized());
    values.put("createdAt", Date.from(client.getCreatedAt()));
    values.put("updatedAt", Date.from(client.getUpdatedAt()));
    return values;
  }

  private Client fromSnapshot(DocumentSnapshot snapshot) {
    Client client = new Client();
    client.setId(snapshot.getString("id") != null ? snapshot.getString("id") : snapshot.getId());
    client.setDocument(snapshot.getString("document"));
    client.setDocumentNormalized(snapshot.getString("documentNormalized"));
    client.setFirstName(snapshot.getString("firstName"));
    client.setSecondName(snapshot.getString("secondName"));
    client.setFirstSurname(snapshot.getString("firstSurname"));
    client.setSecondSurname(snapshot.getString("secondSurname"));
    client.setFullName(snapshot.getString("fullName"));
    client.setFullNameNormalized(snapshot.getString("fullNameNormalized"));
    client.setCreatedAt(instant(snapshot.get("createdAt")));
    client.setUpdatedAt(instant(snapshot.get("updatedAt")));
    return client;
  }

  private Instant instant(Object value) {
    if (value instanceof Timestamp timestamp) {
      return timestamp.toDate().toInstant();
    }
    if (value instanceof Date date) {
      return date.toInstant();
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    return value == null ? null : Instant.parse(String.valueOf(value));
  }
}
