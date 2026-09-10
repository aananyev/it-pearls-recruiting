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
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.security.entity.EntityOp;
import elemental.json.JsonArray;
import org.dom4j.Element;
import org.springframework.context.event.EventListener;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
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
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Security security;

    private UUID conversationId;
    private String activeRequestId;
    private String activeRequestText;
    private UI chatUi;
    private boolean hrmEntityBridgeRegistered = false;

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
        inputArea.setTrimming(false);
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
        inputArea.setTrimming(false);
        sendBtn.setCaption("<svg class=\"llm-chat-send-svg\" viewBox=\"0 0 24 24\" width=\"30\" height=\"30\" preserveAspectRatio=\"xMidYMid meet\"><path fill=\"white\" d=\"M1.101 21.757L23.8 12.028 1.101 2.3 1.1 9.873l16.216 2.155L1.1 14.183z\"/></svg>");
        sendBtn.setDescription("Отправить сообщение (Enter, перенос строки — Shift+Enter)");
        com.vaadin.ui.TextArea vTextArea = inputArea.unwrap(com.vaadin.ui.TextArea.class);
        if (vTextArea != null) {
            vTextArea.setValueChangeMode(com.vaadin.shared.ui.ValueChangeMode.TIMEOUT);
            vTextArea.setValueChangeTimeout(300);
        }
        com.vaadin.ui.JavaScript js = (chatUi != null && chatUi.getPage() != null)
                ? chatUi.getPage().getJavaScript()
                : com.vaadin.ui.JavaScript.getCurrent();
        if (js != null) {
            js.addFunction("hunttechSendChatMessage", (JsonArray arguments) -> {
                if (arguments != null && arguments.length() >= 1) {
                    try {
                        String msg = arguments.getString(0);
                        executeSend(msg);
                    } catch (Exception ex) {
                        log.warn("Ошибка обработки вызова hunttechSendChatMessage: {}", ex.getMessage());
                    }
                }
            });
            if (!hrmEntityBridgeRegistered) {
                hrmEntityBridgeRegistered = true;
                js.addFunction("hunttechOpenHrmEntity", (JsonArray arguments) -> {
                    if (arguments != null && arguments.length() >= 2) {
                        try {
                            String entityType = arguments.getString(0);
                            String entityId = arguments.getString(1);
                            openHrmEntityScreen(entityType, entityId);
                        } catch (Exception ex) {
                            log.warn("Ошибка обработки параметров вызова hunttechOpenHrmEntity: {}", ex.getMessage());
                        }
                    }
                });
            }
            js.execute(
                    "(function() {" +
                    "  function isChatInput(el) {" +
                    "    if (!el) return false;" +
                    "    if (el.tagName === 'TEXTAREA') {" +
                    "      if (el.classList && (el.classList.contains('llm-chat-input-area') || el.classList.contains('v-textarea'))) {" +
                    "        return true;" +
                    "      }" +
                    "      if (el.closest && (el.closest('.llm-chat-input-bar') || el.closest('.llm-chat-input-area') || el.closest('.llm-chat-screen'))) {" +
                    "        return true;" +
                    "      }" +
                    "    }" +
                    "    return false;" +
                    "  }" +
                    "  if (!window._hunttechChatKeyHandlerAttached) {" +
                    "    window._hunttechChatKeyHandlerAttached = true;" +
                    "    document.addEventListener('keydown', function(e) {" +
                    "      if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey && !e.altKey) {" +
                    "        var target = e.target;" +
                    "        if (isChatInput(target)) {" +
                    "          e.preventDefault();" +
                    "          e.stopPropagation();" +
                    "          var btn = document.querySelector('.llm-chat-send-btn');" +
                    "          if (btn && (btn.classList.contains('v-disabled') || btn.disabled)) {" +
                    "            return;" +
                    "          }" +
                    "          var text = target.value;" +
                    "          if (window.hunttechSendChatMessage) {" +
                    "            target.value = '';" +
                    "            window.hunttechSendChatMessage(text);" +
                    "          } else if (btn) {" +
                    "            btn.click();" +
                    "          }" +
                    "        }" +
                    "      }" +
                    "    }, true);" +
                    "  }" +
                    "  if (!window._hunttechChatSendClickHandlerAttached) {" +
                    "    window._hunttechChatSendClickHandlerAttached = true;" +
                    "    document.addEventListener('click', function(e) {" +
                    "      var target = e.target;" +
                    "      var btn = target ? (target.closest ? target.closest('.llm-chat-send-btn') : null) : null;" +
                    "      if (btn && !btn.classList.contains('v-disabled') && !btn.disabled && window.hunttechSendChatMessage) {" +
                    "        var ta = document.querySelector('.llm-chat-input-bar textarea, textarea.llm-chat-input-area, .llm-chat-input-area textarea, .llm-chat-screen textarea');" +
                    "        if (ta && ta.value && ta.value.trim().length > 0) {" +
                    "          e.preventDefault();" +
                    "          e.stopPropagation();" +
                    "          var text = ta.value;" +
                    "          ta.value = '';" +
                    "          window.hunttechSendChatMessage(text);" +
                    "        }" +
                    "      }" +
                    "    }, true);" +
                    "  }" +
                    "  if (!window._hunttechHrmLinkHandlerAttached) {" +
                    "    window._hunttechHrmLinkHandlerAttached = true;" +
                    "    document.addEventListener('click', function(e) {" +
                    "      var target = e.target;" +
                    "      var link = target ? (target.closest ? target.closest('.llm-hrm-entity-link') : null) : null;" +
                    "      if (link && window.hunttechOpenHrmEntity) {" +
                    "        var entity = link.getAttribute('data-entity');" +
                    "        var id = link.getAttribute('data-id');" +
                    "        if (entity && id) {" +
                    "          e.preventDefault();" +
                    "          e.stopPropagation();" +
                    "          window.hunttechOpenHrmEntity(entity, id);" +
                    "        }" +
                    "      }" +
                    "    }, true);" +
                    "  }" +
                    "})()"
            );
        }
    }

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        hrmEntityBridgeRegistered = false;
        if (chatUi != null && chatUi.getPage() != null && chatUi.getPage().getJavaScript() != null) {
            try {
                chatUi.getPage().getJavaScript().removeFunction("hunttechSendChatMessage");
            } catch (Exception ignored) {
            }
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
        executeSend(null);
    }

    private void executeSend() {
        executeSend(null);
    }

    private void executeSend(String rawText) {
        if (!sendBtn.isEnabled()) {
            return;
        }
        String message = (rawText != null && !rawText.trim().isEmpty())
                ? rawText
                : inputArea.getValue();
        if (message == null || message.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Введите сообщение")
                    .show();
            return;
        }
        final String request = message.trim();
        inputArea.setValue("");
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
        } else if (activeRequestText != null && !activeRequestText.isEmpty()) {
            inputArea.setValue(activeRequestText);
        }
        inputArea.setEnabled(true);
        sendBtn.setEnabled(true);
        inputArea.focus();
    }

    private void showError(Exception ex) {
        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption("Не удалось выполнить запрос к ИИ")
                .withDescription(ex.getMessage() == null ? "Проверьте настройки AI и согласие на fallback." : ex.getMessage())
                .show();
    }

    private void openHrmEntityScreen(String entityType, String entityId) {
        if (entityType == null || entityType.trim().isEmpty()) {
            return;
        }
        String trimmedId = entityId == null ? "" : entityId.trim();
        if (trimmedId.isEmpty()) {
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(trimmedId);
        } catch (IllegalArgumentException e) {
            log.warn("Некорректный UUID сущности HRM в ссылке чата: {}", entityId);
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Некорректный идентификатор сущности")
                    .show();
            return;
        }

        try {
            switch (entityType.toLowerCase(Locale.ROOT)) {
                case "candidate":
                    if (!security.isEntityOpPermitted(JobCandidate.class, EntityOp.READ)) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для просмотра кандидата")
                                .show();
                        return;
                    }
                    JobCandidate candidate = dataManager.load(JobCandidate.class)
                            .id(id)
                            .view("jobCandidate-view")
                            .optional()
                            .orElse(null);
                    if (candidate != null) {
                        screenBuilders.editor(JobCandidate.class, this)
                                .editEntity(candidate)
                                .withOpenMode(OpenMode.NEW_TAB)
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption("Кандидат не найден или был удалён")
                                .show();
                    }
                    break;

                case "vacancy":
                    if (!security.isEntityOpPermitted(OpenPosition.class, EntityOp.READ)) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для просмотра вакансии")
                                .show();
                        return;
                    }
                    OpenPosition vacancy = dataManager.load(OpenPosition.class)
                            .id(id)
                            .view("openPosition-view")
                            .optional()
                            .orElse(null);
                    if (vacancy != null) {
                        screenBuilders.editor(OpenPosition.class, this)
                                .editEntity(vacancy)
                                .withOpenMode(OpenMode.NEW_TAB)
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption("Вакансия не найдена или была удалена")
                                .show();
                    }
                    break;

                case "interaction":
                    if (!security.isEntityOpPermitted(IteractionList.class, EntityOp.READ)) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для просмотра взаимодействия")
                                .show();
                        return;
                    }
                    IteractionList interaction = dataManager.load(IteractionList.class)
                            .id(id)
                            .view("iteractionList-edit-view")
                            .optional()
                            .orElse(null);
                    if (interaction != null) {
                        screenBuilders.editor(IteractionList.class, this)
                                .editEntity(interaction)
                                .withOpenMode(OpenMode.NEW_TAB)
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption("Взаимодействие не найдено или было удалено")
                                .show();
                    }
                    break;

                default:
                    log.warn("Неизвестный тип сущности HRM в чате: {}", entityType);
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("Неподдерживаемый тип сущности: " + entityType)
                            .show();
            }
        } catch (Exception ex) {
            log.error("Ошибка при открытии сущности {} ({}) из LLM-чата", entityType, id, ex);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Не удалось открыть карточку")
                    .withDescription("Проверьте права доступа или обратитесь к администратору.")
                    .show();
        }
    }

}
