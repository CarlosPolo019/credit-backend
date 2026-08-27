package com.fya.credits.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fya.credits.dto.request.CreateUserRequest;
import com.fya.credits.dto.response.UserResponse;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.exception.ConflictException;
import com.fya.credits.model.AppUser;
import com.fya.credits.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * This service backs `POST /api/v1/users`, which `SecurityConfig` gates
 * with `hasRole("ADMIN")` — so unlike `AuthServiceTest`, there's no
 * "caller is/isn't admin" branch to test here: by the time this method
 * runs, Spring Security has already guaranteed the caller is an admin.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Mock UserRepository userRepository;

  @Test
  void defaultsToUserRoleWhenNoneRequested() {
    UserService service = new UserService(userRepository, passwordEncoder, CLOCK);
    when(userRepository.findByDocumentNormalized("123456789")).thenReturn(Optional.empty());
    when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = service.create(new CreateUserRequest("Maria Perez", "123456789", "secret123", null));

    ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getRole()).isEqualTo("USER");
    assertThat(passwordEncoder.matches("secret123", captor.getValue().getPasswordHash())).isTrue();
    assertThat(response.role()).isEqualTo("USER");
    assertThat(response.document()).isEqualTo("123456789");
  }

  @Test
  void honorsExplicitAdminRole() {
    UserService service = new UserService(userRepository, passwordEncoder, CLOCK);
    when(userRepository.findByDocumentNormalized("123456789")).thenReturn(Optional.empty());
    when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = service.create(new CreateUserRequest("Maria Perez", "123456789", "secret123", "ADMIN"));

    ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getRole()).isEqualTo("ADMIN");
    assertThat(response.role()).isEqualTo("ADMIN");
  }

  @Test
  void rejectsDuplicateDocument() {
    UserService service = new UserService(userRepository, passwordEncoder, CLOCK);
    AppUser existing = new AppUser();
    existing.setDocumentNormalized("123456789");
    when(userRepository.findByDocumentNormalized("123456789")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.create(new CreateUserRequest("Maria Perez", "123456789", "secret123", "USER")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void rejectsNonNumericDocument() {
    UserService service = new UserService(userRepository, passwordEncoder, CLOCK);

    assertThatThrownBy(() -> service.create(new CreateUserRequest("Maria Perez", "ABC-123", "secret123", "USER")))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("La cédula debe ser numérica");
  }
}
