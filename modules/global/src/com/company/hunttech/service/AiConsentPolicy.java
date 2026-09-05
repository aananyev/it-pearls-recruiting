package com.company.hunttech.service;

/** Versioned consent identifiers shared by the profile UI and middleware gates. */
public final class AiConsentPolicy {
    public static final String PROFILE_EXTERNAL_PROCESSING_VERSION = "2026-07-22-v1";
    public static final String ADMIN_FALLBACK_VERSION = "2026-09-05-v1";
    public static final String LLM_CHAT_PRIVACY_POLICY_VERSION = "llm-chat-privacy-v1";

    private AiConsentPolicy() {
    }
}
