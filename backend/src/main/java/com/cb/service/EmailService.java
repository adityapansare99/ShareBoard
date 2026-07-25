package com.cb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    /**
     * Sends an OTP verification email to the user.
     * Logs the OTP to the console/logger as well for dev mode/fallback.
     */
    public void sendOtpEmail(String recipientEmail, String otpCode) {
        log.info("=================================================");
        log.info("[REGISTER OTP] Sending Verification OTP {} to {}", otpCode, recipientEmail);
        log.info("=================================================");

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipientEmail);
                message.setSubject("Planar — Your Account Verification Code");
                message.setText("Welcome to Planar!\n\nYour 6-digit email verification code is: " + otpCode +
                                 "\n\nThis code will expire in 5 minutes. If you did not request this code, please ignore this email.");
                mailSender.send(message);
                log.info("Successfully sent OTP email to {}", recipientEmail);
            } catch (Exception e) {
                log.warn("Could not send email via JavaMailSender: {}. Fallback OTP in log: {}", e.getMessage(), otpCode);
            }
        } else {
            log.info("JavaMailSender not configured. Relying on logger OTP for dev mode: {}", otpCode);
        }
    }
}
