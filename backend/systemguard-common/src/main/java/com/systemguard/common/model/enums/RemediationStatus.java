package com.systemguard.common.model.enums;

public enum RemediationStatus {
    NONE,
    REQUESTED,
    ANALYZING,
    PATCH_GENERATED,
    PATCH_APPLIED,
    BUILDING,
    TESTING,
    RESCANNING,
    POLICY_CHECK,
    VERIFIED,
    REJECTED,
    FAILED
}
