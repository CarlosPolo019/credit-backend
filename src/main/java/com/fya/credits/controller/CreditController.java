package com.fya.credits.controller;

import com.fya.credits.dto.request.CreateCreditRequest;
import com.fya.credits.dto.request.EstimateCreditRequest;
import com.fya.credits.dto.response.CreditAuditEntryResponse;
import com.fya.credits.dto.response.CreditListResponse;
import com.fya.credits.dto.response.CreditResponse;
import com.fya.credits.dto.response.EstimateCreditResponse;
import com.fya.credits.exception.BadRequestException;
import com.fya.credits.service.CreditPaymentEstimator;
import com.fya.credits.service.CreditPdfService;
import com.fya.credits.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/credits")
public class CreditController {
  private final CreditService creditService;
  private final CreditPdfService creditPdfService;

  public CreditController(CreditService creditService, CreditPdfService creditPdfService) {
    this.creditService = creditService;
    this.creditPdfService = creditPdfService;
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

  @Operation(summary = "Estimate the monthly installment and total payoff without saving anything")
  @PostMapping("/estimate")
  public EstimateCreditResponse estimate(@Valid @RequestBody EstimateCreditRequest request) {
    CreditPaymentEstimator.Estimate estimate =
        CreditPaymentEstimator.estimate(request.amount(), request.interestRate(), request.termMonths());
    return new EstimateCreditResponse(estimate.monthlyPayment(), estimate.totalToPay());
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

  @Operation(summary = "Update a credit's editable fields")
  @PutMapping("/{id}")
  public CreditResponse update(
      @PathVariable String id,
      @Valid @RequestBody CreateCreditRequest request,
      Authentication authentication) {
    if (authentication == null) {
      throw new BadRequestException("El usuario autenticado no está disponible");
    }
    return creditService.update(id, request, authentication.getName());
  }

  @Operation(summary = "Soft-delete a credit")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
    if (authentication == null) {
      throw new BadRequestException("El usuario autenticado no está disponible");
    }
    creditService.delete(id, authentication.getName());
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "List audit entries for a credit's edits and deletion")
  @GetMapping("/{id}/audit")
  public List<CreditAuditEntryResponse> listAudit(@PathVariable String id) {
    return creditService.listAudit(id);
  }

  @Operation(summary = "Download a credit as a branded PDF certificate")
  @GetMapping("/{id}/pdf")
  public ResponseEntity<byte[]> pdf(@PathVariable String id) {
    CreditResponse credit = creditService.getActive(id);
    byte[] pdf = creditPdfService.generate(credit);
    String filename = "credito-" + credit.id() + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .body(pdf);
  }
}
