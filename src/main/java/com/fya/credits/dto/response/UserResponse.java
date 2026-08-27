package com.fya.credits.dto.response;

/**
 * Response for `POST /api/v1/users` — deliberately NOT a `LoginResponse`
 * (no token, no session): this is an admin creating someone else's
 * account, not that account logging in. See `UserService.create`.
 */
public record UserResponse(String document, String fullName, String role) {
}
