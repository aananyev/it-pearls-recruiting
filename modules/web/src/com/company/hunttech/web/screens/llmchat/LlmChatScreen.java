package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.service.LlmChatService;
import com.company.hunttech.service.LlmChatStreamState;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/** Compact floating chat shell with incremental provider output. */
@UiController("hunttech_LlmChatScreen")
@UiDescriptor("llm-chat-screen.xml")
public class LlmChatScreen extends Screen {
    @Inject
    private LlmChatService llmChatService;
    @Inject
    private Notifications notifications;
    @Inject
    private TextArea<String> historyArea;
    @Inject
    private TextArea<String> inputArea;
    @Inject
    private Button sendBtn;
    @Inject
    private Button cancelBtn;
    @Inject
    private Timer streamPollTimer;

    private UUID conversationId;
    private String activeRequestId;
    private String activeRequestText;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        try {
            conversationId = llmChatService.startConversation();
            renderHistory(llmChatService.loadHistory(conversationId));
        } catch (RuntimeException ex) {
            sendBtn.setEnabled(false);
            showError(ex);
        }
    }

    @Subscribe("sendBtn")
    public void onSend(Button.ClickEvent event) {
        String message = inputArea.getValue();
        if (message == null || message.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Введите сообщение")
                    .show();
            return;
        }
        final String request = message.trim();
        inputArea.setEnabled(false);
        sendBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        if (activeRequestId == null || !request.equals(activeRequestText)) {
            activeRequestId = UUID.randomUUID().toString();
            activeRequestText = request;
        }
        final String requestId = activeRequestId;
        try {
            LlmChatStreamState state = llmChatService.startStreaming(conversationId, request, requestId);
            streamPollTimer.start();
            applyStreamState(state);
        } catch (RuntimeException ex) {
            resetControls(false);
            showError(ex);
        }
    }

    @Subscribe("streamPollTimer")
    public void onStreamPoll(Timer.TimerActionEvent event) {
        if (conversationId == null || activeRequestId == null) {
            streamPollTimer.stop();
            return;
        }
        try {
            applyStreamState(llmChatService.pollStreaming(conversationId, activeRequestId));
        } catch (RuntimeException ex) {
            streamPollTimer.stop();
            resetControls(false);
            showError(ex);
        }
    }

    @Subscribe("cancelBtn")
    public void onCancel(Button.ClickEvent event) {
        if (conversationId == null || activeRequestId == null) {
            return;
        }
        try {
            llmChatService.cancelMessage(conversationId, activeRequestId);
            cancelBtn.setEnabled(false);
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Отмена запрошена")
                    .withDescription("Ответ не будет сохранён; провайдер может успеть учесть фактическое usage.")
                    .show();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void renderHistory(List<LlmChatMessage> messages) {
        renderHistory(messages, null);
    }

    private void renderHistory(List<LlmChatMessage> messages, String liveText) {
        StringBuilder rendered = new StringBuilder();
        for (LlmChatMessage message : messages) {
            rendered.append("USER".equals(message.getRole()) ? "Вы" : "ИИ")
                    .append(":\n").append(message.getContent()).append("\n\n");
        }
        if (liveText != null && !liveText.isEmpty()) {
            rendered.append("ИИ:\n").append(liveText).append("\n");
        }
        historyArea.setValue(rendered.toString());
    }

    private void applyStreamState(LlmChatStreamState state) {
        if (state == null) {
            return;
        }
        if (!state.isCompleted()) {
            renderHistory(llmChatService.loadHistory(conversationId), state.getText());
            return;
        }
        streamPollTimer.stop();
        boolean success = "COMPLETED".equals(state.getStatus());
        resetControls(success);
        activeRequestId = null;
        activeRequestText = null;
        renderHistory(llmChatService.loadHistory(conversationId));
        if (!success && state.getErrorMessage() != null) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Запрос к ИИ завершён без ответа")
                    .withDescription(state.getErrorMessage())
                    .show();
        }
    }

    private void resetControls(boolean clearInput) {
        if (clearInput) {
            inputArea.setValue("");
        }
        inputArea.setEnabled(true);
        sendBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
    }

    private void showError(Exception ex) {
        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption("Не удалось выполнить запрос к ИИ")
                .withDescription(ex.getMessage() == null ? "Проверьте настройки AI и согласие на fallback." : ex.getMessage())
                .show();
    }

}
