package com.company.hunttech.core.ai;

/** Internal signal used to prevent fallback after a user cancelled an active provider call. */
public class AiRequestCancelledException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AiRequestCancelledException(String requestId) {
        super("AI-запрос отменён: " + requestId);
    }
}
