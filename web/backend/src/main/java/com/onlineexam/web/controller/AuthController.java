package com.onlineexam.web.controller;

import com.onlineexam.model.User;
import com.onlineexam.service.AuthService;
import com.onlineexam.web.dto.AuthDto.*;
import com.onlineexam.web.security.JwtTokenProvider;
import com.onlineexam.web.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final String adminInvitationCode;

    @Autowired
    public AuthController(
            AuthService authService,
            JwtTokenProvider tokenProvider,
            @Value("${admin.invitation-code:ADMIN-EXAM-SECURE-2026}") String adminInvitationCode
    ) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
        this.adminInvitationCode = adminInvitationCode;
    }

    public AuthController(AuthService authService, JwtTokenProvider tokenProvider) {
        this(authService, tokenProvider, "ADMIN-EXAM-SECURE-2026");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String loginUsername = request.getEffectiveUsername();
        if (loginUsername.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", "Username or email is required."));
        }
        Optional<User> userOpt = authService.authenticate(loginUsername, request.password());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Credentials", "message", "Username or password is incorrect."));
        }

        User user = userOpt.get();
        String token = tokenProvider.generateToken(user);
        UserSummaryDto summary = new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(new AuthResponse(token, summary));
    }

    @PostMapping(value = {"/register", "/register/student"})
    public ResponseEntity<?> registerStudent(@Valid @RequestBody RegisterRequest request) {
        try {
            // Public candidate registration strictly enforces STUDENT role
            User registered = authService.registerStudent(
                    request.username(),
                    request.password(),
                    request.fullName(),
                    request.email()
            );

            String token = tokenProvider.generateToken(registered);
            UserSummaryDto summary = new UserSummaryDto(
                    registered.getId(),
                    registered.getUsername(),
                    registered.getFullName(),
                    registered.getEmail(),
                    registered.getRole().name()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, summary));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration Failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/register/admin")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        if (request.invitationCode() == null || !request.invitationCode().trim().equals(adminInvitationCode.trim())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid Invitation Code", "message", "The admin invitation code provided is invalid or has expired."));
        }

        try {
            // Admin registration creates administrator with ADMIN role
            User registered = authService.registerAdmin(
                    request.username(),
                    request.password(),
                    request.fullName(),
                    request.email()
            );

            String token = tokenProvider.generateToken(registered);
            UserSummaryDto summary = new UserSummaryDto(
                    registered.getId(),
                    registered.getUsername(),
                    registered.getFullName(),
                    registered.getEmail(),
                    registered.getRole().name()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, summary));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration Failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<User> userOpt = authService.getUserById(principal.id());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User u = userOpt.get();
        return ResponseEntity.ok(new UserSummaryDto(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole().name()));
    }
}
