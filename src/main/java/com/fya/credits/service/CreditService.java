package com.fya.credits.service;

import com.fya.credits.dto.request.CreateCreditRequest;
import com.fya.credits.dto.response.CreditListResponse;
import com.fya.credits.dto.response.CreditResponse;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.exception.NotFoundException;
import com.fya.credits.model.Credit;
import com.fya.credits.model.EmailJob;
import com.fya.credits.model.EmailJobStatus;
import com.fya.credits.repository.CreditQuery;
import com.fya.credits.repository.CreditRepository;
import com.fya.credits.repository.EmailJobRepository;
import com.fya.credits.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CreditService {
  private final CreditRepository creditRepository;
  private final EmailJobRepository emailJobRepository;
  private final UserRepository userRepository;
  private final Clock clock;
  private final String notificationEmail;

  public CreditService(
      CreditRepository creditRepository,
      EmailJobRepository emailJobRepository,
      UserRepository userRepository,
      Clock clock,
      @Value("${app.mailgun.notification-email}") String notificationEmail) {
    this.creditRepository = creditRepository;
    this.emailJobRepository = emailJobRepository;
    this.userRepository = userRepository;
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

  public CreditResponse update(String id, CreateCreditRequest request) {
    Credit credit = creditRepository.findActiveById(id)
        .orElseThrow(() -> new NotFoundException("Crédito no disponible"));
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
    return CreditResponse.from(saved);
  }

  public void delete(String id) {
    Credit credit = creditRepository.findActiveById(id)
        .orElseThrow(() -> new NotFoundException("Crédito no disponible"));
    creditRepository.softDelete(credit.getId(), clock.instant());
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
