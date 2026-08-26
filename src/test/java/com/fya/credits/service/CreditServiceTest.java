package com.fya.credits.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fya.credits.dto.request.CreateCreditRequest;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.model.Credit;
import com.fya.credits.model.EmailJob;
import com.fya.credits.model.AppUser;
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

  @Test
  void createsCreditAndPendingEmailJob() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, clock, "fyasocialcapital@gmail.com");
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
  }

  @Test
  void rejectsNonNumericClientDocument() {
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, Clock.systemUTC(), "fyasocialcapital@gmail.com");
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
        creditRepository, emailJobRepository, userRepository, Clock.systemUTC(), "fyasocialcapital@gmail.com");
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
  void rejectsArbitrarySortFields() {
    CreditService service = new CreditService(
        creditRepository, emailJobRepository, userRepository, Clock.systemUTC(), "fyasocialcapital@gmail.com");

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
