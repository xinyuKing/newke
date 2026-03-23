package com.nowcoder.community.service.moderation;

import java.util.Collections;
import java.util.List;

public class ModerationResult {
    private boolean pass;
    private List<String> reasons;
    private List<String> tags;

    public static ModerationResult pass() {
        ModerationResult result = new ModerationResult();
        result.pass = true;
        result.reasons = Collections.emptyList();
        result.tags = Collections.emptyList();
        return result;
    }

    public static ModerationResult reject(List<String> reasons, List<String> tags) {
        ModerationResult result = new ModerationResult();
        result.pass = false;
        result.reasons = reasons == null ? Collections.emptyList() : reasons;
        result.tags = tags == null ? Collections.emptyList() : tags;
        return result;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
