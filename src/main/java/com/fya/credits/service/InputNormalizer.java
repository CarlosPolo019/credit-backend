package com.fya.credits.service;

import java.text.Normalizer;
import java.util.Locale;

public final class InputNormalizer {
  private InputNormalizer() {
  }

  public static String cleanText(String value) {
    if (value == null) {
      return "";
    }
    return value.trim().replaceAll("\\s+", " ");
  }

  public static String searchKey(String value) {
    String normalized = Normalizer.normalize(cleanText(value), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return normalized.toLowerCase(Locale.ROOT);
  }

  public static String sanitizeError(String value) {
    String cleaned = cleanText(value);
    if (cleaned.length() > 300) {
      return cleaned.substring(0, 300);
    }
    return cleaned.replaceAll("(?i)(key|token|secret|password)=\\S+", "$1=***");
  }
}
