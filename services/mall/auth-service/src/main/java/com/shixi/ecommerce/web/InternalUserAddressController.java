package com.shixi.ecommerce.web;

import com.shixi.ecommerce.dto.OrderAddressSnapshotResponse;
import com.shixi.ecommerce.service.UserAddressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUserAddressController {
    private final UserAddressService userAddressService;

    public InternalUserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @GetMapping("/{userId}/default-address")
    public OrderAddressSnapshotResponse getDefaultAddress(@PathVariable Long userId) {
        return userAddressService.getDefaultSnapshot(userId);
    }
}
