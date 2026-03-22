package com.shixi.ecommerce.web.user;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.ReviewCreateRequest;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/reviews")
public class UserReviewController {
    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public UserReviewController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<String> create(@Valid @RequestBody ReviewCreateRequest request) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        reviewService.addReview(userId, request);
        return ApiResponse.ok("OK");
    }
}
