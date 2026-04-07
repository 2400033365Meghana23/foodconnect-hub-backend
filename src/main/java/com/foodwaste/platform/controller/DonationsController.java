package com.foodwaste.platform.controller;

import com.foodwaste.platform.exception.ApiException;
import com.foodwaste.platform.model.DataState;
import com.foodwaste.platform.model.Donation;
import com.foodwaste.platform.model.RequestRecord;
import com.foodwaste.platform.model.UserEntity;
import com.foodwaste.platform.security.CurrentUser;
import com.foodwaste.platform.service.AuthorizationService;
import com.foodwaste.platform.service.JsonDatabaseService;
import com.foodwaste.platform.util.ValidationUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DonationsController {

    private final JsonDatabaseService jsonDatabaseService;
    private final AuthorizationService authorizationService;

    public DonationsController(JsonDatabaseService jsonDatabaseService, AuthorizationService authorizationService) {
        this.jsonDatabaseService = jsonDatabaseService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/donations")
    public Map<String, Object> donations(
        @AuthenticationPrincipal CurrentUser currentUser,
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "all") String status,
        @RequestParam(defaultValue = "all") String category,
        @RequestParam(defaultValue = "false") String mine
    ) {
        DataState state = jsonDatabaseService.readState();
        String term = q.toLowerCase();

        List<Map<String, Object>> list = state.donations.stream()
            .map(donation -> decorateDonation(donation, state.users))
            .filter(item -> {
                if ("true".equalsIgnoreCase(mine) && "donor".equals(currentUser.getRole()) && !currentUser.getId().equals(item.get("donorId"))) {
                    return false;
                }
                String haystack = String.join(" ",
                    stringify(item.get("item")),
                    stringify(item.get("location")),
                    stringify(item.get("recipientOrg")),
                    stringify(item.get("category")),
                    stringify(item.get("donorName"))
                ).toLowerCase();
                return (term.isBlank() || haystack.contains(term))
                    && ("all".equals(status) || status.equals(item.get("status")))
                    && ("all".equals(category) || category.equals(item.get("category")));
            })
            .collect(Collectors.toList());

        return Map.of("donations", list);
    }

    @PostMapping("/donations")
    public Map<String, Object> createDonation(@AuthenticationPrincipal CurrentUser currentUser, @RequestBody Map<String, Object> body) {
        authorizationService.requireRoles(currentUser, "donor", "admin");

        Donation donation = new Donation();
        donation.id = UUID.randomUUID().toString();
        donation.item = ValidationUtils.requireString(body.get("item"), "Item is required", 2, 120);
        donation.category = ValidationUtils.requireString(body.get("category"), "Category is required", 2, 40);
        donation.quantity = ValidationUtils.requirePositiveDouble(body.get("quantity"), "quantity");
        donation.unit = defaultString(body.get("unit"), "kg");
        donation.status = "active";
        donation.date = Instant.now().toString().substring(0, 10);
        donation.expiryDate = defaultString(body.get("expiryDate"), "");
        donation.expiryHours = ValidationUtils.optionalInteger(body.get("expiryHours"));
        donation.location = ValidationUtils.requireString(body.get("location"), "Location is required", 2, 160);
        donation.description = defaultString(body.get("description"), "");
        donation.recipientOrg = defaultString(body.get("recipientOrg"), "");
        donation.donorId = currentUser.getId();
        donation.assignedRequestId = null;
        donation.createdAt = Instant.now().toString();
        donation.updatedAt = donation.createdAt;

        jsonDatabaseService.mutate(state -> {
            state.donations.add(0, donation);
            return state;
        });

        return Map.of("donation", donation);
    }

    @PatchMapping("/donations/{id}")
    public Map<String, Object> updateDonation(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String id,
        @RequestBody Map<String, Object> body
    ) {
        authorizationService.requireRoles(currentUser, "donor", "admin");

        DataState updated = jsonDatabaseService.mutate(state -> {
            Donation donation = state.donations.stream().filter(item -> item.id.equals(id)).findFirst().orElse(null);
            if (donation == null) {
                throw new ApiException(404, "Donation not found");
            }
            if (!"admin".equals(currentUser.getRole()) && !currentUser.getId().equals(donation.donorId)) {
                throw new ApiException(403, "Cannot update another donor donation");
            }

            if (body.containsKey("item")) donation.item = ValidationUtils.requireString(body.get("item"), "Item is required", 2, 120);
            if (body.containsKey("category")) donation.category = ValidationUtils.requireString(body.get("category"), "Category is required", 2, 40);
            if (body.containsKey("quantity")) donation.quantity = ValidationUtils.requirePositiveDouble(body.get("quantity"), "quantity");
            if (body.containsKey("unit")) donation.unit = defaultString(body.get("unit"), donation.unit);
            if (body.containsKey("status")) donation.status = defaultString(body.get("status"), donation.status);
            if (body.containsKey("expiryDate")) donation.expiryDate = defaultString(body.get("expiryDate"), "");
            if (body.containsKey("expiryHours")) donation.expiryHours = ValidationUtils.optionalInteger(body.get("expiryHours"));
            if (body.containsKey("location")) donation.location = ValidationUtils.requireString(body.get("location"), "Location is required", 2, 160);
            if (body.containsKey("description")) donation.description = defaultString(body.get("description"), "");
            if (body.containsKey("recipientOrg")) donation.recipientOrg = defaultString(body.get("recipientOrg"), "");
            donation.updatedAt = Instant.now().toString();
            return state;
        });

        Donation donation = updated.donations.stream().filter(item -> item.id.equals(id)).findFirst().orElseThrow();
        return Map.of("donation", donation);
    }

    @DeleteMapping("/donations/{id}")
    public ResponseEntity<Void> deleteDonation(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String id) {
        authorizationService.requireRoles(currentUser, "donor", "admin");

        jsonDatabaseService.mutate(state -> {
            Donation donation = state.donations.stream().filter(item -> item.id.equals(id)).findFirst().orElse(null);
            if (donation == null) {
                throw new ApiException(404, "Donation not found");
            }
            if (!"admin".equals(currentUser.getRole()) && !currentUser.getId().equals(donation.donorId)) {
                throw new ApiException(403, "Cannot delete another donor donation");
            }
            state.donations.removeIf(item -> item.id.equals(id));
            state.requests.removeIf(item -> item.donationId.equals(id));
            return state;
        });

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/donations/{id}/requests")
    public Map<String, Object> createRequest(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String id,
        @RequestBody Map<String, Object> body
    ) {
        authorizationService.requireRoles(currentUser, "recipient", "admin");

        RequestRecord request = new RequestRecord();
        request.id = UUID.randomUUID().toString();
        request.donationId = id;
        request.recipientId = currentUser.getId();
        request.requestedBy = currentUser.getName();
        request.status = "requested";
        request.requestDate = Instant.now().toString().substring(0, 10);
        request.estimatedDelivery = defaultString(body.get("estimatedDelivery"), "");
        request.deliveryDate = "";
        request.createdAt = Instant.now().toString();
        request.updatedAt = request.createdAt;

        jsonDatabaseService.mutate(state -> {
            Donation donation = state.donations.stream().filter(item -> item.id.equals(id)).findFirst().orElse(null);
            if (donation == null) {
                throw new ApiException(404, "Donation not found");
            }
            if (List.of("completed", "in-transit").contains(donation.status)) {
                throw new ApiException(409, "Donation is " + donation.status);
            }
            boolean existing = state.requests.stream().anyMatch(item ->
                item.donationId.equals(id)
                    && item.recipientId.equals(currentUser.getId())
                    && !List.of("declined", "completed").contains(item.status)
            );
            if (existing) {
                throw new ApiException(409, "Request already exists for this donation");
            }
            state.requests.add(0, request);
            donation.status = "requested";
            donation.updatedAt = Instant.now().toString();
            return state;
        });

        return Map.of("request", request);
    }

    @PatchMapping("/donations/{id}/requests/{requestId}")
    public Map<String, Object> patchRequest(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String id,
        @PathVariable String requestId,
        @RequestBody Map<String, Object> body
    ) {
        authorizationService.requireRoles(currentUser, "donor", "recipient", "admin");
        String action = defaultString(body.get("action"), "");
        if (!List.of("accept", "decline", "complete").contains(action)) {
            throw new ApiException(400, "Invalid payload");
        }

        DataState updated = jsonDatabaseService.mutate(state -> {
            Donation donation = state.donations.stream().filter(item -> item.id.equals(id)).findFirst().orElse(null);
            if (donation == null) {
                throw new ApiException(404, "Donation not found");
            }
            RequestRecord request = state.requests.stream()
                .filter(item -> item.id.equals(requestId) && item.donationId.equals(id))
                .findFirst()
                .orElse(null);
            if (request == null) {
                throw new ApiException(404, "Request not found");
            }

            boolean isDonationOwner = currentUser.getId().equals(donation.donorId);
            boolean isRequestOwner = currentUser.getId().equals(request.recipientId);

            if ("complete".equals(action) && !"admin".equals(currentUser.getRole()) && !isRequestOwner) {
                throw new ApiException(403, "Only request owner can complete delivery");
            }
            if (List.of("accept", "decline").contains(action) && !"admin".equals(currentUser.getRole()) && !isDonationOwner) {
                throw new ApiException(403, "Only donation owner can accept/decline request");
            }

            request.status = switch (action) {
                case "accept" -> "accepted";
                case "decline" -> "declined";
                default -> "completed";
            };
            if ("complete".equals(action)) {
                request.deliveryDate = Instant.now().toString().substring(0, 10);
            }
            request.updatedAt = Instant.now().toString();

            donation.status = switch (action) {
                case "accept" -> "in-transit";
                case "decline" -> "active";
                default -> "completed";
            };
            donation.assignedRequestId = "accept".equals(action) ? requestId : ("decline".equals(action) ? null : donation.assignedRequestId);
            donation.updatedAt = Instant.now().toString();

            if ("accept".equals(action)) {
                for (RequestRecord item : state.requests) {
                    if (item.donationId.equals(id) && !item.id.equals(requestId) && "requested".equals(item.status)) {
                        item.status = "declined";
                        item.updatedAt = Instant.now().toString();
                    }
                }
            }
            return state;
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("donation", updated.donations.stream().filter(item -> item.id.equals(id)).findFirst().orElse(null));
        response.put("request", updated.requests.stream().filter(item -> item.id.equals(requestId)).findFirst().orElse(null));
        return response;
    }

    @GetMapping("/requests")
    public Map<String, Object> requests(@AuthenticationPrincipal CurrentUser currentUser) {
        DataState state = jsonDatabaseService.readState();
        List<RequestRecord> filtered = new ArrayList<>(state.requests);

        if ("recipient".equals(currentUser.getRole())) {
            filtered = filtered.stream().filter(item -> item.recipientId.equals(currentUser.getId())).collect(Collectors.toList());
        }
        if ("donor".equals(currentUser.getRole())) {
            List<String> donationIds = state.donations.stream()
                .filter(item -> item.donorId.equals(currentUser.getId()))
                .map(item -> item.id)
                .toList();
            filtered = filtered.stream().filter(item -> donationIds.contains(item.donationId)).collect(Collectors.toList());
        }

        List<Map<String, Object>> decorated = filtered.stream()
            .sorted(Comparator.comparing((RequestRecord item) -> item.createdAt).reversed())
            .map(item -> decorateRequest(item, state))
            .toList();

        return Map.of("requests", decorated);
    }

    private Map<String, Object> decorateDonation(Donation donation, List<UserEntity> users) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", donation.id);
        map.put("item", donation.item);
        map.put("category", donation.category);
        map.put("quantity", donation.quantity);
        map.put("unit", donation.unit);
        map.put("status", donation.status);
        map.put("date", donation.date);
        map.put("expiryDate", donation.expiryDate);
        map.put("expiryHours", donation.expiryHours);
        map.put("location", donation.location);
        map.put("description", donation.description);
        map.put("recipientOrg", donation.recipientOrg);
        map.put("donorId", donation.donorId);
        map.put("assignedRequestId", donation.assignedRequestId);
        map.put("createdAt", donation.createdAt);
        map.put("updatedAt", donation.updatedAt);
        UserEntity donor = users.stream().filter(item -> item.id.equals(donation.donorId)).findFirst().orElse(null);
        map.put("donorName", donor == null ? "Donor" : donor.name);
        map.put("donorEmail", donor == null ? "" : donor.email);
        return map;
    }

    private Map<String, Object> decorateRequest(RequestRecord request, DataState state) {
        Donation donation = state.donations.stream().filter(item -> item.id.equals(request.donationId)).findFirst().orElse(null);
        UserEntity recipient = state.users.stream().filter(item -> item.id.equals(request.recipientId)).findFirst().orElse(null);
        UserEntity donor = donation == null ? null : state.users.stream().filter(item -> item.id.equals(donation.donorId)).findFirst().orElse(null);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", request.id);
        map.put("donationId", request.donationId);
        map.put("recipientId", request.recipientId);
        map.put("requestedBy", request.requestedBy);
        map.put("status", request.status);
        map.put("requestDate", request.requestDate);
        map.put("estimatedDelivery", request.estimatedDelivery);
        map.put("deliveryDate", request.deliveryDate);
        map.put("createdAt", request.createdAt);
        map.put("updatedAt", request.updatedAt);
        map.put("donationItem", donation == null ? "" : donation.item);
        map.put("donationStatus", donation == null ? "" : donation.status);
        map.put("recipientName", recipient == null ? (request.requestedBy == null ? "Recipient" : request.requestedBy) : recipient.name);
        map.put("donorName", donor == null ? "Donor" : donor.name);
        return map;
    }

    private String defaultString(Object value, String fallback) {
        String text = ValidationUtils.optionalString(value);
        return text == null ? fallback : text;
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
