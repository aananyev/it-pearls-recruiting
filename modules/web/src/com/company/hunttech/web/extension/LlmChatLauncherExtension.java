package com.company.hunttech.web.extension;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.Button;
import elemental.json.JsonArray;

import java.io.Serializable;

/** Adds viewport-safe drag behavior to the native LLM chat launcher button. */
@JavaScript("llm-chat-launcher.js")
public class LlmChatLauncherExtension extends AbstractJavaScriptExtension {

    @FunctionalInterface
    public interface PositionChangeListener extends Serializable {
        void onPositionChanged(String positionJson);
    }

    public void extend(Button button, String storageKey) {
        extend(button, storageKey, null, null);
    }

    public void extend(Button button, String storageKey, String initialPosition, PositionChangeListener listener) {
        super.extend(button);
        if (listener != null) {
            addFunction("savePosition", (JsonArray arguments) -> {
                if (arguments != null && arguments.length() > 0) {
                    listener.onPositionChanged(arguments.getString(0));
                }
            });
        }
        callFunction("initialize", storageKey, initialPosition);
    }
}
