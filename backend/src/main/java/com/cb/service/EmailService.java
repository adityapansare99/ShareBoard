package com.cb.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private Resend resend;

    @PostConstruct
    public void init() {
        String apiKey = System.getenv("RESEND_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("RESEND_API_KEY is not configured.");
        }

        resend = new Resend(apiKey);
    }

    /**
     * Sends an OTP verification email using Resend.
     */
    public void sendOtpEmail(String recipientEmail, String otpCode) {

        log.info("=================================================");
        log.info("[REGISTER OTP] Sending Verification OTP {} to {}", otpCode, recipientEmail);
        log.info("=================================================");

        try {

            log.info("Before Resend send");

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("Planar <onboarding@resend.dev>")
                    .to(recipientEmail)
                    .subject("Planar — Your Account Verification Code")
                    .html("""
                    <h2>Welcome to Planar!</h2>
                    <p>Your OTP is: <b>%s</b></p>
                    """.formatted(otpCode))
                    .build();

            resend.emails().send(params);

            log.info("After Resend send");
            log.info("Successfully sent OTP email to {}", recipientEmail);

        } catch (Exception e) {
            log.error("Could not send email", e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}
