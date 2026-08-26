package com.fya.credits.background;

import com.fya.credits.model.EmailJob;
import com.fya.credits.repository.EmailJobRepository;
import com.fya.credits.service.InputNormalizer;
import com.fya.credits.service.email.EmailService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailJobWorker {
  private final EmailJobRepository emailJobRepository;
  private final EmailService emailService;
  private final Clock clock;
  private final boolean enabled;
  private final int batchSize;
  private final int maxAttempts;

  public EmailJobWorker(
      EmailJobRepository emailJobRepository,
      EmailService emailService,
      Clock clock,
      @Value("${app.email.worker.enabled}") boolean enabled,
      @Value("${app.email.worker.batch-size}") int batchSize,
      @Value("${app.email.worker.max-attempts}") int maxAttempts) {
    this.emailJobRepository = emailJobRepository;
    this.emailService = emailService;
    this.clock = clock;
    this.enabled = enabled;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
  }

  @Scheduled(fixedDelayString = "${app.email.worker.fixed-delay-ms}")
  public void processPendingJobs() {
    if (!enabled) {
      return;
    }
    Instant now = clock.instant();
    for (EmailJob job : emailJobRepository.findEligible(now, batchSize)) {
      process(job);
    }
  }

  void process(EmailJob job) {
    if (!emailJobRepository.claimProcessing(job.getId())) {
      return;
    }
    try {
      emailService.sendCreditRegistered(job);
      emailJobRepository.markSent(job.getId(), clock.instant());
    } catch (RuntimeException ex) {
      int attempts = job.getAttempts() == null ? 0 : job.getAttempts();
      Instant nextAttempt = clock.instant().plus(backoff(attempts + 1));
      emailJobRepository.markRetryOrFailed(
          job,
          InputNormalizer.sanitizeError(ex.getMessage()),
          clock.instant(),
          nextAttempt,
          maxAttempts);
    }
  }

  private Duration backoff(int attempts) {
    return Duration.ofMinutes(Math.min(30, Math.max(1, attempts * attempts)));
  }
}
