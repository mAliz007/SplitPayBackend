package com.SplitPay.controller;

import com.SplitPay.dto.LoginRequest;
import com.SplitPay.dto.SignupRequest;
import com.SplitPay.model.User;
import com.SplitPay.service.AuthService;
import com.SplitPay.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            String result = authService.registerUser(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            // 1. Authenticate user
            User user = authService.verifyCredentials(request.getEmail(), request.getPassword());

            // 2. Generate Tokens
            String accessToken = jwtService.generateAccessToken(user.getEmail());
            String refreshToken = jwtService.generateRefreshToken(user.getEmail());

            // 3. Save Session to MongoDB (The Kill Switch)
            authService.saveSession(user.getId(), refreshToken);

            // 4. Create HttpOnly Cookies
            ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                    .httpOnly(true)
                    .secure(false) // Set to TRUE in production (HTTPS)
                    .path("/")
                    .maxAge(15 * 60) // 15 minutes
                    .sameSite("Lax")
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/api/auth/refresh") // Only sent when refreshing
                    .maxAge(7 * 24 * 60 * 60) // 7 days
                    .sameSite("Lax")
                    .build();

            // 5. Attach cookies to response
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            return ResponseEntity.ok("AUTHENTICATION_SUCCESSFUL");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token) {
        try {
            authService.verifyUser(token);
            return ResponseEntity.ok("VERIFIED");
        } catch (RuntimeException e) {
            // This hits the "TOKEN EXPIRED" UI state in your Blazor page
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. Clear Cookies by sending expired ones
        ResponseCookie deleteAccess = ResponseCookie.from("access_token", "")
                .maxAge(0).path("/").build();
        ResponseCookie deleteRefresh = ResponseCookie.from("refresh_token", "")
                .maxAge(0).path("/api/auth/refresh").build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());

        // 2. Logic to delete session from DB based on the current refresh token cookie
        // ... (Optional: find the cookie in request and delete from UserSessionRepository)

        return ResponseEntity.ok("SESSION_TERMINATED");
    }
}