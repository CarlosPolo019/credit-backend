package com.fya.credits.service;

import com.fya.credits.dto.request.CreateUserRequest;
import com.fya.credits.dto.response.UserResponse;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.exception.ConflictException;
import com.fya.credits.model.AppUser;
import com.fya.credits.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Admin-only account creation — backs `credit-web`'s `/users` page, which
 * creates comercial test accounts (or, if truly needed, another `ADMIN`).
 * `POST /api/v1/users` is gated at the `SecurityConfig` level
 * (`hasRole("ADMIN")`), the same pattern already used for
 * `/api/v1/email-jobs/**` — so only a caller already authenticated as
 * `ADMIN` ever reaches this method; there's no role-branching logic here.
 *
 * This is deliberately separate from `AuthService.register`: that endpoint
 * is public self-registration (always `"USER"`, no auth required) and
 * returns a full login session for the newly created account because it's
 * meant to log the caller in as themselves. This one is an admin acting on
 * someone else's behalf — it never issues a token for the created account,
 * so there's no session to accidentally hand back to the caller.
 */
@Service
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  public UserResponse create(CreateUserRequest request) {
    String fullName = InputNormalizer.cleanText(request.fullName());
    String document = InputNormalizer.cleanText(request.document());
    if (!document.matches("\\d+")) {
      throw new BadRequestException("La cédula debe ser numérica");
    }
    String documentNormalized = InputNormalizer.searchKey(document);
    if (userRepository.findByDocumentNormalized(documentNormalized).isPresent()) {
      throw new ConflictException("La cédula ya está registrada");
    }

    String role = StringUtils.hasText(request.role()) ? request.role() : "USER";

    Instant now = clock.instant();
    AppUser user = new AppUser();
    user.setFullName(fullName);
    user.setDocument(document);
    user.setDocumentNormalized(documentNormalized);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(role);
    user.setIsActive(true);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    AppUser saved = userRepository.save(user);
    return new UserResponse(saved.getDocument(), saved.getFullName(), saved.getRole());
  }
}
