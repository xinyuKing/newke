package com.shixi.ecommerce.integration;

public class CommunityAuthRequest {
    private String username;
    private String password;
    private String email;
    private boolean autoActivate = true;

    public CommunityAuthRequest() {
    }

    public CommunityAuthRequest(String username, String password, String email, boolean autoActivate) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.autoActivate = autoActivate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAutoActivate() {
        return autoActivate;
    }

    public void setAutoActivate(boolean autoActivate) {
        this.autoActivate = autoActivate;
    }
}
