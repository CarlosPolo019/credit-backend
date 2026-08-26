package com.fya.credits.dto.response;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt, SessionUser user) {
  public record SessionUser(String username, String fullName, String document, String role) {
  }
}
