package com.fya.credits.controller;

import com.fya.credits.dto.request.LoginRequest;
import com.fya.credits.dto.request.RegisterRequest;
import com.fya.credits.dto.response.LoginResponse;
import com.fya.credits.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "Login with document and password")
  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @Operation(summary = "Register a user with document and password")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/register")
  public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }
}
