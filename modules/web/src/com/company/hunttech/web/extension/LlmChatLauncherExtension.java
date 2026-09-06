package com.company.hunttech.web.extension;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.Button;

/** Adds viewport-safe drag behavior to the native LLM chat launcher button. */
@JavaScript("llm-chat-launcher.js")
public class LlmChatLauncherExtension extends AbstractJavaScriptExtension {

    public void extend(Button button, String storageKey) {
        super.extend(button);
        callFunction("initialize", storageKey);
    }
}
