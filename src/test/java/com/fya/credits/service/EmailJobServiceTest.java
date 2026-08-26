package com.fya.credits.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fya.credits.exception.BadRequestException;
import com.fya.credits.model.EmailJob;
import com.fya.credits.model.EmailJobStatus;
import com.fya.credits.repository.EmailJobRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailJobServiceTest {
  @Mock EmailJobRepository emailJobRepository;

  @Test
  void listsJobsFilteredByStatus() {
    EmailJobService service = new EmailJobService(emailJobRepository);
    when(emailJobRepository.listAll(any())).thenReturn(List.of(job(EmailJobStatus.FAILED, "Mailgun respondio 401")));

    var response = service.list("FAILED", null, null, null);

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.items().get(0).status()).isEqualTo("FAILED");
    assertThat(response.items().get(0).lastError()).isEqualTo("Mailgun respondio 401");
  }

  @Test
  void rejectsInvalidStatus() {
    EmailJobService service = new EmailJobService(emailJobRepository);

    assertThatThrownBy(() -> service.list("NOT_A_STATUS", null, null, null))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void rejectsArbitrarySortFields() {
    EmailJobService service = new EmailJobService(emailJobRepository);

    assertThatThrownBy(() -> service.list(null, null, "clientName", null))
        .isInstanceOf(BadRequestException.class);
  }

  private EmailJob job(EmailJobStatus status, String lastError) {
    EmailJob job = new EmailJob();
    job.setId("job-1");
    job.setCreditId("CR-100000001");
    job.setRecipient("fyasocialcapital@gmail.com");
    job.setClientName("Pepito Perez");
    job.setCreditAmount(BigDecimal.valueOf(7800000));
    job.setSalespersonName("Carlos Escorcia");
    job.setRegisteredAt(Instant.parse("2026-08-25T20:00:00Z"));
    job.setStatus(status);
    job.setAttempts(3);
    job.setLastError(lastError);
    job.setCreatedAt(Instant.parse("2026-08-25T20:00:00Z"));
    job.setProcessedAt(Instant.parse("2026-08-25T20:05:00Z"));
    job.setNextAttemptAt(null);
    return job;
  }
}
