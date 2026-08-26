package com.fya.credits.repository;

import com.fya.credits.exception.DependencyUnavailableException;
import com.fya.credits.model.AppUser;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private static final String COLLECTION = "users";
  private final Firestore firestore;

  public UserRepository(Firestore firestore) {
    this.firestore = firestore;
  }

  public AppUser save(AppUser user) {
    try {
      user.setId(user.getDocumentNormalized());
      firestore.collection(COLLECTION)
          .document(user.getDocumentNormalized())
          .set(toFirestore(user))
          .get();
      return user;
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo guardar el usuario");
    }
  }

  public Optional<AppUser> findByDocumentNormalized(String documentNormalized) {
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
      throw new DependencyUnavailableException("No se pudo consultar el usuario");
    }
  }

  public Optional<AppUser> findActiveByDocumentNormalized(String documentNormalized) {
    return findByDocumentNormalized(documentNormalized)
        .filter(user -> Boolean.TRUE.equals(user.getIsActive()));
  }

  private Map<String, Object> toFirestore(AppUser user) {
    Map<String, Object> values = new HashMap<>();
    values.put("id", user.getId());
    values.put("fullName", user.getFullName());
    values.put("document", user.getDocument());
    values.put("documentNormalized", user.getDocumentNormalized());
    values.put("passwordHash", user.getPasswordHash());
    values.put("role", user.getRole());
    values.put("isActive", user.getIsActive());
    values.put("createdAt", Date.from(user.getCreatedAt()));
    values.put("updatedAt", Date.from(user.getUpdatedAt()));
    return values;
  }

  private AppUser fromSnapshot(DocumentSnapshot snapshot) {
    AppUser user = new AppUser();
    user.setId(snapshot.getString("id") != null ? snapshot.getString("id") : snapshot.getId());
    user.setFullName(snapshot.getString("fullName"));
    user.setDocument(snapshot.getString("document"));
    user.setDocumentNormalized(snapshot.getString("documentNormalized"));
    user.setPasswordHash(snapshot.getString("passwordHash"));
    user.setRole(snapshot.getString("role"));
    user.setIsActive(snapshot.getBoolean("isActive"));
    user.setCreatedAt(instant(snapshot.get("createdAt")));
    user.setUpdatedAt(instant(snapshot.get("updatedAt")));
    return user;
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
