package com.fya.credits.repository;

import com.fya.credits.exception.DependencyUnavailableException;
import com.fya.credits.model.CreditAuditEntry;
import com.fya.credits.model.CreditAuditEntry.FieldChange;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class CreditAuditRepository {
  private static final String COLLECTION = "credit_audit_logs";
  private final Firestore firestore;

  public CreditAuditRepository(Firestore firestore) {
    this.firestore = firestore;
  }

  public CreditAuditEntry save(CreditAuditEntry entry) {
    try {
      DocumentReference reference = firestore.collection(COLLECTION).document();
      entry.setId(reference.getId());
      reference.set(toFirestore(entry)).get();
      return entry;
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo guardar el historial del crédito");
    }
  }

  public List<CreditAuditEntry> listByCreditId(String creditId) {
    try {
      return firestore.collection(COLLECTION)
          .whereEqualTo("creditId", creditId)
          .get()
          .get()
          .getDocuments()
          .stream()
          .map(this::fromSnapshot)
          .sorted(Comparator.comparing(CreditAuditEntry::getChangedAt, Comparator.nullsLast(Instant::compareTo)).reversed())
          .toList();
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo consultar el historial del crédito");
    }
  }

  private Map<String, Object> toFirestore(CreditAuditEntry entry) {
    Map<String, Object> values = new HashMap<>();
    values.put("id", entry.getId());
    values.put("creditId", entry.getCreditId());
    values.put("action", entry.getAction());
    values.put("changedByUserId", entry.getChangedByUserId());
    values.put("changedByDocument", entry.getChangedByDocument());
    values.put("changedByName", entry.getChangedByName());
    values.put("changedAt", Date.from(entry.getChangedAt()));
    Map<String, Object> changes = new LinkedHashMap<>();
    if (entry.getChanges() != null) {
      entry.getChanges().forEach((field, change) -> changes.put(field, Map.of(
          "before", change.before() == null ? "" : change.before(),
          "after", change.after() == null ? "" : change.after())));
    }
    values.put("changes", changes);
    return values;
  }

  private CreditAuditEntry fromSnapshot(DocumentSnapshot snapshot) {
    CreditAuditEntry entry = new CreditAuditEntry();
    entry.setId(snapshot.getString("id") != null ? snapshot.getString("id") : snapshot.getId());
    entry.setCreditId(snapshot.getString("creditId"));
    entry.setAction(snapshot.getString("action"));
    entry.setChangedByUserId(snapshot.getString("changedByUserId"));
    entry.setChangedByDocument(snapshot.getString("changedByDocument"));
    entry.setChangedByName(snapshot.getString("changedByName"));
    entry.setChangedAt(instant(snapshot.get("changedAt")));
    Map<String, FieldChange> changes = new LinkedHashMap<>();
    Object rawChanges = snapshot.get("changes");
    if (rawChanges instanceof Map<?, ?> map) {
      map.forEach((field, value) -> {
        if (value instanceof Map<?, ?> change) {
          Object before = change.get("before");
          Object after = change.get("after");
          changes.put(String.valueOf(field), new FieldChange(
              before == null ? "" : String.valueOf(before),
              after == null ? "" : String.valueOf(after)));
        }
      });
    }
    entry.setChanges(changes);
    return entry;
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
