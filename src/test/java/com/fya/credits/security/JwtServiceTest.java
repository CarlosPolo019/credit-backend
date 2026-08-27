package com.fya.credits.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private final JwtService service = new JwtService("this-is-a-test-secret-key-with-enough-bytes-1234", 15);

  @Test
  void roundTripsSubjectAndRole() {
    JwtService.TokenResult token = service.createToken("900100001", "ADMIN");

    JwtService.TokenClaims claims = service.validate(token.token());

    assertThat(claims.subject()).isEqualTo("900100001");
    assertThat(claims.role()).isEqualTo("ADMIN");
  }

}
