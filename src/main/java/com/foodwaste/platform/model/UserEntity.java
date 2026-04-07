package com.foodwaste.platform.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserEntity {

    public String id;
    public String email;
    public String passwordHash;
    public String name;
    public String role;
    public String status;
    public String phone;
    public String location;
    public Instant createdAt;
    public Instant updatedAt;

    public Map<String, Object> toPublicMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("email", email);
        map.put("name", name);
        map.put("role", role);
        map.put("status", status);
        map.put("phone", phone == null ? "" : phone);
        map.put("location", location == null ? "" : location);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
