package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.LlmChatStreamEvent;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.service.LlmChatService;
import com.company.hunttech.service.LlmChatStreamState;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DialogWindow;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.gui.settings.Settings;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.UI;
import org.dom4j.Element;
import org.springframework.context.event.EventListener;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/** Compact floating chat shell with incremental provider output. */
@UiController("hunttech_LlmChatScreen")
@UiDescriptor("llm-chat-screen.xml")
public class LlmChatScreen extends Screen {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LlmChatScreen.class);
    private static final String CHAT_LAYOUT_SETTINGS = "llmChatLayout";
    private static final String CHAT_DIALOG_STYLENAME = "llm-chat-window";

    @Inject
    private LlmChatService llmChatService;
    @Inject
    private Notifications notifications;
    @Inject
    private ScrollBoxLayout historyScrollBox;
    @Inject
    private Label<String> historyLabel;
    @Inject
    private TextArea<String> inputArea;
    @Inject
    private Button sendBtn;
    @Inject
    private Timer streamPollTimer;
    @Inject
    private UserSession userSession;
    @Inject
    private com.haulmont.cuba.core.global.DataManager dataManager;

    private UUID conversationId;
    private String activeRequestId;
    private String activeRequestText;
    private UI chatUi;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        DialogWindow dialog = getDialogWindow();
        if (dialog != null) {
            dialog.setDialogStylename(CHAT_DIALOG_STYLENAME);
            dialog.setDialogWidth("840px");
            dialog.setDialogHeight("560px");
            dialog.setModal(false);
            dialog.setDraggable(true);
            dialog.setResizable(true);
            dialog.setCloseable(true);
        }
        ensureUserFallbackConsent();
        try {
            conversationId = llmChatService.startConversation();
            renderHistory(llmChatService.loadHistory(conversationId));
        } catch (RuntimeException ex) {
            sendBtn.setEnabled(false);
            showError(ex);
        }
    }

    private void ensureUserFallbackConsent() {
        if (userSession == null || userSession.getUser() == null || dataManager == null) {
            return;
        }
        com.haulmont.cuba.security.entity.User sessionUser = userSession.getUser();
        com.company.hunttech.entity.ExtUser currentUser = (sessionUser instanceof com.company.hunttech.entity.ExtUser)
                ? (com.company.hunttech.entity.ExtUser) sessionUser
                : dataManager.load(com.company.hunttech.entity.ExtUser.class).id(sessionUser.getId()).optional().orElse(null);
        if (currentUser == null) {
            return;
        }
        try {
            com.company.hunttech.entity.UserAiProfile profile = dataManager.load(com.company.hunttech.entity.UserAiProfile.class)
                    .query("select p from hunttech_UserAiProfile p where p.user.id = :userId")
                    .parameter("userId", currentUser.getId())
                    .view("userAiProfile-view")
                    .optional()
                    .orElse(null);
            if (profile == null) {
                profile = dataManager.create(com.company.hunttech.entity.UserAiProfile.class);
                profile.setUser(currentUser);
                profile.setProfileEnabled(false);
                profile.setExternalProcessingAllowed(false);
                profile.setAdminFallbackConsent(true);
                profile.setAdminFallbackConsentVersion(com.company.hunttech.service.AiConsentPolicy.ADMIN_FALLBACK_VERSION);
                profile.setAdminFallbackConsentAt(new java.util.Date());
                dataManager.commit(profile);
            } else if (profile.getAdminFallbackConsent() == null
                    || (Boolean.TRUE.equals(profile.getAdminFallbackConsent())
                        && !com.company.hunttech.service.AiConsentPolicy.ADMIN_FALLBACK_VERSION.equals(profile.getAdminFallbackConsentVersion()))) {
                profile.setAdminFallbackConsent(true);
                profile.setAdminFallbackConsentVersion(com.company.hunttech.service.AiConsentPolicy.ADMIN_FALLBACK_VERSION);
                profile.setAdminFallbackConsentAt(new java.util.Date());
                dataManager.commit(profile);
            }
        } catch (Exception e) {
            log.warn("Не удалось актуализировать fallback consent для пользователя {}: {}",
                    currentUser.getLogin(), e.getMessage());
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        chatUi = UI.getCurrent();
        if (chatUi != null) {
            chatUi.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        }
        restoreDialogGeometry(getSettings());
        sendBtn.setCaption("<svg class=\"llm-chat-send-svg\" viewBox=\"0 0 24 24\" width=\"20\" height=\"20\"><path fill=\"white\" d=\"M1.101 21.757L23.8 12.028 1.101 2.3 1.1 9.873l16.216 2.155L1.1 14.183z\"/></svg>");
        sendBtn.setDescription("Отправить сообщение (Enter, перенос строки — Shift+Enter)");
        com.vaadin.ui.TextArea vTextArea = inputArea.unwrap(com.vaadin.ui.TextArea.class);
        if (vTextArea != null) {
            vTextArea.setValueChangeMode(com.vaadin.shared.ui.ValueChangeMode.EAGER);
            vTextArea.addShortcutListener(new com.vaadin.event.ShortcutListener("SendOnEnter",
                    com.vaadin.event.ShortcutAction.KeyCode.ENTER, new int[0]) {
                @Override
                public void handleAction(Object sender, Object target) {
                    executeSend();
                }
            });
        }
        if (chatUi != null && chatUi.getPage() != null && chatUi.getPage().getJavaScript() != null) {
            chatUi.getPage().getJavaScript().execute(
                    "(function() {" +
                    "  var ta = document.querySelector('.llm-chat-input-area textarea');" +
                    "  if (ta && !ta._enterBound) {" +
                    "    ta._enterBound = true;" +
                    "    ta.addEventListener('keydown', function(e) {" +
                    "      if (e.key === 'Enter' && !e.shiftKey) {" +
                    "        e.preventDefault();" +
                    "        var btn = document.querySelector('.llm-chat-send-btn');" +
                    "        if (btn) btn.click();" +
                    "      }" +
                    "    });" +
                    "  }" +
                    "})()"
            );
        }
    }

    @EventListener
    public void onLlmChatStreamEvent(LlmChatStreamEvent event) {
        if (conversationId == null || activeRequestId == null
                || !conversationId.equals(event.getConversationId())
                || !activeRequestId.equals(event.getRequestId())
                || userSession == null || userSession.getUser() == null
                || !userSession.getUser().getId().equals(event.getUserId())) {
            return;
        }
        UI ui = chatUi;
        if (ui == null) {
            return;
        }
        ui.access(() -> {
            if (conversationId == null || activeRequestId == null) {
                return;
            }
            try {
                applyStreamState(llmChatService.pollStreaming(conversationId, activeRequestId));
            } catch (RuntimeException ex) {
                streamPollTimer.stop();
                resetControls(false);
                showError(ex);
            }
        });
    }

    @Override
    protected void saveSettings() {
        saveDialogGeometry(getSettings());
        super.saveSettings();
    }

    private void restoreDialogGeometry(Settings settings) {
        DialogWindow dialog = getDialogWindow();
        if (settings == null || dialog == null) {
            return;
        }
        Element layout = settings.get(CHAT_LAYOUT_SETTINGS);
        setPositionIfPresent(layout, "positionX", dialog::setPositionX);
        setPositionIfPresent(layout, "positionY", dialog::setPositionY);
        String width = layout.attributeValue("width");
        if ("AUTO".equalsIgnoreCase(width)) {
            dialog.setDialogWidth("AUTO");
        } else if (width != null && !width.isEmpty()) {
            try {
                int w = Integer.parseInt(width.replaceAll("[^0-9]", ""));
                if (w < 800) {
                    dialog.setDialogWidth("840px");
                } else {
                    dialog.setDialogWidth(width);
                }
            } catch (Exception ignored) {
                dialog.setDialogWidth("840px");
            }
        } else {
            dialog.setDialogWidth("840px");
        }
        setSizeIfPresent(layout, "height", dialog::setDialogHeight);
    }

    private void saveDialogGeometry(Settings settings) {
        DialogWindow dialog = getDialogWindow();
        if (settings == null || dialog == null) {
            return;
        }
        Element layout = settings.get(CHAT_LAYOUT_SETTINGS);
        layout.addAttribute("positionX", String.valueOf(dialog.getPositionX()));
        layout.addAttribute("positionY", String.valueOf(dialog.getPositionY()));
        layout.addAttribute("width", sizeValue(dialog.getDialogWidth(), dialog.getDialogWidthUnit()));
        layout.addAttribute("height", sizeValue(dialog.getDialogHeight(), dialog.getDialogHeightUnit()));
        settings.setModified(true);
    }

    private void setPositionIfPresent(Element layout, String attribute, java.util.function.IntConsumer setter) {
        String value = layout.attributeValue(attribute);
        if (value != null && !value.isEmpty()) {
            try {
                setter.accept(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                // Ignore corrupted legacy settings and keep the framework default.
            }
        }
    }

    private void setSizeIfPresent(Element layout, String attribute, java.util.function.Consumer<String> setter) {
        String value = layout.attributeValue(attribute);
        if (value != null && !value.isEmpty()) {
            setter.accept(value);
        }
    }

    private String sizeValue(float value, com.haulmont.cuba.gui.components.SizeUnit unit) {
        if (value < 0) {
            return "AUTO";
        }
        return Math.round(value) + (unit == null ? "px" : unit.getSymbol());
    }

    private DialogWindow getDialogWindow() {
        return getWindow() instanceof DialogWindow ? (DialogWindow) getWindow() : null;
    }

    @Subscribe("sendBtn")
    public void onSend(Button.ClickEvent event) {
        executeSend();
    }

    private void executeSend() {
        if (!sendBtn.isEnabled()) {
            return;
        }
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

    private void renderHistory(List<LlmChatMessage> messages) {
        renderHistory(messages, null);
    }

    private void renderHistory(List<LlmChatMessage> messages, String liveText) {
        String html = MarkdownRenderer.renderChatHistory(messages, liveText);
        historyLabel.setValue(html);
        scrollToBottom();
    }

    private void scrollToBottom() {
        try {
            com.vaadin.ui.Panel panel = historyScrollBox.unwrap(com.vaadin.ui.Panel.class);
            if (panel != null) {
                panel.setScrollTop(Integer.MAX_VALUE / 2);
            }
        } catch (Exception ex) {
            log.debug("Не удалось выполнить автоскролл historyScrollBox: {}", ex.getMessage());
        }
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
    }

    private void showError(Exception ex) {
        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption("Не удалось выполнить запрос к ИИ")
                .withDescription(ex.getMessage() == null ? "Проверьте настройки AI и согласие на fallback." : ex.getMessage())
                .show();
    }

}
