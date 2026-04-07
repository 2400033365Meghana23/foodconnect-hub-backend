package com.foodwaste.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String jwtSecret;
    private String corsOrigin;
    private String dataFile;
    private long jwtExpirationDays = 7;
    private long otpExpiryMinutes = 10;
    private boolean devOtpMode = true;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getCorsOrigin() {
        return corsOrigin;
    }

    public void setCorsOrigin(String corsOrigin) {
        this.corsOrigin = corsOrigin;
    }

    public String getDataFile() {
        return dataFile;
    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    public long getJwtExpirationDays() {
        return jwtExpirationDays;
    }

    public void setJwtExpirationDays(long jwtExpirationDays) {
        this.jwtExpirationDays = jwtExpirationDays;
    }

    public long getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }

    public void setOtpExpiryMinutes(long otpExpiryMinutes) {
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    public boolean isDevOtpMode() {
        return devOtpMode;
    }

    public void setDevOtpMode(boolean devOtpMode) {
        this.devOtpMode = devOtpMode;
    }
}
