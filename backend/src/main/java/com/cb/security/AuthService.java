package com.cb.security;

import com.cb.model.User;
import com.cb.repository.UserRepository;
import com.cb.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Transactional
    public User register(String email, String name, String password, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        if (!otpService.validateOtp(normalizedEmail, otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP code.");
        }

        User user = User.builder()
            .email(normalizedEmail)
            .name(name.trim())
            .passwordHash(passwordEncoder.encode(password))
            .build();
        return userRepository.save(user);
    }

    @Transactional
    public User register(String email, String name, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        User user = User.builder()
            .email(normalizedEmail)
            .name(name.trim())
            .passwordHash(passwordEncoder.encode(password))
            .build();
        return userRepository.save(user);
    }
}
