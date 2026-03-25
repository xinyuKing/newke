package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.UserAddress;
import com.shixi.ecommerce.dto.UserAddressRequest;
import com.shixi.ecommerce.repository.UserAddressRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository repository;

    private UserAddressService userAddressService;

    @BeforeEach
    void setUp() {
        userAddressService = new UserAddressService(repository);
        lenient().when(repository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateUnsettingOnlyDefaultKeepsAddressAsDefault() {
        UserAddress address = address(1L, 42L, true);
        when(repository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(address));
        when(repository.existsByUserIdAndIsDefaultTrue(42L)).thenReturn(false);
        when(repository.findTopByUserIdAndIdNotOrderByIdDesc(42L, 1L)).thenReturn(Optional.empty());

        UserAddressRequest request = request(false);
        userAddressService.update(42L, 1L, request);

        assertTrue(address.isDefault());
        verify(repository, times(2)).save(address);
    }

    @Test
    void updateUnsettingDefaultPromotesAnotherAddress() {
        UserAddress address = address(1L, 42L, true);
        UserAddress replacement = address(2L, 42L, false);
        when(repository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(address));
        when(repository.existsByUserIdAndIsDefaultTrue(42L)).thenReturn(false);
        when(repository.findTopByUserIdAndIdNotOrderByIdDesc(42L, 1L)).thenReturn(Optional.of(replacement));

        UserAddressRequest request = request(false);
        userAddressService.update(42L, 1L, request);

        assertFalse(address.isDefault());
        assertTrue(replacement.isDefault());
        verify(repository).save(replacement);
    }

    @Test
    void getDefaultSnapshotReturnsCurrentDefaultAddress() {
        UserAddress address = address(1L, 42L, true);
        when(repository.findFirstByUserIdAndIsDefaultTrueOrderByIdDesc(42L)).thenReturn(Optional.of(address));

        var response = userAddressService.getDefaultSnapshot(42L);

        assertEquals("Alice", response.getReceiverName());
        assertEquals("13800000000", response.getReceiverPhone());
        assertEquals("Pudong", response.getDistrict());
        assertEquals("Home", response.getTag());
    }

    @Test
    void getDefaultSnapshotRejectsMissingDefaultAddress() {
        when(repository.findFirstByUserIdAndIsDefaultTrueOrderByIdDesc(42L)).thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(BusinessException.class, () -> userAddressService.getDefaultSnapshot(42L));

        assertEquals("Default shipping address required", exception.getMessage());
    }

    private UserAddressRequest request(Boolean isDefault) {
        UserAddressRequest request = new UserAddressRequest();
        request.setReceiverName("Alice");
        request.setReceiverPhone("13800000000");
        request.setProvince("Shanghai");
        request.setCity("Shanghai");
        request.setDistrict("Pudong");
        request.setDetailAddress("No. 1");
        request.setPostalCode("200000");
        request.setTag("Home");
        request.setIsDefault(isDefault);
        return request;
    }

    private UserAddress address(Long id, Long userId, boolean isDefault) {
        UserAddress address = new UserAddress();
        setId(address, id);
        address.setUserId(userId);
        address.setReceiverName("Alice");
        address.setReceiverPhone("13800000000");
        address.setProvince("Shanghai");
        address.setCity("Shanghai");
        address.setDistrict("Pudong");
        address.setDetailAddress("No. 1");
        address.setPostalCode("200000");
        address.setTag("Home");
        address.setDefault(isDefault);
        return address;
    }

    private void setId(UserAddress address, Long id) {
        try {
            java.lang.reflect.Field field = UserAddress.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(address, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
