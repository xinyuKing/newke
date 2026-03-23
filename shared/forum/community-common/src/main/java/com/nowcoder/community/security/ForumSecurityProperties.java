package com.nowcoder.community.security;

public class ForumSecurityProperties {
    private boolean csrfEnabled = true;
    private boolean secure = false;
    private String sameSite = "Lax";
    private String cookiePath = "/";
    private String ticketCookieName = "ticket";
    private String captchaCookieName = "kaptchaOwner";
    private String csrfCookieName = "forum_csrf";
    private String csrfHeaderName = "X-CSRF-Token";
    private long csrfMaxAgeSeconds = 604800;

    public boolean isCsrfEnabled() {
        return csrfEnabled;
    }

    public void setCsrfEnabled(boolean csrfEnabled) {
        this.csrfEnabled = csrfEnabled;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public String getTicketCookieName() {
        return ticketCookieName;
    }

    public void setTicketCookieName(String ticketCookieName) {
        this.ticketCookieName = ticketCookieName;
    }

    public String getCaptchaCookieName() {
        return captchaCookieName;
    }

    public void setCaptchaCookieName(String captchaCookieName) {
        this.captchaCookieName = captchaCookieName;
    }

    public String getCsrfCookieName() {
        return csrfCookieName;
    }

    public void setCsrfCookieName(String csrfCookieName) {
        this.csrfCookieName = csrfCookieName;
    }

    public String getCsrfHeaderName() {
        return csrfHeaderName;
    }

    public void setCsrfHeaderName(String csrfHeaderName) {
        this.csrfHeaderName = csrfHeaderName;
    }

    public long getCsrfMaxAgeSeconds() {
        return csrfMaxAgeSeconds;
    }

    public void setCsrfMaxAgeSeconds(long csrfMaxAgeSeconds) {
        this.csrfMaxAgeSeconds = csrfMaxAgeSeconds;
    }
}
