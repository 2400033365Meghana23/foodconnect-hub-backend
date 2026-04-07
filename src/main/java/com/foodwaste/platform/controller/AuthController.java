package com.foodwaste.platform.controller;

import com.foodwaste.platform.config.AppProperties;
import com.foodwaste.platform.exception.ApiException;
import com.foodwaste.platform.model.UserEntity;
import com.foodwaste.platform.model.SignupOtpEntity;
import com.foodwaste.platform.repository.SignupOtpRepository;
import com.foodwaste.platform.repository.UserRepository;
import com.foodwaste.platform.security.CurrentUser;
import com.foodwaste.platform.service.JwtService;
import com.foodwaste.platform.service.MailService;
import com.foodwaste.platform.service.PasswordService;
import com.foodwaste.platform.util.ValidationUtils;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.mail.MailException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final SignupOtpRepository signupOtpRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    public AuthController(
        UserRepository userRepository,
        SignupOtpRepository signupOtpRepository,
        PasswordService passwordService,
        JwtService jwtService,
        MailService mailService,
        AppProperties properties
    ) {
        this.userRepository = userRepository;
        this.signupOtpRepository = signupOtpRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.properties = properties;
    }

    @PostMapping("/request-signup-otp")
    public Map<String, Object> requestSignupOtp(@RequestBody Map<String, Object> body) {
        String email = ValidationUtils.requireEmail(body.get("email"));
        if (userRepository.findByEmail(email) != null) {
            throw new ApiException(409, "Email already registered");
        }

        String otpCode = String.valueOf(100000 + random.nextInt(900000));
        signupOtpRepository.save(email, otpCode, Instant.now().plus(properties.getOtpExpiryMinutes(), ChronoUnit.MINUTES));
        try {
            mailService.sendOtp(email, otpCode, properties.getOtpExpiryMinutes());
            return Map.of("message", "OTP generated successfully", "expiresInMinutes", properties.getOtpExpiryMinutes());
        } catch (MailException ex) {
            if (!properties.isDevOtpMode()) {
                throw ex;
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "OTP generated in development mode");
            response.put("expiresInMinutes", properties.getOtpExpiryMinutes());
            response.put("otp", otpCode);
            response.put("delivery", "dev-fallback");
            return response;
        }
    }

    @PostMapping("/signup-with-otp")
    public Map<String, Object> signupWithOtp(@RequestBody Map<String, Object> body) {
        String email = ValidationUtils.requireEmail(body.get("email"));
        if (userRepository.findByEmail(email) != null) {
            throw new ApiException(409, "Email already registered");
        }

        SignupOtpEntity otp = signupOtpRepository.findByEmail(email);
        if (otp == null) {
            throw new ApiException(400, "OTP not requested for this email");
        }
        if (otp.expiresAt.isBefore(Instant.now())) {
            signupOtpRepository.deleteByEmail(email);
            throw new ApiException(400, "OTP expired. Please request a new OTP");
        }
        if (otp.attempts >= 5) {
            signupOtpRepository.deleteByEmail(email);
            throw new ApiException(429, "Too many invalid OTP attempts. Request a new OTP");
        }

        String submittedOtp = ValidationUtils.requireOtp(body.get("otp"));
        if (!submittedOtp.equals(otp.otpCode)) {
            signupOtpRepository.incrementAttempts(email);
            throw new ApiException(400, "Invalid OTP");
        }

        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID().toString();
        user.email = email;
        user.passwordHash = passwordService.hash(ValidationUtils.requirePassword(body.get("password")));
        user.name = defaultName(body.get("name"), email);
        user.role = ValidationUtils.requireRole(body.get("role"));
        user.status = "active";
        user.phone = blank(body.get("phone"));
        user.location = blank(body.get("location"));
        user.createdAt = Instant.now();
        user.updatedAt = user.createdAt;

        UserEntity created = userRepository.create(user);
        signupOtpRepository.deleteByEmail(email);

        return authResponse(created);
    }

    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody Map<String, Object> body) {
        ValidationUtils.requireEmail(body.get("email"));
        ValidationUtils.requirePassword(body.get("password"));
        ValidationUtils.requireRole(body.get("role"));
        throw new ApiException(400, "OTP verification required. Use /auth/request-signup-otp and /auth/signup-with-otp");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String email = ValidationUtils.requireEmail(body.get("email"));
        String password = ValidationUtils.requireString(body.get("password"), "Password is required", 1, 255);
        String role = ValidationUtils.optionalString(body.get("role"));

        UserEntity user = userRepository.findByEmail(email);
        if (user == null || !passwordService.matches(password, user.passwordHash)) {
            throw new ApiException(401, "Invalid credentials");
        }
        if (role != null && !role.isBlank() && !role.equals(user.role)) {
            throw new ApiException(403, "Role mismatch for this account");
        }
        return authResponse(user);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal CurrentUser currentUser) {
        UserEntity user = userRepository.findById(currentUser.getId());
        if (user == null) {
            throw new ApiException(401, "Invalid token user");
        }
        return Map.of("user", user.toPublicMap());
    }

    private Map<String, Object> authResponse(UserEntity user) {
        CurrentUser currentUser = new CurrentUser(user.id, user.email, user.role, user.name);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", jwtService.generateToken(currentUser));
        response.put("user", user.toPublicMap());
        return response;
    }

    private String defaultName(Object name, String email) {
        String value = ValidationUtils.optionalString(name);
        return value == null || value.isBlank() ? email.substring(0, email.indexOf('@')) : value;
    }

    private String blank(Object value) {
        String text = ValidationUtils.optionalString(value);
        return text == null || text.isBlank() ? "" : text;
    }
}
