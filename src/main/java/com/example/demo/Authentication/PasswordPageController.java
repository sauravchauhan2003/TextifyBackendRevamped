package com.example.demo.Authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class PasswordPageController {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam String token,
            Model model
    ) {
        PasswordResetToken resetToken =
                tokenRepository.findByToken(token).orElse(null);

        if (resetToken == null ||
                resetToken.isUsed() ||
                resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {

            return "reset-password-invalid"; // error page
        }

        model.addAttribute("token", token);
        return "reset-password"; // reset-password.html
    }
}
