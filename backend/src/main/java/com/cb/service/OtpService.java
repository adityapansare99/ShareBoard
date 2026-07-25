package com.cb.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final long OTP_VALIDITY_SECONDS = 300; // 5 minutes
    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    private record OtpEntry(String code, Instant expiresAt) {}

    /**
     * Generates a 6-digit numeric OTP for the given email address.
     */
    public String generateOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        int num = 100000 + random.nextInt(900000);
        String code = String.valueOf(num);
        Instant expiresAt = Instant.now().plusSeconds(OTP_VALIDITY_SECONDS);
        otpStorage.put(normalizedEmail, new OtpEntry(code, expiresAt));
        return code;
    }

    public boolean validateOtp(String email, String inputCode) {
        if (email == null || inputCode == null) return false;
        String normalizedEmail = email.trim().toLowerCase();
        OtpEntry entry = otpStorage.get(normalizedEmail);

        if (entry == null) {
            return false;
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            otpStorage.remove(normalizedEmail);
            return false;
        }

        if (entry.code().equals(inputCode.trim())) {
            otpStorage.remove(normalizedEmail);
            return true;
        }

        return false;
    }
}
