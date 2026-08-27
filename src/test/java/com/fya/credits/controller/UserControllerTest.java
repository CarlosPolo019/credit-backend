package com.fya.credits.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fya.credits.config.SecurityConfig;
import com.fya.credits.dto.response.UserResponse;
import com.fya.credits.security.JwtAuthenticationFilter;
import com.fya.credits.security.JwtService;
import com.fya.credits.security.RateLimitFilter;
import com.fya.credits.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP-level test for the most security-sensitive endpoint in the API:
 * POST /api/v1/users requires an authenticated ADMIN (SecurityConfig
 * .hasRole("ADMIN")), same pattern as /api/v1/email-jobs/**. This is the
 * clearest place to prove role enforcement end-to-end through the real
 * filter chain — a USER token must never reach UserService.create, and no
 * token must never reach it either.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RateLimitFilter.class, JwtService.class})
@TestPropertySource(properties = {
    "app.rate-limit.login-per-minute=1000",
    "app.rate-limit.create-credit-per-minute=1000",
    "app.rate-limit.list-credit-per-minute=1000",
})
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;

  @MockitoBean private UserService userService;

  private String bearerToken(String subject, String role) {
    return "Bearer " + jwtService.createToken(subject, role).token();
  }

  private String validCreateUserJson() {
    return """
        {"fullName":"Maria Perez","document":"123456789","password":"secret123"}
        """;
  }

  @Test
  void create_withoutToken_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validCreateUserJson()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void create_withUserRoleToken_returns403_neverReachesService() throws Exception {
    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", bearerToken("900100002", "USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validCreateUserJson()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("FORBIDDEN")));
  }

  @Test
  void create_withAdminRoleToken_returns201WithNoTokenInResponse() throws Exception {
    when(userService.create(any())).thenReturn(new UserResponse("123456789", "Maria Perez", "USER"));

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", bearerToken("900100001", "ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(validCreateUserJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.document", is("123456789")))
        .andExpect(jsonPath("$.role", is("USER")))
        // UserResponse has no "token" field at all — this is an admin
        // action, not a login, so it must never carry a session.
        .andExpect(jsonPath("$.token").doesNotExist());
  }
}
