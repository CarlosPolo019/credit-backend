package com.fya.credits.service;

import com.fya.credits.dto.request.LoginRequest;
import com.fya.credits.dto.response.LoginResponse;
import com.fya.credits.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
  private static final String DEFAULT_DEMO_PASSWORD = "demo12345";
  private final String demoUsername;
  private final String passwordHash;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      @Value("${app.demo-user.username}") String demoUsername,
      @Value("${app.demo-user.password-hash}") String passwordHash,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.demoUsername = demoUsername;
    this.passwordHash = passwordHash;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public LoginResponse login(LoginRequest request) {
    String username = InputNormalizer.cleanText(request.username());
    if (!demoUsername.equals(username) || !passwordMatches(request.password())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    JwtService.TokenResult token = jwtService.createToken(username);
    return new LoginResponse(
        token.token(),
        "Bearer",
        token.expiresAt(),
        new LoginResponse.DemoUser(username, "USER"));
  }

  private boolean passwordMatches(String rawPassword) {
    if (StringUtils.hasText(passwordHash)) {
      if (passwordHash.startsWith("$2")) {
        return passwordEncoder.matches(rawPassword, passwordHash);
      }
      return passwordEncoder.matches(rawPassword, passwordEncoder.encode(passwordHash));
    }
    return passwordEncoder.matches(rawPassword, passwordEncoder.encode(DEFAULT_DEMO_PASSWORD));
  }
}
