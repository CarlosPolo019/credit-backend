package com.fya.credits.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fya.credits.config.SecurityConfig;
import com.fya.credits.dto.request.LoginRequest;
import com.fya.credits.dto.response.LoginResponse;
import com.fya.credits.security.JwtAuthenticationFilter;
import com.fya.credits.security.JwtService;
import com.fya.credits.security.RateLimitFilter;
import com.fya.credits.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP-level test for AuthController — POST /api/v1/auth/login is the one
 * route in the whole API that must stay reachable with no token at all
 * (permitAll in SecurityConfig); this confirms that's actually true through
 * the real filter chain, plus that a bad-credentials failure surfaces as a
 * 401 with the shape GlobalExceptionHandler promises, not a 500.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RateLimitFilter.class, JwtService.class})
@TestPropertySource(properties = {
    "app.rate-limit.login-per-minute=1000",
    "app.rate-limit.create-credit-per-minute=1000",
    "app.rate-limit.list-credit-per-minute=1000",
})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;

  @Test
  void login_withValidCredentials_returns200WithTokenAndUser_noAuthHeaderNeeded() throws Exception {
    Instant expiresAt = Instant.parse("2026-08-25T21:00:00Z");
    when(authService.login(new LoginRequest("900100001", "demo12345")))
        .thenReturn(new LoginResponse(
            "jwt-token",
            "Bearer",
            expiresAt,
            new LoginResponse.SessionUser("900100001", "Carlos Escorcia", "900100001", "ADMIN")));

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"900100001","password":"demo12345"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", is("jwt-token")))
        .andExpect(jsonPath("$.user.role", is("ADMIN")));
  }

  @Test
  void login_withBadCredentials_returns401NotAStackTrace() throws Exception {
    when(authService.login(new LoginRequest("900100001", "wrong")))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"900100001","password":"wrong"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
  }

  @Test
  void login_withBlankUsername_returns400ValidationError() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"","password":"demo12345"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
        .andExpect(jsonPath("$.errors.username").exists());
  }
}
