package com.fya.credits.service;

import com.fya.credits.dto.request.LoginRequest;
import com.fya.credits.dto.request.RegisterRequest;
import com.fya.credits.dto.response.LoginResponse;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.exception.ConflictException;
import com.fya.credits.model.AppUser;
import com.fya.credits.repository.UserRepository;
import com.fya.credits.security.JwtService;
import java.time.Clock;
import java.time.Instant;
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
  private final UserRepository userRepository;
  private final Clock clock;

  public AuthService(
      @Value("${app.demo-user.username}") String demoUsername,
      @Value("${app.demo-user.password-hash}") String passwordHash,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      UserRepository userRepository,
      Clock clock) {
    this.demoUsername = demoUsername;
    this.passwordHash = passwordHash;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  public LoginResponse login(LoginRequest request) {
    String document = InputNormalizer.cleanText(request.username());
    String documentNormalized = InputNormalizer.searchKey(document);
    return userRepository.findActiveByDocumentNormalized(documentNormalized)
        .map(user -> loginRegisteredUser(user, request.password()))
        .orElseGet(() -> loginDemoUser(document, request.password()));
  }

  public LoginResponse register(RegisterRequest request) {
    String fullName = InputNormalizer.cleanText(request.fullName());
    String document = InputNormalizer.cleanText(request.document());
    if (!document.matches("\\d+")) {
      throw new BadRequestException("La cédula debe ser numérica");
    }
    String documentNormalized = InputNormalizer.searchKey(document);
    if (userRepository.findByDocumentNormalized(documentNormalized).isPresent()) {
      throw new ConflictException("La cédula ya está registrada");
    }

    Instant now = clock.instant();
    AppUser user = new AppUser();
    user.setFullName(fullName);
    user.setDocument(document);
    user.setDocumentNormalized(documentNormalized);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole("USER");
    user.setIsActive(true);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    return responseFor(userRepository.save(user));
  }

  private LoginResponse loginRegisteredUser(AppUser user, String password) {
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    return responseFor(user);
  }

  private LoginResponse loginDemoUser(String username, String password) {
    if (!demoUsername.equals(username) || !demoPasswordMatches(password)) {
      throw new BadCredentialsException("Invalid credentials");
    }
    JwtService.TokenResult token = jwtService.createToken(username, "USER");
    return new LoginResponse(
        token.token(),
        "Bearer",
        token.expiresAt(),
        new LoginResponse.SessionUser(username, "Demo User", username, "USER"));
  }

  private LoginResponse responseFor(AppUser user) {
    JwtService.TokenResult token = jwtService.createToken(user.getDocumentNormalized(), user.getRole());
    return new LoginResponse(
        token.token(),
        "Bearer",
        token.expiresAt(),
        new LoginResponse.SessionUser(
            user.getDocumentNormalized(),
            user.getFullName(),
            user.getDocument(),
            user.getRole()));
  }

  private boolean demoPasswordMatches(String rawPassword) {
    if (StringUtils.hasText(passwordHash)) {
      if (passwordHash.startsWith("$2")) {
        return passwordEncoder.matches(rawPassword, passwordHash);
      }
      return passwordEncoder.matches(rawPassword, passwordEncoder.encode(passwordHash));
    }
    return passwordEncoder.matches(rawPassword, passwordEncoder.encode(DEFAULT_DEMO_PASSWORD));
  }
}
