package com.foodwaste.platform.security;

public class CurrentUser {

    private final String id;
    private final String email;
    private final String role;
    private final String name;

    public CurrentUser(String id, String email, String role, String name) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }
}
