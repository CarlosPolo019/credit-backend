package com.fya.credits.background;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.fya.credits.model.EmailJob;
import com.fya.credits.repository.EmailJobRepository;
import com.fya.credits.service.email.EmailService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailJobWorkerTest {
  @Mock EmailJobRepository emailJobRepository;
  @Mock EmailService emailService;

  @Test
  void marksClaimedJobAsSent() {
    EmailJob job = new EmailJob();
    job.setId("job-1");
    job.setAttempts(0);
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
    EmailJobWorker worker = new EmailJobWorker(emailJobRepository, emailService, clock, true, 10, 3);
    when(emailJobRepository.claimProcessing("job-1")).thenReturn(true);

    worker.process(job);

    verify(emailService).sendCreditRegistered(job);
    verify(emailJobRepository).markSent("job-1", clock.instant());
  }

  @Test
  void marksClaimedJobForRetryWhenEmailFails() {
    EmailJob job = new EmailJob();
    job.setId("job-2");
    job.setAttempts(0);
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
    EmailJobWorker worker = new EmailJobWorker(emailJobRepository, emailService, clock, true, 10, 3);
    when(emailJobRepository.claimProcessing("job-2")).thenReturn(true);
    doThrow(new RuntimeException("mailgun timeout")).when(emailService).sendCreditRegistered(job);

    worker.process(job);

    verify(emailJobRepository).markRetryOrFailed(
        job,
        "mailgun timeout",
        clock.instant(),
        clock.instant().plus(Duration.ofMinutes(1)),
        3);
  }
}
