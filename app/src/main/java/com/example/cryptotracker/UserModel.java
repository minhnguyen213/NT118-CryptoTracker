package com.example.cryptotracker;

public class UserModel {
    private String id;
    private String email;
    private String fullName;
    private String role;

    public UserModel(String id, String email, String fullName, String role) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
