package com.fya.credits.service.email;

import com.fya.credits.exception.DependencyUnavailableException;
import com.fya.credits.model.EmailJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class MailgunEmailService implements EmailService {
  private final RestClient.Builder restClientBuilder;
  private final String apiKey;
  private final String domain;
  private final String baseUrl;
  private final String fromEmail;
  private final String fromName;

  public MailgunEmailService(
      RestClient.Builder restClientBuilder,
      @Value("${app.mailgun.api-key}") String apiKey,
      @Value("${app.mailgun.domain}") String domain,
      @Value("${app.mailgun.base-url}") String baseUrl,
      @Value("${app.mailgun.from-email}") String fromEmail,
      @Value("${app.mailgun.from-name}") String fromName) {
    this.restClientBuilder = restClientBuilder;
    this.apiKey = apiKey;
    this.domain = domain;
    this.baseUrl = baseUrl;
    this.fromEmail = fromEmail;
    this.fromName = fromName;
  }

  @Override
  public void sendCreditRegistered(EmailJob job) {
    ensureConfigured();
    LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("from", "%s <%s>".formatted(fromName, fromEmail));
    body.add("to", job.getRecipient());
    body.add("subject", "Nuevo crédito registrado");
    body.add("text", """
        Se registró un nuevo crédito.

        Cliente: %s
        Valor: %s
        Comercial: %s
        Fecha de registro: %s
        """.formatted(
        job.getClientName(),
        job.getCreditAmount().toPlainString(),
        job.getSalespersonName(),
        job.getRegisteredAt()));

    restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeaders(headers -> headers.setBasicAuth("api", apiKey))
        .build()
        .post()
        .uri("/{domain}/messages", domain)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  private void ensureConfigured() {
    if (!StringUtils.hasText(apiKey)
        || !StringUtils.hasText(domain)
        || !StringUtils.hasText(fromEmail)) {
      throw new DependencyUnavailableException("Mailgun no está configurado");
    }
  }
}
