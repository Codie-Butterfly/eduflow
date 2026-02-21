package com.eduflow.service.impl;

import com.eduflow.dto.request.*;
import com.eduflow.dto.response.AuthResponse;
import com.eduflow.dto.response.MessageResponse;
import com.eduflow.entity.academic.Parent;
import com.eduflow.entity.academic.Student;
import com.eduflow.entity.academic.Teacher;
import com.eduflow.entity.user.Role;
import com.eduflow.entity.user.User;
import com.eduflow.exception.BadRequestException;
import com.eduflow.exception.DuplicateResourceException;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.exception.UnauthorizedException;
import com.eduflow.repository.academic.ParentRepository;
import com.eduflow.repository.academic.StudentRepository;
import com.eduflow.repository.academic.TeacherRepository;
import com.eduflow.repository.user.RoleRepository;
import com.eduflow.repository.user.UserRepository;
import com.eduflow.security.jwt.JwtTokenProvider;
import com.eduflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        Role.RoleName roleName;
        try {
            roleName = Role.RoleName.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + request.getRole());
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .enabled(true)
                .build();

        user.addRole(role);
        user = userRepository.save(user);

        // Create corresponding entity based on role
        createRoleSpecificEntity(user, roleName);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        log.info("User registered successfully: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String email = tokenProvider.getUsernameFromToken(request.getRefreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!request.getRefreshToken().equals(user.getRefreshToken())) {
            throw new UnauthorizedException("Refresh token mismatch");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + role.getName().name()))
                        .collect(Collectors.toList())
        );

        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public MessageResponse logout(String refreshToken) {
        if (refreshToken != null && tokenProvider.validateToken(refreshToken)) {
            String email = tokenProvider.getUsernameFromToken(refreshToken);
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setRefreshToken(null);
                userRepository.save(user);
            });
        }
        SecurityContextHolder.clearContext();
        return MessageResponse.success("Logged out successfully");
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user != null) {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            userRepository.save(user);
            // TODO: Send email with reset link
            log.info("Password reset token generated for user: {}", user.getEmail());
        }

        // Always return success to prevent email enumeration
        return MessageResponse.success("If the email exists, a password reset link will be sent");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setRefreshToken(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getEmail());
        return MessageResponse.success("Password reset successfully");
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.getFullName())
                        .roles(user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toSet()))
                        .build())
                .build();
    }

    private void createRoleSpecificEntity(User user, Role.RoleName roleName) {
        switch (roleName) {
            case TEACHER -> {
                String employeeId = generateEmployeeId();
                Teacher teacher = Teacher.builder()
                        .employeeId(employeeId)
                        .user(user)
                        .dateOfJoining(LocalDate.now())
                        .build();
                teacherRepository.save(teacher);
                log.info("Teacher profile created for user: {} with employeeId: {}", user.getEmail(), employeeId);
            }
            case STUDENT -> {
                String studentId = generateStudentId();
                Student student = Student.builder()
                        .studentId(studentId)
                        .user(user)
                        .enrollmentDate(LocalDate.now())
                        .status(Student.StudentStatus.ACTIVE)
                        .build();
                studentRepository.save(student);
                log.info("Student profile created for user: {} with studentId: {}", user.getEmail(), studentId);
            }
            case PARENT -> {
                Parent parent = Parent.builder()
                        .user(user)
                        .build();
                parentRepository.save(parent);
                log.info("Parent profile created for user: {}", user.getEmail());
            }
            default -> {
                // ADMIN doesn't need a separate entity
            }
        }
    }

    private String generateEmployeeId() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "TCH" + year;
        Integer maxNum = teacherRepository.findMaxEmployeeIdNumber(prefix);
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        return prefix + String.format("%04d", nextNum);
    }

    private String generateStudentId() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "STU" + year;
        Integer maxNum = studentRepository.findMaxStudentIdNumber(prefix);
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        return prefix + String.format("%04d", nextNum);
    }
}
