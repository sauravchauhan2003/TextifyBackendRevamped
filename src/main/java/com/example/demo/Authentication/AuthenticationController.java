package com.example.demo.Authentication;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private UserRepository repository;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    // =========================
    // REGISTER (EMAIL)
    // =========================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (repository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        UserModel user = new UserModel();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider(AuthProvider.LOCAL);

        repository.save(user);

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token, false));
    }

    // =========================
    // LOGIN (EMAIL)
    // =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        UserModel user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            return ResponseEntity.badRequest().body("Use Google login");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Wrong password");
        }

        String token = jwtService.generateToken(user);
        boolean hasPublicKey = user.getPublic_key() != null;

        return ResponseEntity.ok(new LoginResponse(token, hasPublicKey));
    }

    // =========================
    // GOOGLE OAUTH
    // =========================
    @PostMapping("/oauth/google")
    public ResponseEntity<?> googleOAuth(@RequestBody GoogleOAuthRequest request) {

        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            return ResponseEntity.badRequest().body("idToken is missing");
        }

        GoogleIdToken.Payload payload =
                googleOAuthService.verify(request.getIdToken());

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        Optional<UserModel> existing = repository.findByEmail(email);
        UserModel user;

        if (existing.isPresent()) {

            user = existing.get();

            if (user.getProvider() != AuthProvider.GOOGLE) {
                return ResponseEntity.badRequest()
                        .body("Account exists with password login");
            }

        } else {

            user = new UserModel();
            user.setEmail(email);
            user.setUsername(name);
            user.setProvider(AuthProvider.GOOGLE);
            user.setPassword(passwordEncoder.encode("GOOGLE_USER"));

            repository.save(user);
        }

        String token = jwtService.generateToken(user);
        boolean hasPublicKey = user.getPublic_key() != null;

        return ResponseEntity.ok(new LoginResponse(token, hasPublicKey));
    }

    // =========================
    // UPLOAD PUBLIC KEY (JWT REQUIRED)
    // =========================
    @PostMapping("/public-key")
    public ResponseEntity<?> uploadPublicKey(
            @RequestBody PublicKeyRequest request,
            Authentication authentication
    ) {

        UserModel user = (UserModel) authentication.getPrincipal();

        if (request.getPublicKey() == null || request.getPublicKey().isBlank()) {
            return ResponseEntity.badRequest().body("Public key is required");
        }

        user.setPublic_key(request.getPublicKey());
        repository.save(user);

        return ResponseEntity.ok("Public key uploaded successfully");
    }

    // =========================
    // FORGOT PASSWORD
    // =========================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {

        Optional<UserModel> userOpt = repository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.ok("If email exists, reset link sent");
        }

        UserModel user = userOpt.get();

        if (user.getProvider() != AuthProvider.LOCAL) {
            return ResponseEntity.badRequest()
                    .body("Password reset not allowed for Google login");
        }

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryTime(LocalDateTime.now().plusMinutes(30));

        passwordResetTokenRepository.save(resetToken);

        String resetLink = "https://localhost:8000/reset-password?token=" + token;

        emailService.send(
                user.getEmail(),
                "Reset your password",
                "Click the link to reset password:\n" + resetLink
        );

        return ResponseEntity.ok("If email exists, reset link sent");
    }

    // =========================
    // RESET PASSWORD
    // =========================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(request.getToken())
                        .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.isUsed()) {
            return ResponseEntity.badRequest().body("Token already used");
        }

        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token expired");
        }

        UserModel user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        repository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return ResponseEntity.ok("Password reset successful");
    }
}

