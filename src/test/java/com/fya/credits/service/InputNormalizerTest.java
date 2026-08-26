package com.fya.credits.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InputNormalizerTest {
  @Test
  void cleansTextAndSearchKeys() {
    assertThat(InputNormalizer.cleanText("  Giselle   López  ")).isEqualTo("Giselle López");
    assertThat(InputNormalizer.searchKey("  Giselle   López  ")).isEqualTo("giselle lopez");
  }

  @Test
  void sanitizesSecretLikeErrors() {
    assertThat(InputNormalizer.sanitizeError("failed token=abc123"))
        .isEqualTo("failed token=***");
  }
}
