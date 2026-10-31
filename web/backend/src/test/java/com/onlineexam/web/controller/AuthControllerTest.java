package com.onlineexam.web.controller;

import com.onlineexam.dao.UserDAO;
import com.onlineexam.model.User;
import com.onlineexam.service.AuthService;
import com.onlineexam.web.dto.AuthDto.AdminRegisterRequest;
import com.onlineexam.web.dto.AuthDto.AuthResponse;
import com.onlineexam.web.dto.AuthDto.LoginRequest;
import com.onlineexam.web.dto.AuthDto.RegisterRequest;
import com.onlineexam.web.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {

    private AuthController authController;
    private AuthService authService;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        UserDAO userDAO = new UserDAO();
        authService = new AuthService(userDAO);
        tokenProvider = new JwtTokenProvider(
                "OnlineExamSecretKeyForJwtSigningMustBeAtLeast256BitsLongForHmacSha256!",
                86400000L
        );
        authController = new AuthController(authService, tokenProvider);
    }

    @Test
    @DisplayName("Valid admin credentials authenticate and return JWT token")
    void testValidAdminLogin() {
        LoginRequest request = new LoginRequest("admin", "admin123");
        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Valid student credentials authenticate and return JWT token")
    void testValidStudentLogin() {
        LoginRequest request = new LoginRequest("student1", "student123");
        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Invalid password returns 401 Unauthorized")
    void testInvalidPasswordLogin() {
        LoginRequest request = new LoginRequest("student1", "wrongpassword");
        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Non-existent user returns 401 Unauthorized")
    void testNonExistentUserLogin() {
        LoginRequest request = new LoginRequest("nonexistent_user_xyz", "somepassword");
        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Student registration strictly assigns STUDENT role")
    void testStudentRegistrationAssignsStudentRole() {
        String unique = "student_" + System.currentTimeMillis();
        RegisterRequest request = new RegisterRequest(unique, "Secret123!", "Test Student", unique + "@example.com");
        ResponseEntity<?> response = authController.registerStudent(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody() instanceof AuthResponse);
        AuthResponse authRes = (AuthResponse) response.getBody();
        assertNotNull(authRes.token());
        assertEquals("STUDENT", authRes.user().role());
        assertEquals(unique, authRes.user().username());
    }

    @Test
    @DisplayName("Admin registration with valid invitation code assigns ADMIN role")
    void testAdminRegistrationWithValidCode() {
        String unique = "admin_" + System.currentTimeMillis();
        AdminRegisterRequest request = new AdminRegisterRequest(
                unique,
                "AdminSecret123!",
                "Test Admin",
                unique + "@example.com",
                "ADMIN-EXAM-SECURE-2026"
        );
        ResponseEntity<?> response = authController.registerAdmin(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody() instanceof AuthResponse);
        AuthResponse authRes = (AuthResponse) response.getBody();
        assertNotNull(authRes.token());
        assertEquals("ADMIN", authRes.user().role());
        assertEquals(unique, authRes.user().username());
    }

    @Test
    @DisplayName("Admin registration with invalid invitation code is rejected with 403 Forbidden")
    void testAdminRegistrationWithInvalidCode() {
        String unique = "fakeadmin_" + System.currentTimeMillis();
        AdminRegisterRequest request = new AdminRegisterRequest(
                unique,
                "AdminSecret123!",
                "Fake Admin",
                unique + "@example.com",
                "WRONG-CODE"
        );
        ResponseEntity<?> response = authController.registerAdmin(request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Duplicate registration by username or email fails with 400 Bad Request")
    void testDuplicateRegistrationFails() {
        String unique = "dupuser_" + System.currentTimeMillis();
        RegisterRequest first = new RegisterRequest(unique, "Secret123!", "First User", unique + "@example.com");
        ResponseEntity<?> firstResp = authController.registerStudent(first);
        assertEquals(HttpStatus.CREATED, firstResp.getStatusCode());

        // Duplicate username
        RegisterRequest duplicateUser = new RegisterRequest(unique, "Secret123!", "Another Name", "different@example.com");
        ResponseEntity<?> dupUserResp = authController.registerStudent(duplicateUser);
        assertEquals(HttpStatus.BAD_REQUEST, dupUserResp.getStatusCode());

        // Duplicate email
        RegisterRequest duplicateEmail = new RegisterRequest("diff_" + unique, "Secret123!", "Another Name", unique + "@example.com");
        ResponseEntity<?> dupEmailResp = authController.registerStudent(duplicateEmail);
        assertEquals(HttpStatus.BAD_REQUEST, dupEmailResp.getStatusCode());
    }
}
