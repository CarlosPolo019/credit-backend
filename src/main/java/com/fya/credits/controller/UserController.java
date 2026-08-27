package com.fya.credits.controller;

import com.fya.credits.dto.request.CreateUserRequest;
import com.fya.credits.dto.response.UserResponse;
import com.fya.credits.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(summary = "Admin-only: create a user account (role defaults to USER, ADMIN allowed). "
      + "Requires a Bearer token from an authenticated ADMIN (SecurityConfig hasRole(\"ADMIN\"), "
      + "same pattern as /api/v1/email-jobs/**). Does not log the created account in.")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
    return userService.create(request);
  }
}
