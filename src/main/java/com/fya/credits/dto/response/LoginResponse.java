package com.fya.credits.dto.response;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt, DemoUser user) {
  public record DemoUser(String username, String role) {
  }
}
