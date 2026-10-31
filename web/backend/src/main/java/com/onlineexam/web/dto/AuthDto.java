package com.onlineexam.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record LoginRequest(
            String username,
            String usernameOrEmail,
            @NotBlank(message = "Password is required") String password
    ) {
        public LoginRequest(String username, String password) {
            this(username, username, password);
        }

        public String getEffectiveUsername() {
            if (username != null && !username.isBlank()) return username.trim();
            if (usernameOrEmail != null && !usernameOrEmail.isBlank()) return usernameOrEmail.trim();
            return "";
        }
    }

    public record RegisterRequest(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
            String username,

            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String password,

            @NotBlank(message = "Full name is required")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Valid email address is required")
            String email
    ) {}

    public record AdminRegisterRequest(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
            String username,

            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String password,

            @NotBlank(message = "Full name is required")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Valid email address is required")
            String email,

            @NotBlank(message = "Admin invitation code is required")
            String invitationCode
    ) {}

    public record AuthResponse(
            String token,
            UserSummaryDto user
    ) {}

    public record UserSummaryDto(
            Long id,
            String username,
            String fullName,
            String email,
            String role
    ) {}
}
