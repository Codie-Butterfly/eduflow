package com.eduflow.service;

import com.eduflow.dto.request.*;
import com.eduflow.dto.response.AuthResponse;
import com.eduflow.dto.response.MessageResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    MessageResponse logout(String refreshToken);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);
}
