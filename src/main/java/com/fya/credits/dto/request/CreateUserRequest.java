package com.fya.credits.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for `POST /api/v1/users` (admin-only, see `UserController`/`UserService`).
 * `role` is optional and defaults to `"USER"` in the service when absent —
 * this endpoint itself is only reachable by an already-authenticated
 * `ADMIN` (`SecurityConfig` gates it with `hasRole("ADMIN")`, same pattern
 * as `/api/v1/email-jobs/**`), so honoring `"ADMIN"` here is safe: nobody
 * who isn't already an admin can reach this at all.
 */
public record CreateUserRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Pattern(regexp = "\\d+", message = "La cédula debe ser numérica") @Size(max = 20) String document,
    @NotBlank @Size(min = 8, max = 200) String password,
    @Pattern(regexp = "ADMIN|USER", message = "El rol debe ser ADMIN o USER") String role) {
}
