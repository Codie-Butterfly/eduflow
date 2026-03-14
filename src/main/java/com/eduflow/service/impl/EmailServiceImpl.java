package com.eduflow.service.impl;

import com.eduflow.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:https://eduflow.com}")
    private String frontendUrl;

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
    private static final int PASSWORD_LENGTH = 12;

    @Override
    public String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String name, String role, String password) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to EduFlow - Your Account Credentials");

            String htmlContent = buildWelcomeEmailHtml(name, toEmail, password, role);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    @Override
    @Async
    public void sendWelcomeEmailAsync(String toEmail, String name, String role, String password) {
        try {
            sendWelcomeEmail(toEmail, name, role, password);
        } catch (Exception e) {
            log.error("Async welcome email failed for {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildWelcomeEmailHtml(String name, String email, String password, String role) {
        String loginUrl = frontendUrl + "/auth/login";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: #f5f5f5;
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #3f51b5 0%%, #1a237e 100%%);
                        padding: 40px 30px;
                        text-align: center;
                    }
                    .logo {
                        font-size: 32px;
                        font-weight: bold;
                        color: white;
                        letter-spacing: 2px;
                    }
                    .logo-icon {
                        font-size: 40px;
                        margin-bottom: 10px;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    h2 {
                        color: #1a237e;
                        margin-top: 0;
                        font-size: 24px;
                    }
                    p {
                        color: #555;
                        line-height: 1.6;
                        margin: 15px 0;
                    }
                    .credentials {
                        background: #f8f9fa;
                        padding: 25px;
                        border-radius: 8px;
                        margin: 25px 0;
                        border-left: 4px solid #3f51b5;
                    }
                    .credential-row {
                        margin: 15px 0;
                    }
                    .label {
                        color: #888;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 5px;
                    }
                    .value {
                        font-size: 18px;
                        font-weight: 600;
                        color: #333;
                        font-family: 'Courier New', monospace;
                        background: white;
                        padding: 8px 12px;
                        border-radius: 4px;
                        display: inline-block;
                    }
                    .role-badge {
                        display: inline-block;
                        background: #e8eaf6;
                        color: #3f51b5;
                        padding: 4px 12px;
                        border-radius: 20px;
                        font-size: 14px;
                        font-weight: 500;
                    }
                    .button {
                        display: inline-block;
                        background: linear-gradient(135deg, #3f51b5 0%%, #1a237e 100%%);
                        color: white;
                        padding: 14px 40px;
                        text-decoration: none;
                        border-radius: 6px;
                        font-weight: 600;
                        margin-top: 20px;
                        text-align: center;
                    }
                    .button:hover {
                        opacity: 0.9;
                    }
                    .warning {
                        background: #fff3e0;
                        border-left: 4px solid #ff9800;
                        padding: 15px;
                        border-radius: 4px;
                        margin: 25px 0;
                    }
                    .warning strong {
                        color: #e65100;
                    }
                    .footer {
                        padding: 25px 30px;
                        text-align: center;
                        color: #999;
                        font-size: 12px;
                        border-top: 1px solid #eee;
                        background: #fafafa;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo-icon">🎓</div>
                        <div class="logo">EduFlow</div>
                    </div>
                    <div class="content">
                        <h2>Welcome to EduFlow!</h2>
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Your <span class="role-badge">%s</span> account has been created successfully. Below are your login credentials:</p>

                        <div class="credentials">
                            <div class="credential-row">
                                <div class="label">Username (Email)</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="credential-row">
                                <div class="label">Password</div>
                                <div class="value">%s</div>
                            </div>
                        </div>

                        <div class="warning">
                            <strong>Important:</strong> For security purposes, please change your password immediately after your first login.
                        </div>

                        <center>
                            <a href="%s" class="button">Login to EduFlow</a>
                        </center>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from EduFlow. Please do not reply to this email.</p>
                        <p>&copy; 2024 EduFlow. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, role, email, password, loginUrl);
    }
}