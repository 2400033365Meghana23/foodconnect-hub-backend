package com.foodwaste.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodwaste.platform.exception.ApiException;
import com.foodwaste.platform.model.DataState;
import com.foodwaste.platform.model.Donation;
import com.foodwaste.platform.model.RequestRecord;
import com.foodwaste.platform.model.UserEntity;
import com.foodwaste.platform.repository.UserRepository;
import com.foodwaste.platform.security.CurrentUser;
import com.foodwaste.platform.service.AuthorizationService;
import com.foodwaste.platform.service.JsonDatabaseService;
import com.foodwaste.platform.util.ValidationUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ManagementController {

    private static final List<String> CONTENT_TYPES = List.of("pages", "faqs", "announcements");
    private static final List<String> SETTINGS_SECTIONS = List.of("general", "notifications", "donations", "security");

    private final JsonDatabaseService jsonDatabaseService;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ManagementController(
        JsonDatabaseService jsonDatabaseService,
        AuthorizationService authorizationService,
        UserRepository userRepository,
        ObjectMapper objectMapper
    ) {
        this.jsonDatabaseService = jsonDatabaseService;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/analytics/overview")
    public Map<String, Object> analyticsOverview(@AuthenticationPrincipal CurrentUser currentUser) {
        authorizationService.requireRoles(currentUser, "analyst", "admin");
        DataState state = jsonDatabaseService.readState();

        int totalDonations = state.donations.size();
        int totalRequests = state.requests.size();
        long completedDonations = state.donations.stream().filter(item -> "completed".equals(item.status)).count();
        long activeDonations = state.donations.stream().filter(item -> !"completed".equals(item.status)).count();
        double totalQuantity = state.donations.stream().mapToDouble(item -> item.quantity == null ? 0d : item.quantity).sum();

        return Map.of(
            "totalDonations", totalDonations,
            "totalRequests", totalRequests,
            "completedDonations", completedDonations,
            "activeDonations", activeDonations,
            "totalQuantity", totalQuantity,
            "mealsProvided", completedDonations * 15,
            "foodSavedKg", Math.round(totalQuantity)
        );
    }

    @GetMapping("/analytics/categories")
    public Map<String, Object> analyticsCategories(@AuthenticationPrincipal CurrentUser currentUser) {
        authorizationService.requireRoles(currentUser, "analyst", "admin");
        DataState state = jsonDatabaseService.readState();

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Donation donation : state.donations) {
            counts.merge(donation.category, 1, Integer::sum);
        }
        List<Map<String, Object>> categories = counts.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", entry.getKey());
                item.put("value", entry.getValue());
                return item;
            })
            .toList();
        return Map.of("categories", categories);
    }

    @GetMapping("/analytics/monthly")
    public Map<String, Object> analyticsMonthly(@AuthenticationPrincipal CurrentUser currentUser) {
        authorizationService.requireRoles(currentUser, "analyst", "admin");
        DataState state = jsonDatabaseService.readState();

        Map<String, Map<String, Object>> monthly = new LinkedHashMap<>();
        for (Donation donation : state.donations) {
            LocalDate date = LocalDate.parse((donation.date == null || donation.date.isBlank()) ? donation.createdAt.substring(0, 10) : donation.date);
            String key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            monthly.putIfAbsent(key, new LinkedHashMap<>(Map.of("month", key, "donations", 0, "quantity", 0d)));
            Map<String, Object> metrics = monthly.get(key);
            metrics.put("donations", ((Integer) metrics.get("donations")) + 1);
            metrics.put("quantity", ((Double) metrics.get("quantity")) + (donation.quantity == null ? 0d : donation.quantity));
        }
        return Map.of("monthly", new ArrayList<>(monthly.values()));
    }

    @GetMapping("/analytics/top-donors")
    public Map<String, Object> topDonors(@AuthenticationPrincipal CurrentUser currentUser) {
        authorizationService.requireRoles(currentUser, "analyst", "admin");
        DataState state = jsonDatabaseService.readState();

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Donation donation : state.donations) {
            counts.merge(donation.donorId, 1, Integer::sum);
        }
        List<Map<String, Object>> result = counts.entrySet().stream()
            .map(entry -> {
                UserEntity donor = state.users.stream().filter(item -> item.id.equals(entry.getKey())).findFirst().orElse(null);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", entry.getKey());
                item.put("name", donor == null ? "Donor" : donor.name);
                item.put("donations", entry.getValue());
                return item;
            })
            .sorted((a, b) -> Integer.compare((Integer) b.get("donations"), (Integer) a.get("donations")))
            .limit(10)
            .toList();
        return Map.of("topDonors", result);
    }

    @GetMapping("/analytics/top-recipients")
    public Map<String, Object> topRecipients(@AuthenticationPrincipal CurrentUser currentUser) {
        authorizationService.requireRoles(currentUser, "analyst", "admin");
        DataState state = jsonDatabaseService.readState();

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RequestRecord request : state.requests) {
            if ("completed".equals(request.status)) {
                counts.merge(request.recipientId, 1, Integer::sum);
            }
        }
        List<Map<String, Object>> result = counts.entrySet().stream()
            .map(entry -> {
                UserEntity recipient = state.users.stream().filter(item -> item.id.equals(entry.getKey())).findFirst().orElse(null);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", entry.getKey());
                item.put("name", recipient == null ? "Recipient" : recipient.name);
                item.put("received", entry.getValue());
                return item;
            })
            .sorted((a, b) -> Integer.compare((Integer) b.get("received"), (Integer) a.get("received")))
            .limit(10)
            .toList();
        return Map.of("topRecipients", result);
    }

    @GetMapping("/content/{type}")
    public Map<String, Object> content(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String type) {
        authorizationService.requireRoles(currentUser, "admin");
        validateContentType(type);
        DataState state = jsonDatabaseService.readState();
        return Map.of("items", state.content.get(type));
    }

    @PostMapping("/content/{type}")
    public Map<String, Object> createContent(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String type,
        @RequestBody Map<String, Object> body
    ) {
        authorizationService.requireRoles(currentUser, "admin");
        validateContentType(type);

        Map<String, Object> item = new LinkedHashMap<>(body);
        item.put("id", UUID.randomUUID().toString());
        item.put("createdAt", Instant.now().toString());
        item.put("updatedAt", Instant.now().toString());

        jsonDatabaseService.mutate(state -> {
            state.content.get(type).add(0, item);
            return state;
        });
        return Map.of("item", item);
    }

    @PatchMapping("/content/{type}/{id}")
    public Map<String, Object> updateContent(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String type,
        @PathVariable String id,
        @RequestBody Map<String, Object> body
    ) {
        authorizationService.requireRoles(currentUser, "admin");
        validateContentType(type);

        DataState updated = jsonDatabaseService.mutate(state -> {
            Map<String, Object> item = state.content.get(type).stream()
                .filter(entry -> id.equals(entry.get("id")))
                .findFirst()
                .orElse(null);
            if (item == null) {
                throw new ApiException(404, "Content item not found");
            }
            item.putAll(body);
            item.put("updatedAt", Instant.now().toString());
            return state;
        });

        Map<String, Object> item = updated.content.get(type).stream()
            .filter(entry -> id.equals(entry.get("id")))
            .findFirst()
            .orElse(null);
        return Map.of("item", item);
    }

    @DeleteMapping("/content/{type}/{id}")
    public ResponseEntity<Void> deleteContent(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String type,
        @PathVariable String id
    ) {
        authorizationService.requireRoles(currentUser, "admin");
        validateContentType(type);
        jsonDatabaseService.mutate(state -> {
            state.content.get(type).removeIf(item -> id.equals(item.get("id")));
            return state;
        });
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public Map<String, Object> settings(@AuthenticationPrincipal CurrentUser currentUser) {
        authorizationService.requireRoles(currentUser, "admin");
        return Map.of("settings", jsonDatabaseService.readState().settings);
    }

    @PutMapping("/settings/{section}")
    public Map<String, Object> updateSettings(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String section,
        @RequestBody Map<String, Object> body
    ) {
        authorizationService.requireRoles(currentUser, "admin");
        if (!SETTINGS_SECTIONS.contains(section)) {
            throw new ApiException(400, "Invalid settings section");
        }

        DataState updated = jsonDatabaseService.mutate(state -> {
            state.settings.get(section).putAll(body);
            return state;
        });
        return Map.of("section", section, "settings", updated.settings.get(section));
    }

    @GetMapping("/admin/backup")
    public ResponseEntity<String> backup(@AuthenticationPrincipal CurrentUser currentUser) throws Exception {
        authorizationService.requireRoles(currentUser, "admin");
        String body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonDatabaseService.readState());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("food-waste-backup-" + System.currentTimeMillis() + ".json").build().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body);
    }

    @PostMapping("/admin/restore")
    public Map<String, Object> restore(@AuthenticationPrincipal CurrentUser currentUser, @RequestBody DataState state) {
        authorizationService.requireRoles(currentUser, "admin");
        if (state == null || state.users == null || state.donations == null || state.requests == null || state.content == null || state.settings == null) {
            throw new ApiException(400, "Missing backup keys: users, donations, requests, content, settings");
        }
        jsonDatabaseService.replaceAll(state);
        return Map.of("message", "Backup restored successfully");
    }

    @GetMapping("/admin/dashboard")
    public Map<String, Object> dashboard(@AuthenticationPrincipal CurrentUser currentUser) {
        authorizationService.requireRoles(currentUser, "admin");
        DataState state = jsonDatabaseService.readState();

        long activeUsers = state.users.stream().filter(item -> !"inactive".equals(item.status)).count();
        long totalDonations = state.donations.size();
        long activeDonations = state.donations.stream().filter(item -> !"completed".equals(item.status)).count();
        long completedDonations = state.donations.stream().filter(item -> "completed".equals(item.status)).count();
        long pendingRequests = state.requests.stream().filter(item -> "requested".equals(item.status)).count();

        return Map.of(
            "activeUsers", activeUsers,
            "totalDonations", totalDonations,
            "activeDonations", activeDonations,
            "completedDonations", completedDonations,
            "pendingRequests", pendingRequests
        );
    }

    @GetMapping("/users")
    public Map<String, Object> users(
        @AuthenticationPrincipal CurrentUser currentUser,
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "all") String role,
        @RequestParam(defaultValue = "all") String status
    ) {
        authorizationService.requireRoles(currentUser, "admin");
        String term = q.toLowerCase();
        List<Map<String, Object>> users = userRepository.listUsers().stream()
            .map(UserEntity::toPublicMap)
            .filter(item -> {
                String haystack = (stringify(item.get("name")) + " " + stringify(item.get("email")) + " " + stringify(item.get("location"))).toLowerCase();
                return (term.isBlank() || haystack.contains(term))
                    && ("all".equals(role) || role.equals(item.get("role")))
                    && ("all".equals(status) || status.equals(item.get("status")));
            })
            .collect(Collectors.toList());
        return Map.of("users", users);
    }

    @PatchMapping("/users/{id}")
    public Map<String, Object> updateUser(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String id,
        @RequestBody Map<String, Object> body
    ) {
        authorizationService.requireRoles(currentUser, "admin");
        UserEntity existing = userRepository.findById(id);
        if (existing == null) {
            throw new ApiException(404, "User not found");
        }

        UserEntity patch = new UserEntity();
        patch.name = body.containsKey("name") ? ValidationUtils.requireString(body.get("name"), "Name is required", 2, 120) : existing.name;
        patch.role = body.containsKey("role") ? ValidationUtils.requireRole(body.get("role")) : existing.role;
        patch.status = body.containsKey("status") ? ValidationUtils.requireString(body.get("status"), "Status is required", 2, 20) : existing.status;
        patch.phone = body.containsKey("phone") ? defaultString(body.get("phone"), "") : existing.phone;
        patch.location = body.containsKey("location") ? defaultString(body.get("location"), "") : existing.location;

        return Map.of("user", userRepository.update(id, patch).toPublicMap());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String id) {
        authorizationService.requireRoles(currentUser, "admin");
        if (currentUser.getId().equals(id)) {
            throw new ApiException(400, "Admin cannot delete self");
        }

        UserEntity existing = userRepository.findById(id);
        if (existing == null) {
            throw new ApiException(404, "User not found");
        }

        userRepository.deleteById(id);
        jsonDatabaseService.mutate(state -> {
            state.donations.removeIf(item -> id.equals(item.donorId));
            state.requests.removeIf(item -> id.equals(item.recipientId));
            return state;
        });
        return ResponseEntity.noContent().build();
    }

    private void validateContentType(String type) {
        if (!CONTENT_TYPES.contains(type)) {
            throw new ApiException(400, "Invalid content type");
        }
    }

    private String defaultString(Object value, String fallback) {
        String text = ValidationUtils.optionalString(value);
        return text == null ? fallback : text;
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
