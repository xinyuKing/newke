package com.nowcoder.community.util;

public final class ApiResponseUtils {
    private ApiResponseUtils() {}

    public static boolean isOk(ApiResponse<?> response) {
        return response != null && response.getCode() == 0;
    }

    public static <T> T unwrap(ApiResponse<T> response) {
        return isOk(response) ? response.getData() : null;
    }
}
