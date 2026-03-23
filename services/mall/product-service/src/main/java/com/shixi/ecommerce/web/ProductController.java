package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.dto.ReviewResponse;
import com.shixi.ecommerce.dto.ReviewSliceResponse;
import com.shixi.ecommerce.dto.ReviewSummaryResponse;
import com.shixi.ecommerce.service.ProductRecommendService;
import com.shixi.ecommerce.service.ProductSearchService;
import com.shixi.ecommerce.service.ProductService;
import com.shixi.ecommerce.service.ReviewService;
import com.shixi.ecommerce.service.ReviewSummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final ReviewSummaryService reviewSummaryService;
    private final ReviewService reviewService;
    private final ProductSearchService productSearchService;
    private final ProductRecommendService productRecommendService;

    public ProductController(ProductService productService,
                             ReviewSummaryService reviewSummaryService,
                             ReviewService reviewService,
                             ProductSearchService productSearchService,
                             ProductRecommendService productRecommendService) {
        this.productService = productService;
        this.reviewSummaryService = reviewSummaryService;
        this.reviewService = reviewService;
        this.productSearchService = productSearchService;
        this.productRecommendService = productRecommendService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> listActive(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(productService.listActiveProducts(page, size));
    }

    @GetMapping("/cursor")
    public ApiResponse<CursorPageResponse<ProductResponse>> listActiveCursor(@RequestParam(required = false)
                                                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                             LocalDateTime cursorTime,
                                                                             @RequestParam(required = false) Long cursorId,
                                                                             @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(productService.listActiveProductsCursor(cursorTime, cursorId, size));
    }

    @GetMapping("/search")
    public ApiResponse<CursorPageResponse<ProductResponse>> search(@RequestParam(required = false) String q,
                                                                   @RequestParam(required = false)
                                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                   LocalDateTime cursorTime,
                                                                   @RequestParam(required = false) Long cursorId,
                                                                   @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(productSearchService.searchActiveProducts(q, cursorTime, cursorId, size));
    }

    @GetMapping("/recommend")
    public ApiResponse<List<ProductResponse>> recommend(@RequestParam(required = false) Integer size) {
        return ApiResponse.ok(productRecommendService.recommend(size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProductResponse(id));
    }

    @GetMapping("/{id}/review-summary")
    public ApiResponse<ReviewSummaryResponse> getReviewSummary(@PathVariable Long id) {
        return ApiResponse.ok(reviewSummaryService.getSummary(id));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<ReviewSliceResponse> listReviews(@PathVariable Long id,
                                                        @RequestParam(required = false) Integer rating,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(reviewService.listReviews(id, rating, page, size));
    }

    @GetMapping("/{id}/reviews/cursor")
    public ApiResponse<CursorPageResponse<ReviewResponse>> listReviewsCursor(@PathVariable Long id,
                                                                             @RequestParam(required = false) Integer rating,
                                                                             @RequestParam(required = false)
                                                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                             LocalDateTime cursorTime,
                                                                             @RequestParam(required = false) Long cursorId,
                                                                             @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(reviewService.listReviewsCursor(id, rating, cursorTime, cursorId, size));
    }
}
