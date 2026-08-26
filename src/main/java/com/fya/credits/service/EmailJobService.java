package com.fya.credits.service;

import com.fya.credits.dto.response.EmailJobListResponse;
import com.fya.credits.dto.response.EmailJobResponse;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.model.EmailJobStatus;
import com.fya.credits.repository.EmailJobQuery;
import com.fya.credits.repository.EmailJobRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmailJobService {
  private final EmailJobRepository emailJobRepository;

  public EmailJobService(EmailJobRepository emailJobRepository) {
    this.emailJobRepository = emailJobRepository;
  }

  public EmailJobListResponse list(String status, String search, String sortBy, String direction) {
    String resolvedStatus = resolveStatus(status);
    String resolvedSortBy = resolveSortBy(sortBy);
    String resolvedDirection = resolveDirection(direction);
    List<EmailJobResponse> items = emailJobRepository
        .listAll(new EmailJobQuery(resolvedStatus, search, resolvedSortBy, resolvedDirection))
        .stream()
        .map(EmailJobResponse::from)
        .toList();
    return new EmailJobListResponse(items, items.size());
  }

  private String resolveStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    try {
      return EmailJobStatus.valueOf(status).name();
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("status no válido");
    }
  }

  private String resolveSortBy(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return "createdAt";
    }
    if (!sortBy.equals("createdAt") && !sortBy.equals("status")) {
      throw new BadRequestException("sortBy no permitido");
    }
    return sortBy;
  }

  private String resolveDirection(String direction) {
    if (direction == null || direction.isBlank()) {
      return "desc";
    }
    if (!direction.equals("asc") && !direction.equals("desc")) {
      throw new BadRequestException("direction no permitida");
    }
    return direction;
  }
}
