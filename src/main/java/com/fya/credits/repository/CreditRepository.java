package com.fya.credits.repository;

import com.fya.credits.exception.DependencyUnavailableException;
import com.fya.credits.model.Credit;
import com.fya.credits.service.InputNormalizer;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class CreditRepository {
  private static final String COLLECTION = "credits";
  private final Firestore firestore;

  public CreditRepository(Firestore firestore) {
    this.firestore = firestore;
  }

  public Credit save(Credit credit) {
    try {
      DocumentReference reference = StringUtils.hasText(credit.getId())
          ? firestore.collection(COLLECTION).document(credit.getId())
          : firestore.collection(COLLECTION).document();
      credit.setId(reference.getId());
      reference.set(toFirestore(credit)).get();
      return credit;
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo guardar el crédito");
    }
  }

  public Optional<Credit> findActiveById(String id) {
    try {
      DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
      if (!snapshot.exists()) {
        return Optional.empty();
      }
      Credit credit = fromSnapshot(snapshot);
      return Boolean.TRUE.equals(credit.getIsActive()) ? Optional.of(credit) : Optional.empty();
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo consultar el crédito");
    }
  }

  public List<Credit> listActive(CreditQuery query) {
    try {
      List<Credit> credits = firestore.collection(COLLECTION)
          .whereEqualTo("isActive", true)
          .get()
          .get()
          .getDocuments()
          .stream()
          .map(this::fromSnapshot)
          .filter(credit -> matches(credit, query))
          .sorted(comparator(query))
          .toList();
      return credits;
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudieron consultar los créditos");
    }
  }

  public void softDelete(String id, Instant now) {
    try {
      Map<String, Object> updates = new HashMap<>();
      updates.put("isActive", false);
      updates.put("deletedAt", Date.from(now));
      updates.put("updatedAt", Date.from(now));
      firestore.collection(COLLECTION).document(id).update(updates).get();
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo deshabilitar el crédito");
    }
  }

  private boolean matches(Credit credit, CreditQuery query) {
    return contains(credit.getClientNameNormalized(), query.clientName())
        && contains(credit.getClientDocumentNormalized(), query.clientDocument())
        && contains(credit.getSalespersonNameNormalized(), query.salesperson());
  }

  private boolean contains(String field, String query) {
    if (!StringUtils.hasText(query)) {
      return true;
    }
    return Optional.ofNullable(field).orElse("").contains(InputNormalizer.searchKey(query));
  }

  private Comparator<Credit> comparator(CreditQuery query) {
    Comparator<Credit> comparator = "amount".equals(query.sortBy())
        ? Comparator.comparing(Credit::getAmount, Comparator.nullsLast(BigDecimal::compareTo))
        : Comparator.comparing(Credit::getCreatedAt, Comparator.nullsLast(Instant::compareTo));
    if ("desc".equals(query.direction())) {
      comparator = comparator.reversed();
    }
    return comparator;
  }

  private Map<String, Object> toFirestore(Credit credit) {
    Map<String, Object> values = new HashMap<>();
    values.put("id", credit.getId());
    values.put("clientName", credit.getClientName());
    values.put("clientNameNormalized", credit.getClientNameNormalized());
    values.put("clientDocument", credit.getClientDocument());
    values.put("clientDocumentNormalized", credit.getClientDocumentNormalized());
    values.put("amount", credit.getAmount().toPlainString());
    values.put("interestRate", credit.getInterestRate().toPlainString());
    values.put("termMonths", credit.getTermMonths());
    values.put("salespersonName", credit.getSalespersonName());
    values.put("salespersonNameNormalized", credit.getSalespersonNameNormalized());
    values.put("isActive", credit.getIsActive());
    values.put("createdAt", Date.from(credit.getCreatedAt()));
    values.put("updatedAt", Date.from(credit.getUpdatedAt()));
    values.put("deletedAt", credit.getDeletedAt() == null ? null : Date.from(credit.getDeletedAt()));
    return values;
  }

  private Credit fromSnapshot(DocumentSnapshot snapshot) {
    Credit credit = new Credit();
    credit.setId(snapshot.getString("id") != null ? snapshot.getString("id") : snapshot.getId());
    credit.setClientName(snapshot.getString("clientName"));
    credit.setClientNameNormalized(snapshot.getString("clientNameNormalized"));
    credit.setClientDocument(snapshot.getString("clientDocument"));
    credit.setClientDocumentNormalized(snapshot.getString("clientDocumentNormalized"));
    credit.setAmount(bigDecimal(snapshot.get("amount")));
    credit.setInterestRate(bigDecimal(snapshot.get("interestRate")));
    Long term = snapshot.getLong("termMonths");
    credit.setTermMonths(term == null ? null : term.intValue());
    credit.setSalespersonName(snapshot.getString("salespersonName"));
    credit.setSalespersonNameNormalized(snapshot.getString("salespersonNameNormalized"));
    credit.setIsActive(snapshot.getBoolean("isActive"));
    credit.setCreatedAt(instant(snapshot.get("createdAt")));
    credit.setUpdatedAt(instant(snapshot.get("updatedAt")));
    credit.setDeletedAt(instant(snapshot.get("deletedAt")));
    return credit;
  }

  private BigDecimal bigDecimal(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
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
