package com.foodwaste.platform.util;

import com.foodwaste.platform.exception.ApiException;
import java.util.Set;
import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern OTP = Pattern.compile("^\\d{6}$");
    private static final Set<String> ROLES = Set.of("admin", "donor", "recipient", "analyst");

    private ValidationUtils() {
    }

    public static String requireEmail(Object value) {
        String email = requireString(value, "Email is required", 3, 255).trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new ApiException(400, "Invalid email address");
        }
        return email;
    }

    public static String requirePassword(Object value) {
        String password = requireString(value, "Password is required", 8, 255);
        if (!password.matches(".*[A-Z].*")) {
            throw new ApiException(400, "Password must include uppercase");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ApiException(400, "Password must include lowercase");
        }
        if (!password.matches(".*\\d.*")) {
            throw new ApiException(400, "Password must include number");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new ApiException(400, "Password must include special character");
        }
        return password;
    }

    public static String requireRole(Object value) {
        String role = requireString(value, "Role is required", 2, 20);
        if (!ROLES.contains(role)) {
            throw new ApiException(400, "Invalid role");
        }
        return role;
    }

    public static String requireOtp(Object value) {
        String otp = requireString(value, "OTP is required", 6, 6);
        if (!OTP.matcher(otp).matches()) {
            throw new ApiException(400, "OTP must be 6 digits");
        }
        return otp;
    }

    public static String requireString(Object value, String message, int min, int max) {
        String text = optionalString(value);
        if (text == null || text.length() < min) {
            throw new ApiException(400, message);
        }
        if (text.length() > max) {
            throw new ApiException(400, "Invalid payload");
        }
        return text;
    }

    public static String optionalString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    public static Double requirePositiveDouble(Object value, String field) {
        Double number = optionalDouble(value);
        if (number == null || number <= 0) {
            throw new ApiException(400, field + " must be a positive number");
        }
        return number;
    }

    public static Double optionalDouble(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "Invalid payload");
        }
    }

    public static Integer optionalInteger(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "Invalid payload");
        }
    }
}
