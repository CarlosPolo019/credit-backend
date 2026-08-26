package com.fya.credits.repository;

import com.fya.credits.exception.DependencyUnavailableException;
import com.fya.credits.model.EmailJob;
import com.fya.credits.model.EmailJobStatus;
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
public class EmailJobRepository {
  private static final String COLLECTION = "email_jobs";
  private final Firestore firestore;

  public EmailJobRepository(Firestore firestore) {
    this.firestore = firestore;
  }

  public EmailJob save(EmailJob job) {
    try {
      DocumentReference reference = firestore.collection(COLLECTION).document();
      job.setId(reference.getId());
      reference.set(toFirestore(job)).get();
      return job;
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo crear el trabajo de correo");
    }
  }

  public List<EmailJob> findEligible(Instant now, int batchSize) {
    try {
      return firestore.collection(COLLECTION)
          .get()
          .get()
          .getDocuments()
          .stream()
          .map(this::fromSnapshot)
          .filter(job -> job.getStatus() == EmailJobStatus.PENDING || job.getStatus() == EmailJobStatus.RETRY)
          .filter(job -> job.getNextAttemptAt() == null || !job.getNextAttemptAt().isAfter(now))
          .limit(batchSize)
          .toList();
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudieron consultar los trabajos de correo");
    }
  }

  public List<EmailJob> listAll(EmailJobQuery query) {
    try {
      return firestore.collection(COLLECTION)
          .get()
          .get()
          .getDocuments()
          .stream()
          .map(this::fromSnapshot)
          .filter(job -> matches(job, query))
          .sorted(comparator(query))
          .toList();
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudieron consultar los trabajos de correo");
    }
  }

  private boolean matches(EmailJob job, EmailJobQuery query) {
    if (StringUtils.hasText(query.status()) && job.getStatus() != EmailJobStatus.valueOf(query.status())) {
      return false;
    }
    if (!StringUtils.hasText(query.search())) {
      return true;
    }
    String searchKey = InputNormalizer.searchKey(query.search());
    return InputNormalizer.searchKey(Optional.ofNullable(job.getClientName()).orElse("")).contains(searchKey)
        || InputNormalizer.searchKey(Optional.ofNullable(job.getRecipient()).orElse("")).contains(searchKey);
  }

  private Comparator<EmailJob> comparator(EmailJobQuery query) {
    Comparator<EmailJob> comparator = "status".equals(query.sortBy())
        ? Comparator.comparing(job -> job.getStatus().name())
        : Comparator.comparing(EmailJob::getCreatedAt, Comparator.nullsLast(Instant::compareTo));
    return "asc".equals(query.direction()) ? comparator : comparator.reversed();
  }

  public boolean claimProcessing(String jobId) {
    try {
      DocumentReference reference = firestore.collection(COLLECTION).document(jobId);
      return firestore.runTransaction(transaction -> {
        DocumentSnapshot snapshot = transaction.get(reference).get();
        if (!snapshot.exists()) {
          return false;
        }
        EmailJobStatus status = EmailJobStatus.valueOf(snapshot.getString("status"));
        if (status != EmailJobStatus.PENDING && status != EmailJobStatus.RETRY) {
          return false;
        }
        transaction.update(reference, Map.of("status", EmailJobStatus.PROCESSING.name()));
        return true;
      }).get();
    } catch (Exception ex) {
      return false;
    }
  }

  public void markSent(String jobId, Instant now) {
    update(jobId, Map.of(
        "status", EmailJobStatus.SENT.name(),
        "processedAt", Date.from(now),
        "lastError", ""));
  }

  public void markRetryOrFailed(
      EmailJob job,
      String error,
      Instant processedAt,
      Instant nextAttemptAt,
      int maxAttempts) {
    int attempts = job.getAttempts() == null ? 1 : job.getAttempts() + 1;
    EmailJobStatus nextStatus = attempts >= maxAttempts ? EmailJobStatus.FAILED : EmailJobStatus.RETRY;
    Map<String, Object> updates = new HashMap<>();
    updates.put("status", nextStatus.name());
    updates.put("attempts", attempts);
    updates.put("lastError", error);
    updates.put("processedAt", Date.from(processedAt));
    updates.put("nextAttemptAt", Date.from(nextAttemptAt));
    update(job.getId(), updates);
  }

  private void update(String jobId, Map<String, Object> values) {
    try {
      firestore.collection(COLLECTION).document(jobId).update(values).get();
    } catch (Exception ex) {
      throw new DependencyUnavailableException("No se pudo actualizar el trabajo de correo");
    }
  }

  private Map<String, Object> toFirestore(EmailJob job) {
    Map<String, Object> values = new HashMap<>();
    values.put("id", job.getId());
    values.put("creditId", job.getCreditId());
    values.put("recipient", job.getRecipient());
    values.put("clientName", job.getClientName());
    values.put("creditAmount", job.getCreditAmount().toPlainString());
    values.put("salespersonName", job.getSalespersonName());
    values.put("registeredAt", Date.from(job.getRegisteredAt()));
    values.put("status", job.getStatus().name());
    values.put("attempts", job.getAttempts());
    values.put("lastError", job.getLastError());
    values.put("createdAt", Date.from(job.getCreatedAt()));
    values.put("processedAt", job.getProcessedAt() == null ? null : Date.from(job.getProcessedAt()));
    values.put("nextAttemptAt", job.getNextAttemptAt() == null ? null : Date.from(job.getNextAttemptAt()));
    return values;
  }

  private EmailJob fromSnapshot(DocumentSnapshot snapshot) {
    EmailJob job = new EmailJob();
    job.setId(snapshot.getString("id") != null ? snapshot.getString("id") : snapshot.getId());
    job.setCreditId(snapshot.getString("creditId"));
    job.setRecipient(snapshot.getString("recipient"));
    job.setClientName(snapshot.getString("clientName"));
    job.setCreditAmount(bigDecimal(snapshot.get("creditAmount")));
    job.setSalespersonName(snapshot.getString("salespersonName"));
    job.setRegisteredAt(instant(snapshot.get("registeredAt")));
    job.setStatus(EmailJobStatus.valueOf(snapshot.getString("status")));
    Long attempts = snapshot.getLong("attempts");
    job.setAttempts(attempts == null ? 0 : attempts.intValue());
    job.setLastError(snapshot.getString("lastError"));
    job.setCreatedAt(instant(snapshot.get("createdAt")));
    job.setProcessedAt(instant(snapshot.get("processedAt")));
    job.setNextAttemptAt(instant(snapshot.get("nextAttemptAt")));
    return job;
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
