package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.service.agent.refund.RefundIntentLabel;
import java.util.ArrayList;
import java.util.List;

public class RefundIntentSample {
    private String text;
    private RefundIntentLabel label;

    public RefundIntentSample() {}

    public RefundIntentSample(String text, RefundIntentLabel label) {
        this.text = text;
        this.label = label;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public RefundIntentLabel getLabel() {
        return label;
    }

    public void setLabel(RefundIntentLabel label) {
        this.label = label;
    }

    public static List<RefundIntentSample> defaultSamples() {
        List<RefundIntentSample> samples = new ArrayList<>();
        samples.add(new RefundIntentSample("\u6211\u8981\u9000\u6b3e", RefundIntentLabel.REFUND));
        samples.add(new RefundIntentSample(
                "\u9000\u8d27\u9000\u6b3e\uff0c\u5546\u54c1\u7834\u635f", RefundIntentLabel.REFUND));
        samples.add(new RefundIntentSample("refund for broken item", RefundIntentLabel.REFUND));
        samples.add(
                new RefundIntentSample("\u672a\u6536\u5230\u8d27\uff0c\u8bf7\u9000\u6b3e", RefundIntentLabel.REFUND));
        samples.add(new RefundIntentSample(
                "\u53ea\u60f3\u9000\u6b3e\uff0c\u4e0d\u8981\u5546\u54c1", RefundIntentLabel.REFUND));

        samples.add(new RefundIntentSample("\u67e5\u8be2\u7269\u6d41", RefundIntentLabel.NOT_REFUND));
        samples.add(new RefundIntentSample("\u4f18\u60e0\u4ef7\u662f\u591a\u5c11", RefundIntentLabel.NOT_REFUND));
        samples.add(new RefundIntentSample("\u4f60\u4eec\u7684\u5ba2\u670d\u5728\u5417", RefundIntentLabel.NOT_REFUND));
        samples.add(new RefundIntentSample("where is my package", RefundIntentLabel.NOT_REFUND));
        samples.add(new RefundIntentSample("price inquiry for sku 123", RefundIntentLabel.NOT_REFUND));
        return samples;
    }
}
