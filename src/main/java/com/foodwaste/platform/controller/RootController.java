package com.foodwaste.platform.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
            "ok", true,
            "service", "food-waste-platform-backend",
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "ok", true,
            "message", "Food Waste Platform backend is running",
            "health", "/api/health",
            "apiBase", "/api"
        );
    }
}
