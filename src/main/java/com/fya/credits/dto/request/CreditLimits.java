package com.fya.credits.dto.request;

/**
 * Shared bounds for credit terms, matching what a real consumer-credit
 * product would offer (monthly rate on a personal loan, term capped like a
 * bank caps unsecured lending) — used by both {@link CreateCreditRequest}
 * and {@link EstimateCreditRequest} so create and estimate never disagree.
 */
public final class CreditLimits {
  public static final String MAX_AMOUNT = "200000000";
  public static final String MIN_INTEREST_RATE = "0.5";
  public static final String MAX_INTEREST_RATE = "3.5";
  public static final int MIN_TERM_MONTHS = 1;
  public static final int MAX_TERM_MONTHS = 60;

  private CreditLimits() {
  }
}
