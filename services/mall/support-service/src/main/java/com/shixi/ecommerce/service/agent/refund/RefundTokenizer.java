package com.shixi.ecommerce.service.agent.refund;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RefundTokenizer {
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        char[] chars = text.toLowerCase().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (isHan(ch)) {
                tokens.add(String.valueOf(ch));
                if (i + 1 < chars.length && isHan(chars[i + 1])) {
                    tokens.add("" + ch + chars[i + 1]);
                }
            } else if (Character.isLetterOrDigit(ch)) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(ch);
                int j = i + 1;
                while (j < chars.length && Character.isLetterOrDigit(chars[j])) {
                    buffer.append(chars[j]);
                    j++;
                }
                tokens.add(buffer.toString());
                i = j - 1;
            }
        }
        return tokens;
    }

    private boolean isHan(char ch) {
        return ch >= 0x4E00 && ch <= 0x9FFF;
    }
}
