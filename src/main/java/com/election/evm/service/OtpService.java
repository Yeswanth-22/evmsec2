package com.election.evm.service;

import com.election.evm.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private static final int MAX_ATTEMPTS = 5;

    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${app.otp.ttl-ms:300000}")
    private long otpTtlMs;

    @Value("${app.otp.resend-cooldown-ms:60000}")
    private long resendCooldownMs;

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public ApiResponse<Void> sendOtp(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (email.isBlank()) {
            return ApiResponse.failure("Email is required.");
        }

        Instant now = Instant.now();
        OtpEntry existing = otpStore.get(email);
        if (existing != null && now.isBefore(existing.resendAllowedAt())) {
            long waitSeconds = existing.resendAllowedAt().getEpochSecond() - now.getEpochSecond();
            return ApiResponse.failure("Please wait " + Math.max(waitSeconds, 1) + " seconds before requesting a new OTP.");
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = now.plusMillis(otpTtlMs);
        Instant resendAllowedAt = now.plusMillis(resendCooldownMs);
        otpStore.put(email, new OtpEntry(otp, expiresAt, resendAllowedAt, MAX_ATTEMPTS, false));

        try {
            sendMail(email, otp, expiresAt);
            return ApiResponse.success("OTP sent to your email.");
        } catch (MailException ex) {
            otpStore.remove(email);
            return ApiResponse.failure("Unable to send OTP email. Check mail configuration.");
        }
    }

    public ApiResponse<Void> verifyOtp(String rawEmail, String rawOtp) {
        String email = normalizeEmail(rawEmail);
        String otp = rawOtp == null ? "" : rawOtp.trim();

        OtpEntry entry = otpStore.get(email);
        if (entry == null) {
            return ApiResponse.failure("No OTP request found for this email.");
        }

        Instant now = Instant.now();
        if (now.isAfter(entry.expiresAt())) {
            otpStore.remove(email);
            return ApiResponse.failure("OTP expired. Please request a new one.");
        }

        if (entry.attemptsLeft() <= 0) {
            otpStore.remove(email);
            return ApiResponse.failure("Too many invalid attempts. Request a new OTP.");
        }

        if (!entry.code().equals(otp)) {
            otpStore.put(email, entry.withAttemptsLeft(entry.attemptsLeft() - 1));
            return ApiResponse.failure("Invalid OTP.");
        }

        otpStore.put(email, entry.markVerified());
        return ApiResponse.success("OTP verified successfully.");
    }

    public boolean isVerified(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        OtpEntry entry = otpStore.get(email);
        if (entry == null) {
            return false;
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            otpStore.remove(email);
            return false;
        }

        return entry.verified();
    }

    public void clear(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        otpStore.remove(email);
    }

    private void sendMail(String toEmail, String otp, Instant expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress.trim());
        }
        message.setTo(toEmail);
        message.setSubject("Your EVM verification code");
        message.setText("Your OTP is " + otp + ". It expires at " + expiresAt + " UTC.");
        mailSender.send(message);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record OtpEntry(
            String code,
            Instant expiresAt,
            Instant resendAllowedAt,
            int attemptsLeft,
            boolean verified
    ) {
        private OtpEntry withAttemptsLeft(int updatedAttempts) {
            return new OtpEntry(code, expiresAt, resendAllowedAt, updatedAttempts, verified);
        }

        private OtpEntry markVerified() {
            return new OtpEntry(code, expiresAt, resendAllowedAt, attemptsLeft, true);
        }
    }
}
