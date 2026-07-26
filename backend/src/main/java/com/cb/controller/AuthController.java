package com.cb.controller;

import com.cb.config.UserContext;
import com.cb.dto.LoginRequest;
import com.cb.dto.RegisterRequest;
import com.cb.security.AuthService;
import com.cb.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.cb.repository.UserRepository;
import com.cb.service.EmailService;
import com.cb.service.OtpService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserContext userContext;
    private final EmailService emailService;
    private final OtpService otpService;
    private final UserRepository userRepository;

    /**
     * Spring Security 6 defaults requireExplicitSave=true, so setting the
     * SecurityContext on the current request does NOT persist it to the
     * session. We must save explicitly via the repository.
     */
    private final SecurityContextRepository securityContextRepository
            = new HttpSessionSecurityContextRepository();

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required"));
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email " + normalizedEmail + " is already registered"));
        }

        String otp = otpService.generateOtp(normalizedEmail);

        try {
            emailService.sendOtpEmail(normalizedEmail, otp);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Verification OTP sent to " + normalizedEmail
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Email could not be sent"
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody @Valid RegisterRequest req) {
        if (req.otp() != null && !req.otp().trim().isEmpty()) {
            authService.register(req.email(), req.name(), req.password(), req.otp());
        } else {
            authService.register(req.email(), req.name(), req.password());
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody @Valid LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken
                .unauthenticated(req.email().trim().toLowerCase(), req.password());
        // BadCredentialsException -> mapped to 401 by AuthExceptionHandler
        Authentication auth = authenticationManager.authenticate(token);

        request.getSession(true);          // guarantee a session exists
        request.changeSessionId();         // session-fixation protection
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        securityContextRepository.saveContext(ctx, request, response); // <-- makes it stick

        UserPrincipal up = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "email", up.getEmail(),
                "name", up.getName()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        if (!userContext.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "email", userContext.getCurrentUserEmail(),
                "name", userContext.getCurrentUserName()
        ));
    }
}
