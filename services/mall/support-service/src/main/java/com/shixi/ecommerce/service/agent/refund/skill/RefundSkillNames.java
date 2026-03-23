package com.shixi.ecommerce.service.agent.refund.skill;

public final class RefundSkillNames {
    public static final String EXTRACT_REFUND_SLOTS = "extract_refund_slots";
    public static final String CONFIRM_ORDER = "confirm_order";
    public static final String CLASSIFY_REASON = "classify_refund_reason";
    public static final String CHECK_EVIDENCE = "check_evidence_requirement";
    public static final String EVALUATE_POLICY = "evaluate_refund_policy";
    public static final String VERIFY_LOGISTICS = "verify_logistics_state";
    public static final String SCORE_RISK = "score_refund_risk";
    public static final String COMPOSE_REPLY = "compose_customer_reply";
    public static final String VALIDATE_REPLY = "validate_reply";

    private RefundSkillNames() {
    }
}
