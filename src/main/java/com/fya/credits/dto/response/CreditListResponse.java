package com.fya.credits.dto.response;

import java.util.List;

public record CreditListResponse(List<CreditResponse> items, int total) {
}
