package com.shixi.ecommerce.network;

import java.util.ArrayList;
import java.util.List;

public class ClientIpProperties {
    private List<String> trustedProxies = new ArrayList<>(List.of("127.0.0.1/32", "::1/128"));

    public List<String> getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies == null ? new ArrayList<>() : new ArrayList<>(trustedProxies);
    }
}
