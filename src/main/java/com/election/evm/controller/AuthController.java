package com.election.evm.controller;

import com.election.evm.dto.ApiResponse;
import com.election.evm.dto.AuthResponse;
import com.election.evm.dto.AuthRequest;
import com.election.evm.dto.LoginRequest;
import com.election.evm.dto.OtpSendRequest;
import com.election.evm.dto.OtpVerifyRequest;
import com.election.evm.entity.User;
import com.election.evm.service.EvmService;
import com.election.evm.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller
 * Handles user registration, login, token refresh, and OAuth2 callbacks
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AuthController {
    private final EvmService service;
    private final OtpService otpService;

    public AuthController(EvmService service, OtpService otpService) {
        this.service = service;
        this.otpService = otpService;
    }

    /**
     * Register a new user
     * @param request - AuthRequest containing name, email, password, role
     * @return ApiResponse with registered user
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody AuthRequest request) {
        return service.register(request);
    }

    @PostMapping("/otp/send")
    public ApiResponse<Void> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        return otpService.sendOtp(request.email());
    }

    @PostMapping("/otp/verify")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return otpService.verifyOtp(request.email(), request.otp());
    }

    /**
     * Login user with email and password
     * @param request - LoginRequest containing email and password
     * @return ApiResponse with JWT token and user data
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    /**
     * Refresh JWT token before expiry
     * @param authHeader - Bearer token in Authorization header
     * @return ApiResponse with new JWT token
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.failure("Invalid authorization header.");
        }

        String token = authHeader.substring(7);
        return service.refreshToken(token);
    }

    /**
     * Get current authenticated user profile
     * @return ApiResponse with current user details
     */
    @GetMapping("/me")
    public ApiResponse<User> me() {
        return service.getCurrentUser();
    }

    /**
     * OAuth2 login success callback
     * Called after successful OAuth2 authentication
     * @return ApiResponse with JWT token and user data
     */
    @GetMapping("/oauth-success")
    public ApiResponse<AuthResponse> oauthSuccess() {
        return service.handleOAuthSuccess();
    }
}
