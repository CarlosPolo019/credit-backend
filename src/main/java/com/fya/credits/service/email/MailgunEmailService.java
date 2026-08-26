package com.fya.credits.service.email;

import com.fya.credits.exception.DependencyUnavailableException;
import com.fya.credits.model.EmailJob;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Service
public class MailgunEmailService implements EmailService {
  private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
  private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, h:mm a", ES_CO);
  // Fixed instead of derived from app.frontend.base-url: email clients need a
  // stable, always-reachable image URL, and this asset is only ever hosted
  // at the production domain regardless of which environment sent the email.
  private static final String LOGO_URL = "https://fyatest.cmescorcia.com/fya-mark.png";

  private final RestClient.Builder restClientBuilder;
  private final String apiKey;
  private final String domain;
  private final String baseUrl;
  private final String fromEmail;
  private final String fromName;
  private final String frontendBaseUrl;

  public MailgunEmailService(
      RestClient.Builder restClientBuilder,
      @Value("${app.mailgun.api-key}") String apiKey,
      @Value("${app.mailgun.domain}") String domain,
      @Value("${app.mailgun.base-url}") String baseUrl,
      @Value("${app.mailgun.from-email}") String fromEmail,
      @Value("${app.mailgun.from-name}") String fromName,
      @Value("${app.frontend.base-url}") String frontendBaseUrl) {
    this.restClientBuilder = restClientBuilder;
    this.apiKey = apiKey;
    this.domain = domain;
    this.baseUrl = baseUrl;
    this.fromEmail = fromEmail;
    this.fromName = fromName;
    this.frontendBaseUrl = frontendBaseUrl;
  }

  @Override
  public void sendCreditRegistered(EmailJob job) {
    ensureConfigured();
    String detailUrl = frontendBaseUrl.replaceAll("/+$", "") + "/credits/" + job.getCreditId();
    String amount = formatAmount(job.getCreditAmount());
    String registeredAt = formatDate(job);

    LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("from", "%s <%s>".formatted(fromName, fromEmail));
    body.add("to", job.getRecipient());
    body.add("subject", "Nuevo crédito registrado · %s".formatted(job.getClientName()));
    body.add("text", """
        Se registró un nuevo crédito.

        Cliente: %s
        Valor: %s
        Comercial: %s
        Fecha de registro: %s

        Ver el detalle completo: %s
        """.formatted(job.getClientName(), amount, job.getSalespersonName(), registeredAt, detailUrl));
    body.add("html", buildHtml(job, amount, registeredAt, detailUrl));

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

  private String buildHtml(EmailJob job, String amount, String registeredAt, String detailUrl) {
    return """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;padding:0;background-color:#eef6f2;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#eef6f2;padding:32px 16px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 12px 32px rgba(5,34,36,0.12);">
                    <tr>
                      <td style="background-color:#052224;padding:28px 32px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                          <tr>
                            <td valign="middle" style="width:44px;">
                              <img src="%s" alt="Fya" width="36" height="36" style="display:block;border-radius:8px;" />
                            </td>
                            <td valign="middle" style="padding-left:12px;">
                              <span style="color:#ffffff;font-size:16px;font-weight:600;letter-spacing:0.02em;">Fya Social Capital</span>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 32px 8px 32px;">
                        <span style="display:inline-block;background-color:#e7fbf1;color:#038a54;font-size:12px;font-weight:700;letter-spacing:0.06em;text-transform:uppercase;padding:6px 12px;border-radius:999px;">Nuevo crédito registrado</span>
                        <h1 style="margin:18px 0 6px 0;color:#052224;font-size:22px;line-height:1.3;">%s</h1>
                        <p style="margin:0;color:#5b6b6a;font-size:14px;line-height:1.5;">Se acaba de registrar un crédito en la plataforma. Este es el resumen:</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 32px 8px 32px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5faf8;border:1px solid #e3efeb;border-radius:12px;">
                          <tr>
                            <td style="padding:16px 20px;border-bottom:1px solid #e3efeb;">
                              <div style="color:#8a9997;font-size:11px;font-weight:700;letter-spacing:0.05em;text-transform:uppercase;">Cliente</div>
                              <div style="color:#052224;font-size:15px;font-weight:600;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 20px;border-bottom:1px solid #e3efeb;">
                              <div style="color:#8a9997;font-size:11px;font-weight:700;letter-spacing:0.05em;text-transform:uppercase;">Valor del crédito</div>
                              <div style="color:#03a565;font-size:20px;font-weight:700;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 20px;border-bottom:1px solid #e3efeb;">
                              <div style="color:#8a9997;font-size:11px;font-weight:700;letter-spacing:0.05em;text-transform:uppercase;">Comercial</div>
                              <div style="color:#052224;font-size:15px;font-weight:600;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 20px;">
                              <div style="color:#8a9997;font-size:11px;font-weight:700;letter-spacing:0.05em;text-transform:uppercase;">Fecha de registro</div>
                              <div style="color:#052224;font-size:15px;font-weight:600;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td align="center" style="padding:28px 32px 8px 32px;">
                        <a href="%s" style="display:inline-block;background-color:#00d280;color:#052224;font-size:14px;font-weight:700;text-decoration:none;padding:14px 28px;border-radius:10px;">Ver detalle completo →</a>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:24px 32px 32px 32px;">
                        <p style="margin:0;color:#a3b0ae;font-size:12px;line-height:1.5;text-align:center;">Notificación automática de Fya Social Capital · Prueba técnica de créditos</p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """.formatted(
        LOGO_URL,
        HtmlUtils.htmlEscape(job.getClientName()),
        HtmlUtils.htmlEscape(job.getClientName()),
        amount,
        HtmlUtils.htmlEscape(job.getSalespersonName()),
        registeredAt,
        detailUrl);
  }

  private String formatAmount(BigDecimal amount) {
    NumberFormat currency = NumberFormat.getCurrencyInstance(ES_CO);
    currency.setMaximumFractionDigits(0);
    return currency.format(amount);
  }

  private String formatDate(EmailJob job) {
    if (job.getRegisteredAt() == null) {
      return "-";
    }
    return DATE_FORMAT.format(job.getRegisteredAt().atZone(BOGOTA));
  }

  private void ensureConfigured() {
    if (!StringUtils.hasText(apiKey)
        || !StringUtils.hasText(domain)
        || !StringUtils.hasText(fromEmail)) {
      throw new DependencyUnavailableException("Mailgun no está configurado");
    }
  }
}
