package com.fya.credits.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fya.credits.config.SecurityConfig;
import com.fya.credits.dto.response.CreditListResponse;
import com.fya.credits.dto.response.CreditResponse;
import com.fya.credits.exception.NotFoundException;
import com.fya.credits.security.JwtAuthenticationFilter;
import com.fya.credits.security.JwtService;
import com.fya.credits.security.RateLimitFilter;
import com.fya.credits.service.CreditPdfService;
import com.fya.credits.service.CreditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP-level test for CreditController: real Spring MVC dispatch, real
 * SecurityConfig/JwtAuthenticationFilter chain (not mocked), real bean
 * validation and GlobalExceptionHandler — only CreditService/CreditPdfService
 * are mocked, so this exercises exactly the layer the service-level tests
 * (CreditServiceTest) don't: status codes, JSON shape, and that a request
 * without/with the right role/token actually gets past (or rejected by)
 * Spring Security, not just that the service method does the right thing in
 * isolation.
 *
 * @WebMvcTest only boots the web layer — it does NOT instantiate
 * FirebaseConfig's Firestore bean (which would try to reach real GCP
 * credentials and fail outside a configured environment), unlike a full
 * @SpringBootTest would.
 */
@WebMvcTest(CreditController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RateLimitFilter.class, JwtService.class})
@TestPropertySource(properties = {
    "app.rate-limit.login-per-minute=1000",
    "app.rate-limit.create-credit-per-minute=1000",
    "app.rate-limit.list-credit-per-minute=1000",
})
class CreditControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;

  @MockitoBean private CreditService creditService;
  @MockitoBean private CreditPdfService creditPdfService;

  private String bearerToken(String subject, String role) {
    return "Bearer " + jwtService.createToken(subject, role).token();
  }

  @Test
  void create_withoutToken_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/credits")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validCreateCreditJson()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void create_withValidTokenAndBody_returns201WithLocationAndBody() throws Exception {
    CreditResponse response = sampleCreditResponse();
    when(creditService.create(any(), eq("900100001"))).thenReturn(response);

    mockMvc.perform(post("/api/v1/credits")
            .header("Authorization", bearerToken("900100001", "USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validCreateCreditJson()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/credits/CR-1"))
        .andExpect(jsonPath("$.id", is("CR-1")))
        .andExpect(jsonPath("$.clientDocument", is("100000001")))
        .andExpect(jsonPath("$.amount", is(7800000)));

    verify(creditService).create(any(), eq("900100001"));
  }

  @Test
  void create_withInvalidBody_returns400WithValidationError() throws Exception {
    // amount <= 0 violates @DecimalMin(inclusive=false) on CreateCreditRequest.
    String invalidJson = """
        {
          "clientFirstName": "Pepito",
          "clientFirstSurname": "Perez",
          "clientDocument": "100000001",
          "amount": 0,
          "interestRate": 2,
          "termMonths": 10
        }
        """;

    mockMvc.perform(post("/api/v1/credits")
            .header("Authorization", bearerToken("900100001", "USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
        .andExpect(jsonPath("$.errors.amount").exists());
  }

  @Test
  void list_withValidToken_returns200WithItems() throws Exception {
    when(creditService.list(any(), any(), any(), any(), any()))
        .thenReturn(new CreditListResponse(List.of(sampleCreditResponse()), 1));

    mockMvc.perform(get("/api/v1/credits")
            .header("Authorization", bearerToken("900100001", "USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total", is(1)))
        .andExpect(jsonPath("$.items[0].id", is("CR-1")));
  }

  @Test
  void get_creditNotFound_returns404() throws Exception {
    when(creditService.getActive("missing")).thenThrow(new NotFoundException("Crédito no disponible"));

    mockMvc.perform(get("/api/v1/credits/missing")
            .header("Authorization", bearerToken("900100001", "USER")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("NOT_FOUND")));
  }

  private String validCreateCreditJson() {
    return """
        {
          "clientFirstName": "Pepito",
          "clientFirstSurname": "Perez",
          "clientDocument": "100000001",
          "amount": 7800000,
          "interestRate": 2,
          "termMonths": 10
        }
        """;
  }

  private CreditResponse sampleCreditResponse() {
    Instant now = Instant.parse("2026-08-25T20:00:00Z");
    return new CreditResponse(
        "CR-1",
        "Pepito",
        "",
        "Perez",
        "",
        "Pepito Perez",
        "100000001",
        new BigDecimal("7800000"),
        new BigDecimal("2"),
        10,
        "900100001",
        "900100001",
        "Carlos Escorcia",
        true,
        now,
        now,
        null,
        new BigDecimal("868347"),
        new BigDecimal("8683469"));
  }
}
