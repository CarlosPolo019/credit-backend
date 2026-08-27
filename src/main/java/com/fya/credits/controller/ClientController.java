package com.fya.credits.controller;

import com.fya.credits.dto.response.ClientResponse;
import com.fya.credits.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
  private final ClientService clientService;

  public ClientController(ClientService clientService) {
    this.clientService = clientService;
  }

  @Operation(summary = "List all clients (document + name), used by the credit form's autocomplete")
  @GetMapping
  public List<ClientResponse> list() {
    return clientService.list();
  }
}
