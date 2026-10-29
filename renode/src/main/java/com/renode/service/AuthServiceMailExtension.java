package com.renode.service;

import com.renode.entity.Company;
import com.renode.entity.VerificationToken;
import com.renode.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceMailExtension {

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private EmailDispatcherService emailDispatcherService;

    @Value("${renode.app.base-url:http://localhost:8080}")
    private String baseUrl;

    public void sendRegistrationVerificationEmail(Company company) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setCompanyId(company.getId());
        verificationToken.setToken(token);
        verificationToken.setTokenType("EMAIL_VERIFICATION");
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        verificationToken.setIsUsed(false);
        tokenRepository.save(verificationToken);

        String verifyLink = baseUrl + "/api/auth/verify?token=" + token;
        String htmlBody = "<div style='font-family:Inter,sans-serif;background:#070b14;color:#f8fafc;padding:30px;border-radius:12px;'>" +
                "<h2 style='color:#38bdf8;'>Welcome to ReNode, " + company.getCompanyName() + "!</h2>" +
                "<p>Please verify your email address to unlock full marketplace access.</p>" +
                "<a href='" + verifyLink + "' style='background:#10b981;color:white;padding:12px 20px;text-decoration:none;border-radius:6px;display:inline-block;margin-top:15px;font-weight:600;'>Verify Email Address</a>" +
                "</div>";

        emailDispatcherService.queueEmail(
                company.getEmail(),
                "Verify Your ReNode Account",
                htmlBody,
                "Please verify your email using this link: " + verifyLink
        );
    }

    public void sendPasswordResetEmail(Company company) {
        String token = UUID.randomUUID().toString();
        VerificationToken resetToken = new VerificationToken();
        resetToken.setCompanyId(company.getId());
        resetToken.setToken(token);
        resetToken.setTokenType("PASSWORD_RESET");
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30)); // 30 min expiry
        resetToken.setIsUsed(false);
        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/reset-password.html?token=" + token;
        String htmlBody = "<div style='font-family:Inter,sans-serif;background:#070b14;color:#f8fafc;padding:30px;border-radius:12px;'>" +
                "<h2 style='color:#ef4444;'>Password Reset Request</h2>" +
                "<p>We received a request to reset your ReNode password. This link expires in 30 minutes.</p>" +
                "<a href='" + resetLink + "' style='background:#38bdf8;color:#070b14;padding:12px 20px;text-decoration:none;border-radius:6px;display:inline-block;margin-top:15px;font-weight:600;'>Reset Password</a>" +
                "<p style='margin-top:20px;font-size:12px;color:#94a3b8;'>If you did not request this, please ignore this email.</p>" +
                "</div>";

        emailDispatcherService.queueEmail(
                company.getEmail(),
                "Password Reset - ReNode",
                htmlBody,
                "Reset your password using this link: " + resetLink
        );
    }
}