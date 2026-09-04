package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.service.LlmChatService;
import com.company.hunttech.service.LlmChatResponse;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/** Compact floating chat shell for the synchronous MVP. */
@UiController("hunttech_LlmChatScreen")
@UiDescriptor("llm-chat-screen.xml")
public class LlmChatScreen extends Screen {
    @Inject
    private LlmChatService llmChatService;
    @Inject
    private BackgroundWorker backgroundWorker;
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
        BackgroundTask<Integer, LlmChatResponse> task =
                new BackgroundTask<Integer, LlmChatResponse>(60, this) {
                    @Override
                    public LlmChatResponse run(TaskLifeCycle<Integer> taskLifeCycle) {
                        return llmChatService.sendMessage(conversationId, request, requestId);
                    }

                    @Override
                    public void done(LlmChatResponse response) {
                        inputArea.setValue("");
                        inputArea.setEnabled(true);
                        sendBtn.setEnabled(true);
                        cancelBtn.setEnabled(false);
                        activeRequestId = null;
                        activeRequestText = null;
                        renderHistory(llmChatService.loadHistory(conversationId));
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        inputArea.setEnabled(true);
                        sendBtn.setEnabled(true);
                        cancelBtn.setEnabled(false);
                        if (isCancellationMessage(ex)) {
                            activeRequestId = null;
                            activeRequestText = null;
                        }
                        showError(ex);
                        return true;
                    }

                    @Override
                    public boolean handleTimeoutException() {
                        inputArea.setEnabled(true);
                        sendBtn.setEnabled(true);
                        cancelBtn.setEnabled(false);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Чат не ответил вовремя")
                                .withDescription("Результат запроса не подтверждён; резерв квоты помечен как уточняющий.")
                                .show();
                        return true;
                    }
                };
        backgroundWorker.handle(task).execute();
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
        StringBuilder rendered = new StringBuilder();
        for (LlmChatMessage message : messages) {
            rendered.append("USER".equals(message.getRole()) ? "Вы" : "ИИ")
                    .append(":\n").append(message.getContent()).append("\n\n");
        }
        historyArea.setValue(rendered.toString());
    }

    private void showError(Exception ex) {
        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption("Не удалось выполнить запрос к ИИ")
                .withDescription(ex.getMessage() == null ? "Проверьте настройки AI и согласие на fallback." : ex.getMessage())
                .show();
    }

    private boolean isCancellationMessage(Exception ex) {
        return ex != null && ex.getMessage() != null && ex.getMessage().contains("Запрос отменён");
    }
}
