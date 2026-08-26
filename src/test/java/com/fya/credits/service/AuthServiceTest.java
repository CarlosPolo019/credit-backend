package com.fya.credits.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  private static final Instant EXPIRES_AT = Instant.parse("2026-08-25T21:00:00Z");
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);

  @Mock UserRepository userRepository;
  @Mock JwtService jwtService;

  @Test
  void registersUserWithDocumentIdentity() {
    AuthService service = service("");
    when(userRepository.findByDocumentNormalized("123456789")).thenReturn(Optional.empty());
    when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.createToken("123456789")).thenReturn(new JwtService.TokenResult("jwt", EXPIRES_AT));

    LoginResponse response = service.register(new RegisterRequest(
        "  Maria   Perez  ",
        " 123456789 ",
        "secret123"));

    ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
    verify(userRepository).save(userCaptor.capture());

    assertThat(userCaptor.getValue().getFullName()).isEqualTo("Maria Perez");
    assertThat(userCaptor.getValue().getDocument()).isEqualTo("123456789");
    assertThat(userCaptor.getValue().getDocumentNormalized()).isEqualTo("123456789");
    assertThat(passwordEncoder.matches("secret123", userCaptor.getValue().getPasswordHash())).isTrue();
    assertThat(response.token()).isEqualTo("jwt");
    assertThat(response.user().document()).isEqualTo("123456789");
  }

  @Test
  void rejectsDuplicateDocument() {
    AuthService service = service("");
    when(userRepository.findByDocumentNormalized("123456789")).thenReturn(Optional.of(user("123456789", "secret123")));

    assertThatThrownBy(() -> service.register(new RegisterRequest(
        "Maria Perez",
        "123456789",
        "secret123")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void rejectsNonNumericRegisterDocument() {
    AuthService service = service("");

    assertThatThrownBy(() -> service.register(new RegisterRequest(
        "Maria Perez",
        "ABC-123",
        "secret123")))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("La cédula debe ser numérica");
  }

  @Test
  void logsInRegisteredUserByDocument() {
    AuthService service = service("");
    when(userRepository.findActiveByDocumentNormalized("123456789"))
        .thenReturn(Optional.of(user("123456789", "secret123")));
    when(jwtService.createToken("123456789")).thenReturn(new JwtService.TokenResult("jwt", EXPIRES_AT));

    LoginResponse response = service.login(new LoginRequest("123456789", "secret123"));

    assertThat(response.token()).isEqualTo("jwt");
    assertThat(response.user().username()).isEqualTo("123456789");
    assertThat(response.user().fullName()).isEqualTo("Maria Perez");
  }

  @Test
  void rejectsInvalidRegisteredPassword() {
    AuthService service = service("");
    when(userRepository.findActiveByDocumentNormalized("123456789"))
        .thenReturn(Optional.of(user("123456789", "secret123")));

    assertThatThrownBy(() -> service.login(new LoginRequest("123456789", "wrong-password")))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void keepsDemoUserFallback() {
    AuthService service = service("");
    when(userRepository.findActiveByDocumentNormalized("demo")).thenReturn(Optional.empty());
    when(jwtService.createToken("demo")).thenReturn(new JwtService.TokenResult("demo-jwt", EXPIRES_AT));

    LoginResponse response = service.login(new LoginRequest("demo", "demo12345"));

    assertThat(response.token()).isEqualTo("demo-jwt");
    assertThat(response.user().username()).isEqualTo("demo");
  }

  private AuthService service(String demoPasswordHash) {
    return new AuthService("demo", demoPasswordHash, passwordEncoder, jwtService, userRepository, clock);
  }

  private AppUser user(String document, String password) {
    AppUser user = new AppUser();
    user.setId(document);
    user.setFullName("Maria Perez");
    user.setDocument(document);
    user.setDocumentNormalized(document);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole("USER");
    user.setIsActive(true);
    user.setCreatedAt(clock.instant());
    user.setUpdatedAt(clock.instant());
    return user;
  }
}
