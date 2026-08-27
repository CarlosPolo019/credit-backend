package com.fya.credits.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fya.credits.dto.request.CreateCreditRequest;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.exception.NotFoundException;
import com.fya.credits.model.AppUser;
import com.fya.credits.model.Credit;
import com.fya.credits.model.CreditAuditEntry;
import com.fya.credits.model.EmailJob;
import com.fya.credits.repository.CreditAuditRepository;
import com.fya.credits.repository.CreditRepository;
import com.fya.credits.repository.EmailJobRepository;
import com.fya.credits.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {
  @Mock CreditRepository creditRepository;
  @Mock EmailJobRepository emailJobRepository;
  @Mock UserRepository userRepository;
  @Mock CreditAuditRepository creditAuditRepository;
  @Mock ClientService clientService;

  @Test
  void createsCreditAndPendingEmailJob() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, creditAuditRepository, clientService, clock, "fyasocialcapital@gmail.com");
    when(userRepository.findActiveByDocumentNormalized("900100001"))
        .thenReturn(Optional.of(user("900100001", "Carlos Escorcia")));
    when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> {
      Credit credit = invocation.getArgument(0);
      credit.setId("credit-1");
      return credit;
    });

    service.create(new CreateCreditRequest(
        "  Pepito  ",
        "",
        " Perez ",
        "",
        " 100000001 ",
        BigDecimal.valueOf(7800000),
        BigDecimal.valueOf(2),
        10), "900100001");

    ArgumentCaptor<Credit> creditCaptor = ArgumentCaptor.forClass(Credit.class);
    ArgumentCaptor<EmailJob> jobCaptor = ArgumentCaptor.forClass(EmailJob.class);
    verify(creditRepository).save(creditCaptor.capture());
    verify(emailJobRepository).save(jobCaptor.capture());

    assertThat(creditCaptor.getValue().getClientFirstName()).isEqualTo("Pepito");
    assertThat(creditCaptor.getValue().getClientFirstSurname()).isEqualTo("Perez");
    assertThat(creditCaptor.getValue().getClientName()).isEqualTo("Pepito Perez");
    assertThat(creditCaptor.getValue().getClientDocument()).isEqualTo("100000001");
    assertThat(creditCaptor.getValue().getRegisteredByUserId()).isEqualTo("900100001");
    assertThat(creditCaptor.getValue().getSalespersonDocument()).isEqualTo("900100001");
    assertThat(creditCaptor.getValue().getSalespersonName()).isEqualTo("Carlos Escorcia");
    assertThat(creditCaptor.getValue().getIsActive()).isTrue();
    assertThat(jobCaptor.getValue().getCreditId()).isEqualTo("credit-1");
    assertThat(jobCaptor.getValue().getRecipient()).isEqualTo("fyasocialcapital@gmail.com");
    assertThat(jobCaptor.getValue().getSalespersonName()).isEqualTo("Carlos Escorcia");
    verify(clientService).upsert("100000001", "Pepito", "", "Perez", "");
  }

  @Test
  void rejectsNonNumericClientDocument() {
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, creditAuditRepository, clientService, Clock.systemUTC(), "fyasocialcapital@gmail.com");
    when(userRepository.findActiveByDocumentNormalized("900100001"))
        .thenReturn(Optional.of(user("900100001", "Carlos Escorcia")));

    assertThatThrownBy(() -> service.create(new CreateCreditRequest(
        "Pepito",
        "",
        "Perez",
        "",
        "ABC-123",
        BigDecimal.valueOf(7800000),
        BigDecimal.valueOf(2),
        10), "900100001"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("La cédula o ID debe ser numérica");
  }

  @Test
  void rejectsCreateWhenAuthenticatedUserIsNotRegistered() {
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, creditAuditRepository, clientService, Clock.systemUTC(), "fyasocialcapital@gmail.com");
    when(userRepository.findActiveByDocumentNormalized("999999999"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(new CreateCreditRequest(
        "Pepito",
        "",
        "Perez",
        "",
        "100000001",
        BigDecimal.valueOf(7800000),
        BigDecimal.valueOf(2),
        10), "999999999"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("El usuario autenticado no está registrado");
  }

  @Test
  void updatesEditableFieldsAndRecordsAuditEntry() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, creditAuditRepository, clientService, clock, "fyasocialcapital@gmail.com");
    Credit existing = new Credit();
    existing.setId("credit-1");
    existing.setClientFirstName("Pepito");
    existing.setClientFirstSurname("Perez");
    existing.setClientDocument("100000001");
    existing.setAmount(BigDecimal.valueOf(7800000));
    existing.setInterestRate(BigDecimal.valueOf(2));
    existing.setTermMonths(10);
    existing.setSalespersonDocument("900100001");
    existing.setSalespersonName("Carlos Escorcia");
    existing.setIsActive(true);
    existing.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
    when(creditRepository.findActiveById("credit-1")).thenReturn(Optional.of(existing));
    when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userRepository.findActiveByDocumentNormalized("900100002"))
        .thenReturn(Optional.of(user("900100002", "Jennifer Navarro")));

    service.update("credit-1", new CreateCreditRequest(
        "Pepito", "", "Perez", "", "100000001",
        BigDecimal.valueOf(9000000), BigDecimal.valueOf(3), 12), "900100002");

    ArgumentCaptor<Credit> creditCaptor = ArgumentCaptor.forClass(Credit.class);
    verify(creditRepository).save(creditCaptor.capture());
    assertThat(creditCaptor.getValue().getAmount()).isEqualByComparingTo("9000000");
    assertThat(creditCaptor.getValue().getTermMonths()).isEqualTo(12);
    assertThat(creditCaptor.getValue().getClientName()).isEqualTo("Pepito Perez");
    assertThat(creditCaptor.getValue().getSalespersonName()).isEqualTo("Carlos Escorcia");
    assertThat(creditCaptor.getValue().getUpdatedAt()).isEqualTo(Instant.parse("2026-08-25T20:00:00Z"));

    ArgumentCaptor<CreditAuditEntry> auditCaptor = ArgumentCaptor.forClass(CreditAuditEntry.class);
    verify(creditAuditRepository).save(auditCaptor.capture());
    CreditAuditEntry entry = auditCaptor.getValue();
    assertThat(entry.getCreditId()).isEqualTo("credit-1");
    assertThat(entry.getAction()).isEqualTo("UPDATED");
    assertThat(entry.getChangedByDocument()).isEqualTo("900100002");
    assertThat(entry.getChangedByName()).isEqualTo("Jennifer Navarro");
    assertThat(entry.getChanges()).containsEntry("amount", new CreditAuditEntry.FieldChange("7800000", "9000000"));
    assertThat(entry.getChanges()).containsEntry("termMonths", new CreditAuditEntry.FieldChange("10", "12"));
    assertThat(entry.getChanges()).doesNotContainKey("clientFirstName");
    verify(clientService).upsert("100000001", "Pepito", "", "Perez", "");
  }

  @Test
  void skipsAuditEntryWhenUpdateDoesNotChangeAnything() {
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, creditAuditRepository, clientService, Clock.systemUTC(), "fyasocialcapital@gmail.com");
    Credit existing = new Credit();
    existing.setId("credit-1");
    existing.setClientFirstName("Pepito");
    existing.setClientFirstSurname("Perez");
    existing.setClientDocument("100000001");
    existing.setAmount(BigDecimal.valueOf(7800000));
    existing.setInterestRate(BigDecimal.valueOf(2));
    existing.setTermMonths(10);
    existing.setIsActive(true);
    when(creditRepository.findActiveById("credit-1")).thenReturn(Optional.of(existing));
    when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userRepository.findActiveByDocumentNormalized("900100002"))
        .thenReturn(Optional.of(user("900100002", "Jennifer Navarro")));

    service.update("credit-1", new CreateCreditRequest(
        "Pepito", "", "Perez", "", "100000001",
        BigDecimal.valueOf(7800000), BigDecimal.valueOf(2), 10), "900100002");

    verify(creditAuditRepository, never()).save(any());
  }

  @Test
  void rejectsUpdateWhenCreditDoesNotExist() {
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, creditAuditRepository, clientService, Clock.systemUTC(), "fyasocialcapital@gmail.com");
    when(creditRepository.findActiveById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update("missing", new CreateCreditRequest(
        "Pepito", "", "Perez", "", "100000001",
        BigDecimal.valueOf(9000000), BigDecimal.valueOf(3), 12), "900100002"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deleteRecordsAuditEntry() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, creditAuditRepository, clientService, clock, "fyasocialcapital@gmail.com");
    Credit existing = new Credit();
    existing.setId("credit-1");
    existing.setIsActive(true);
    when(creditRepository.findActiveById("credit-1")).thenReturn(Optional.of(existing));
    when(userRepository.findActiveByDocumentNormalized("900100002"))
        .thenReturn(Optional.of(user("900100002", "Jennifer Navarro")));

    service.delete("credit-1", "900100002");

    verify(creditRepository).softDelete("credit-1", clock.instant());
    ArgumentCaptor<CreditAuditEntry> auditCaptor = ArgumentCaptor.forClass(CreditAuditEntry.class);
    verify(creditAuditRepository).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getAction()).isEqualTo("DELETED");
    assertThat(auditCaptor.getValue().getChangedByDocument()).isEqualTo("900100002");
  }

  @Test
  void rejectsArbitrarySortFields() {
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, creditAuditRepository, clientService, Clock.systemUTC(), "fyasocialcapital@gmail.com");

    assertThatThrownBy(() -> service.list(null, null, null, "clientName", "desc"))
        .isInstanceOf(BadRequestException.class);
  }

  private AppUser user(String document, String fullName) {
    AppUser user = new AppUser();
    user.setId(document);
    user.setFullName(fullName);
    user.setDocument(document);
    user.setDocumentNormalized(document);
    user.setRole("COMMERCIAL");
    user.setIsActive(true);
    return user;
  }
}
