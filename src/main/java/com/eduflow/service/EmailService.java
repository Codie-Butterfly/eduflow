package com.eduflow.service;

public interface EmailService {

    /**
     * Generates a secure random password
     */
    String generateSecurePassword();

    /**
     * Sends welcome email with login credentials
     */
    void sendWelcomeEmail(String toEmail, String name, String role, String password);

    /**
     * Sends welcome email asynchronously (non-blocking)
     */
    void sendWelcomeEmailAsync(String toEmail, String name, String role, String password);

    /**
     * Sends password reset email with reset link
     */
    void sendPasswordResetEmail(String toEmail, String name, String resetToken);

    /**
     * Sends password reset email asynchronously (non-blocking)
     */
    void sendPasswordResetEmailAsync(String toEmail, String name, String resetToken);
}