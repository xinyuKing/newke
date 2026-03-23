package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.Review;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 评价摘要构建器，用于输出简要评论摘要。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Component
public class ReviewSummaryBuilder {
    private static final List<String> POSITIVE_KEYWORDS =
            List.of("好", "满意", "推荐", "质量", "速度", "快", "便宜", "不错", "服务", "nice", "good", "great");
    private static final List<String> NEGATIVE_KEYWORDS =
            List.of("差", "慢", "坏", "失望", "破损", "不满意", "漏发", "退款", "退货", "poor", "bad", "slow");

    /**
     * 生成商品评价摘要。
     *
     * @param productId 商品 ID
     * @param reviews   评价列表
     * @return 摘要文本
     */
    public String buildSummary(Long productId, List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "暂无评价。";
        }
        long total = reviews.size();
        long positive = 0;
        long neutral = 0;
        long negative = 0;
        long sum = 0;
        for (Review review : reviews) {
            Integer rating = review.getRating();
            int value = rating == null ? 0 : rating;
            sum += value;
            if (value >= 4) {
                positive++;
            } else if (value == 3) {
                neutral++;
            } else {
                negative++;
            }
        }
        double avg = total == 0 ? 0.0 : (double) sum / total;

        String posKeywords = topKeywords(reviews, POSITIVE_KEYWORDS);
        String negKeywords = topKeywords(reviews, NEGATIVE_KEYWORDS);

        return String.format(
                "商品%d评价摘要：总数=%d，均分=%.2f，正向=%d，中性=%d，负向=%d。正向关注：%s。负向关注：%s。",
                productId,
                total,
                avg,
                positive,
                neutral,
                negative,
                posKeywords.isBlank() ? "无" : posKeywords,
                negKeywords.isBlank() ? "无" : negKeywords);
    }

    private String topKeywords(List<Review> reviews, List<String> keywords) {
        Map<String, Integer> counter = new HashMap<>();
        for (Review review : reviews) {
            String content =
                    review.getContent() == null ? "" : review.getContent().toLowerCase();
            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    counter.put(keyword, counter.getOrDefault(keyword, 0) + 1);
                }
            }
        }
        return counter.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
    }
}
