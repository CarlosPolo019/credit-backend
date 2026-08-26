package com.fya.credits.service;

import com.fya.credits.dto.request.CreateCreditRequest;
import com.fya.credits.dto.response.CreditAuditEntryResponse;
import com.fya.credits.dto.response.CreditListResponse;
import com.fya.credits.dto.response.CreditResponse;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.exception.NotFoundException;
import com.fya.credits.model.AppUser;
import com.fya.credits.model.Credit;
import com.fya.credits.model.CreditAuditEntry;
import com.fya.credits.model.CreditAuditEntry.FieldChange;
import com.fya.credits.model.EmailJob;
import com.fya.credits.model.EmailJobStatus;
import com.fya.credits.repository.CreditAuditRepository;
import com.fya.credits.repository.CreditQuery;
import com.fya.credits.repository.CreditRepository;
import com.fya.credits.repository.EmailJobRepository;
import com.fya.credits.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CreditService {
  private final CreditRepository creditRepository;
  private final EmailJobRepository emailJobRepository;
  private final UserRepository userRepository;
  private final CreditAuditRepository creditAuditRepository;
  private final Clock clock;
  private final String notificationEmail;

  public CreditService(
      CreditRepository creditRepository,
      EmailJobRepository emailJobRepository,
      UserRepository userRepository,
      CreditAuditRepository creditAuditRepository,
      Clock clock,
      @Value("${app.mailgun.notification-email}") String notificationEmail) {
    this.creditRepository = creditRepository;
    this.emailJobRepository = emailJobRepository;
    this.userRepository = userRepository;
    this.creditAuditRepository = creditAuditRepository;
    this.clock = clock;
    this.notificationEmail = notificationEmail;
  }

  public CreditResponse create(CreateCreditRequest request, String authenticatedDocument) {
    Instant now = clock.instant();
    String documentNormalized = InputNormalizer.searchKey(authenticatedDocument);
    var salesperson = userRepository.findActiveByDocumentNormalized(documentNormalized)
        .orElseThrow(() -> new BadRequestException("El usuario autenticado no está registrado"));
    String clientFirstName = InputNormalizer.cleanText(request.clientFirstName());
    String clientSecondName = InputNormalizer.cleanText(request.clientSecondName());
    String clientFirstSurname = InputNormalizer.cleanText(request.clientFirstSurname());
    String clientSecondSurname = InputNormalizer.cleanText(request.clientSecondSurname());
    String clientDocument = numericDocument(request.clientDocument());
    String clientName = joinName(clientFirstName, clientSecondName, clientFirstSurname, clientSecondSurname);

    Credit credit = new Credit();
    credit.setClientFirstName(clientFirstName);
    credit.setClientSecondName(clientSecondName);
    credit.setClientFirstSurname(clientFirstSurname);
    credit.setClientSecondSurname(clientSecondSurname);
    credit.setClientName(clientName);
    credit.setClientNameNormalized(InputNormalizer.searchKey(credit.getClientName()));
    credit.setClientDocument(clientDocument);
    credit.setClientDocumentNormalized(InputNormalizer.searchKey(credit.getClientDocument()));
    credit.setAmount(request.amount());
    credit.setInterestRate(request.interestRate());
    credit.setTermMonths(request.termMonths());
    credit.setRegisteredByUserId(salesperson.getId());
    credit.setSalespersonDocument(salesperson.getDocument());
    credit.setSalespersonDocumentNormalized(salesperson.getDocumentNormalized());
    credit.setSalespersonName(salesperson.getFullName());
    credit.setSalespersonNameNormalized(InputNormalizer.searchKey(credit.getSalespersonName()));
    credit.setIsActive(true);
    credit.setCreatedAt(now);
    credit.setUpdatedAt(now);
    credit.setDeletedAt(null);

    Credit saved = creditRepository.save(credit);
    emailJobRepository.save(emailJobFor(saved, now));
    return CreditResponse.from(saved);
  }

  public CreditListResponse list(
      String clientName,
      String clientDocument,
      String salesperson,
      String sortBy,
      String direction) {
    String resolvedSortBy = resolveSortBy(sortBy);
    String resolvedDirection = resolveDirection(direction);
    List<CreditResponse> items = creditRepository
        .listActive(new CreditQuery(clientName, clientDocument, salesperson, resolvedSortBy, resolvedDirection))
        .stream()
        .map(CreditResponse::from)
        .toList();
    return new CreditListResponse(items, items.size());
  }

  public CreditResponse getActive(String id) {
    return creditRepository.findActiveById(id)
        .map(CreditResponse::from)
        .orElseThrow(() -> new NotFoundException("Crédito no disponible"));
  }

  public CreditResponse update(String id, CreateCreditRequest request, String authenticatedDocument) {
    Credit credit = creditRepository.findActiveById(id)
        .orElseThrow(() -> new NotFoundException("Crédito no disponible"));
    AppUser editor = resolveEditor(authenticatedDocument);
    Map<String, FieldChange> before = snapshotEditableFields(credit);

    String clientFirstName = InputNormalizer.cleanText(request.clientFirstName());
    String clientSecondName = InputNormalizer.cleanText(request.clientSecondName());
    String clientFirstSurname = InputNormalizer.cleanText(request.clientFirstSurname());
    String clientSecondSurname = InputNormalizer.cleanText(request.clientSecondSurname());
    String clientDocument = numericDocument(request.clientDocument());
    String clientName = joinName(clientFirstName, clientSecondName, clientFirstSurname, clientSecondSurname);

    credit.setClientFirstName(clientFirstName);
    credit.setClientSecondName(clientSecondName);
    credit.setClientFirstSurname(clientFirstSurname);
    credit.setClientSecondSurname(clientSecondSurname);
    credit.setClientName(clientName);
    credit.setClientNameNormalized(InputNormalizer.searchKey(credit.getClientName()));
    credit.setClientDocument(clientDocument);
    credit.setClientDocumentNormalized(InputNormalizer.searchKey(credit.getClientDocument()));
    credit.setAmount(request.amount());
    credit.setInterestRate(request.interestRate());
    credit.setTermMonths(request.termMonths());
    credit.setUpdatedAt(clock.instant());

    Credit saved = creditRepository.save(credit);
    Map<String, FieldChange> changes = diff(before, snapshotEditableFields(saved));
    if (!changes.isEmpty()) {
      creditAuditRepository.save(auditEntry(saved.getId(), "UPDATED", editor, changes));
    }
    return CreditResponse.from(saved);
  }

  public void delete(String id, String authenticatedDocument) {
    Credit credit = creditRepository.findActiveById(id)
        .orElseThrow(() -> new NotFoundException("Crédito no disponible"));
    AppUser editor = resolveEditor(authenticatedDocument);
    creditRepository.softDelete(credit.getId(), clock.instant());
    creditAuditRepository.save(auditEntry(credit.getId(), "DELETED", editor, Map.of()));
  }

  public List<CreditAuditEntryResponse> listAudit(String creditId) {
    return creditAuditRepository.listByCreditId(creditId).stream()
        .map(CreditAuditEntryResponse::from)
        .toList();
  }

  private AppUser resolveEditor(String authenticatedDocument) {
    String documentNormalized = InputNormalizer.searchKey(authenticatedDocument);
    return userRepository.findActiveByDocumentNormalized(documentNormalized)
        .orElseThrow(() -> new BadRequestException("El usuario autenticado no está registrado"));
  }

  private Map<String, FieldChange> snapshotEditableFields(Credit credit) {
    Map<String, FieldChange> snapshot = new LinkedHashMap<>();
    snapshot.put("clientFirstName", new FieldChange(credit.getClientFirstName(), null));
    snapshot.put("clientSecondName", new FieldChange(credit.getClientSecondName(), null));
    snapshot.put("clientFirstSurname", new FieldChange(credit.getClientFirstSurname(), null));
    snapshot.put("clientSecondSurname", new FieldChange(credit.getClientSecondSurname(), null));
    snapshot.put("clientDocument", new FieldChange(credit.getClientDocument(), null));
    snapshot.put("amount", new FieldChange(credit.getAmount() == null ? null : credit.getAmount().toPlainString(), null));
    snapshot.put("interestRate", new FieldChange(credit.getInterestRate() == null ? null : credit.getInterestRate().toPlainString(), null));
    snapshot.put("termMonths", new FieldChange(credit.getTermMonths() == null ? null : credit.getTermMonths().toString(), null));
    return snapshot;
  }

  private Map<String, FieldChange> diff(Map<String, FieldChange> before, Map<String, FieldChange> after) {
    Map<String, FieldChange> changes = new LinkedHashMap<>();
    before.forEach((field, beforeValue) -> {
      String previous = normalizeBlank(beforeValue.before());
      String next = normalizeBlank(after.get(field).before());
      if (!Objects.equals(previous, next)) {
        changes.put(field, new FieldChange(previous, next));
      }
    });
    return changes;
  }

  private String normalizeBlank(String value) {
    return value == null ? "" : value;
  }

  private CreditAuditEntry auditEntry(String creditId, String action, AppUser editor, Map<String, FieldChange> changes) {
    CreditAuditEntry entry = new CreditAuditEntry();
    entry.setCreditId(creditId);
    entry.setAction(action);
    entry.setChangedByUserId(editor.getId());
    entry.setChangedByDocument(editor.getDocument());
    entry.setChangedByName(editor.getFullName());
    entry.setChangedAt(clock.instant());
    entry.setChanges(changes);
    return entry;
  }

  private EmailJob emailJobFor(Credit credit, Instant now) {
    EmailJob job = new EmailJob();
    job.setCreditId(credit.getId());
    job.setRecipient(notificationEmail);
    job.setClientName(credit.getClientName());
    job.setCreditAmount(credit.getAmount());
    job.setSalespersonName(credit.getSalespersonName());
    job.setRegisteredAt(credit.getCreatedAt());
    job.setStatus(EmailJobStatus.PENDING);
    job.setAttempts(0);
    job.setLastError("");
    job.setCreatedAt(now);
    job.setProcessedAt(null);
    job.setNextAttemptAt(now);
    return job;
  }

  private String resolveSortBy(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return "createdAt";
    }
    if (!sortBy.equals("createdAt") && !sortBy.equals("amount")) {
      throw new BadRequestException("sortBy no permitido");
    }
    return sortBy;
  }

  private String resolveDirection(String direction) {
    if (direction == null || direction.isBlank()) {
      return "desc";
    }
    if (!direction.equals("asc") && !direction.equals("desc")) {
      throw new BadRequestException("direction no permitida");
    }
    return direction;
  }

  private String numericDocument(String value) {
    String document = InputNormalizer.cleanText(value);
    if (!document.matches("\\d+")) {
      throw new BadRequestException("La cédula o ID debe ser numérica");
    }
    return document;
  }

  private String joinName(String firstName, String secondName, String firstSurname, String secondSurname) {
    return String.join(" ", List.of(firstName, secondName, firstSurname, secondSurname).stream()
        .filter(part -> part != null && !part.isBlank())
        .toList());
  }
}
