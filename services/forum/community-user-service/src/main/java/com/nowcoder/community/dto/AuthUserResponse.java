package com.nowcoder.community.dto;

public class AuthUserResponse {
    private int id;
    private String username;
    private int type;
    private int status;
    private String email;
    private String headerUrl;

    public AuthUserResponse() {
    }

    public AuthUserResponse(int id, String username, int type, int status, String email, String headerUrl) {
        this.id = id;
        this.username = username;
        this.type = type;
        this.status = status;
        this.email = email;
        this.headerUrl = headerUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHeaderUrl() {
        return headerUrl;
    }

    public void setHeaderUrl(String headerUrl) {
        this.headerUrl = headerUrl;
    }
}
