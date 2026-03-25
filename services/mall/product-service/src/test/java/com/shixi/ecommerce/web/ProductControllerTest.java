package com.shixi.ecommerce.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ReviewSummaryService reviewSummaryService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private ProductSearchService productSearchService;

    @Mock
    private ProductRecommendService productRecommendService;

    private ProductController productController;

    @BeforeEach
    void setUp() {
        productController = new ProductController(
                productService, reviewSummaryService, reviewService, productSearchService, productRecommendService);
    }

    @Test
    void getByIdUsesPublicProductResponse() {
        ProductResponse response =
                new ProductResponse(1001L, 7L, "Keyboard", "Mechanical keyboard", null, new BigDecimal("399.00"), null);
        when(productService.getPublicProductResponse(1001L)).thenReturn(response);

        ProductResponse body = productController.getById(1001L).getData();

        assertEquals(1001L, body.getId());
        verify(productService).getPublicProductResponse(1001L);
    }

    @Test
    void getReviewSummaryRequiresPublicProductAccess() {
        ReviewSummaryResponse summary = new ReviewSummaryResponse(1001L, 8L, "Praised for latency");
        when(reviewSummaryService.getSummary(1001L)).thenReturn(summary);

        ReviewSummaryResponse body = productController.getReviewSummary(1001L).getData();

        assertEquals(8L, body.getReviewCount());
        InOrder inOrder = inOrder(productService, reviewSummaryService);
        inOrder.verify(productService).assertPublicProductAccessible(1001L);
        inOrder.verify(reviewSummaryService).getSummary(1001L);
    }

    @Test
    void listReviewsRequiresPublicProductAccess() {
        ReviewSliceResponse response = new ReviewSliceResponse(List.of(), false);
        when(reviewService.listReviews(1001L, 5, 0, 10)).thenReturn(response);

        ReviewSliceResponse body =
                productController.listReviews(1001L, 5, 0, 10).getData();

        assertEquals(0, body.getItems().size());
        InOrder inOrder = inOrder(productService, reviewService);
        inOrder.verify(productService).assertPublicProductAccessible(1001L);
        inOrder.verify(reviewService).listReviews(1001L, 5, 0, 10);
    }

    @Test
    void listReviewsCursorRequiresPublicProductAccess() {
        LocalDateTime cursorTime = LocalDateTime.of(2026, 3, 24, 10, 0);
        CursorPageResponse<ReviewResponse> response = new CursorPageResponse<>(List.of(), false, cursorTime, 11L);
        when(reviewService.listReviewsCursor(1001L, 5, cursorTime, 11L, 10)).thenReturn(response);

        CursorPageResponse<ReviewResponse> body = productController
                .listReviewsCursor(1001L, 5, cursorTime, 11L, 10)
                .getData();

        assertEquals(11L, body.getNextCursorId());
        InOrder inOrder = inOrder(productService, reviewService);
        inOrder.verify(productService).assertPublicProductAccessible(1001L);
        inOrder.verify(reviewService).listReviewsCursor(1001L, 5, cursorTime, 11L, 10);
    }
}
