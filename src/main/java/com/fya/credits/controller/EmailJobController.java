package com.fya.credits.controller;

import com.fya.credits.dto.response.EmailJobListResponse;
import com.fya.credits.service.EmailJobService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email-jobs")
public class EmailJobController {
  private final EmailJobService emailJobService;

  public EmailJobController(EmailJobService emailJobService) {
    this.emailJobService = emailJobService;
  }

  @Operation(summary = "List email notification jobs with status filters")
  @GetMapping
  public EmailJobListResponse list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String direction) {
    return emailJobService.list(status, search, sortBy, direction);
  }
}
