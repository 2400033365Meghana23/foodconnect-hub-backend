package com.foodwaste.platform.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public String hash(String value) {
        return encoder.encode(value);
    }

    public boolean matches(String value, String hash) {
        return encoder.matches(value, hash);
    }
}
