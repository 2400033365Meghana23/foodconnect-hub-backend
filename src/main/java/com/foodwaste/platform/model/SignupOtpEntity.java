package com.foodwaste.platform.model;

import java.time.Instant;

public class SignupOtpEntity {

    public Long id;
    public String email;
    public String otpCode;
    public Instant expiresAt;
    public int attempts;
}
