package com.fya.credits.controller;

import com.fya.credits.dto.request.CreateCreditRequest;
import com.fya.credits.dto.response.CreditListResponse;
import com.fya.credits.dto.response.CreditResponse;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/credits")
public class CreditController {
  private final CreditService creditService;

  public CreditController(CreditService creditService) {
    this.creditService = creditService;
  }

  @Operation(summary = "Register a credit and queue its email notification")
  @PostMapping
  public ResponseEntity<CreditResponse> create(
      @Valid @RequestBody CreateCreditRequest request,
      Authentication authentication) {
    if (authentication == null) {
      throw new BadRequestException("El usuario autenticado no está disponible");
    }
    CreditResponse response = creditService.create(request, authentication.getName());
    return ResponseEntity.created(URI.create("/api/v1/credits/" + response.id())).body(response);
  }

  @Operation(summary = "List active credits with filters and controlled sorting")
  @GetMapping
  public CreditListResponse list(
      @RequestParam(required = false) String clientName,
      @RequestParam(required = false) String clientDocument,
      @RequestParam(required = false) String salesperson,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String direction) {
    return creditService.list(clientName, clientDocument, salesperson, sortBy, direction);
  }

  @Operation(summary = "Get one active credit")
  @GetMapping("/{id}")
  public CreditResponse get(@PathVariable String id) {
    return creditService.getActive(id);
  }

  @Operation(summary = "Soft-delete a credit")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    creditService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
