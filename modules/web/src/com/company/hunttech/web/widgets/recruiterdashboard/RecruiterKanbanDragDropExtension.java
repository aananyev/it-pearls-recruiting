package com.company.hunttech.web.widgets.recruiterdashboard;

import com.haulmont.cuba.gui.components.HBoxLayout;
import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.AbstractComponent;
import elemental.json.JsonArray;

import java.io.Serializable;

/**
 * Расширение Drag-and-Drop правой кнопкой мыши для карточек Канбан-доски.
 */
@JavaScript("recruiter-kanban-dnd.js")
public class RecruiterKanbanDragDropExtension extends AbstractJavaScriptExtension {

    @FunctionalInterface
    public interface CardMoveListener extends Serializable {
        void onCardMoved(String interactionId, String targetStage);
    }

    public void extend(HBoxLayout kanbanBoard, CardMoveListener listener) {
        super.extend(kanbanBoard.unwrap(AbstractComponent.class));
        if (listener != null) {
            addFunction("onCardMoved", (JsonArray arguments) -> {
                if (arguments != null && arguments.length() >= 2) {
                    String interactionId = arguments.getString(0);
                    String targetStage = arguments.getString(1);
                    listener.onCardMoved(interactionId, targetStage);
                }
            });
        }
    }

    public void reinit() {
        callFunction("initDragAndDrop");
    }
}
