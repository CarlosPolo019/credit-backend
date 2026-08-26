package com.fya.credits.dto.response;

import java.util.List;

public record EmailJobListResponse(List<EmailJobResponse> items, int total) {
}
