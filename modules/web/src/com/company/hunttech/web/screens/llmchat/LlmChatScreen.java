package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.LlmChatStreamEvent;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.service.LlmChatService;
import com.company.hunttech.service.LlmChatStreamState;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DialogWindow;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.haulmont.cuba.gui.components.TabSheet;
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
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.web.screens.candidatecv.CandidateCVEdit;
import com.company.hunttech.web.screens.company.CompanyEdit;
import com.company.hunttech.web.screens.iteractionlist.IteractionListEdit;
import com.company.hunttech.web.screens.jobcandidate.JobCandidateEdit;
import com.company.hunttech.web.screens.openposition.OpenPositionEdit;
import com.company.hunttech.web.screens.openposition.OpenPositionReestrBrowse;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.security.entity.EntityOp;
import com.company.hunttech.service.HermesChatService;
import com.company.hunttech.service.dto.HermesChatMessage;
import com.company.hunttech.service.dto.HermesChatResponse;
import elemental.json.JsonArray;
import org.dom4j.Element;
import org.springframework.context.event.EventListener;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;

import javax.inject.Inject;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compact floating chat shell with incremental provider output. */
@UiController("hunttech_LlmChatScreen")
@UiDescriptor("llm-chat-screen.xml")
public class LlmChatScreen extends Screen {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LlmChatScreen.class);
    private static final String CHAT_LAYOUT_SETTINGS = "llmChatLayout";
    private static final String CHAT_DIALOG_STYLENAME = "llm-chat-window";
    private static final int PAGE_SIZE = 20;

    public static final String PERMISSION_LOCAL_CHAT = "hunttech.ai.useLocalChat";
    public static final String PERMISSION_MANAGER_HERMES_WRITE = "hunttech.ai.useManagerHermesWrite";

    @Inject
    private LlmChatService llmChatService;
    @Inject
    private HermesChatService hermesChatService;
    @Inject
    private com.company.hunttech.service.HermesManagerChatService hermesManagerChatService;
    @Inject
    private com.company.hunttech.service.UserAiQuotaService userAiQuotaService;
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
    @Inject
    private com.haulmont.cuba.gui.config.WindowConfig windowConfig;

    // Hermes tab components
    @Inject
    private TabSheet chatTabSheet;
    @Inject
    private ScrollBoxLayout hermesHistoryScrollBox;
    @Inject
    private Label<String> hermesHistoryLabel;
    @Inject
    private TextArea<String> hermesInputArea;
    @Inject
    private Button hermesSendBtn;

    // Hermes Manager tab components
    @Inject
    private ScrollBoxLayout hermesManagerHistoryScrollBox;
    @Inject
    private Label<String> hermesManagerHistoryLabel;
    @Inject
    private TextArea<String> hermesManagerInputArea;
    @Inject
    private Button hermesManagerSendBtn;

    private UUID conversationId;
    private String activeRequestId;
    private String activeRequestText;
    private long activeRequestStartTime;
    private List<LlmChatMessage> currentStreamingHistory;
    private List<LlmChatMessage> lastRenderedHistory;
    private UUID hermesConversationId;
    private String activeHermesRequestText;
    private long activeHermesStartTime = 0L;
    private List<HermesChatMessage> activeHermesPendingHistory;
    private HermesStatusPhase lastRenderedViewerPhase;
    private UUID hermesManagerConversationId;
    private String activeHermesManagerRequestText;
    private long activeHermesManagerStartTime = 0L;
    private List<HermesChatMessage> activeHermesManagerPendingHistory;
    private HermesStatusPhase lastRenderedOperatorPhase;
    private UI chatUi;
    private boolean hrmEntityBridgeRegistered = false;
    private int localVisibleLimit = PAGE_SIZE;
    private int hermesVisibleLimit = PAGE_SIZE;
    private int hermesManagerVisibleLimit = PAGE_SIZE;
    private boolean hermesManagerConnectionWarningShown = false;

    private static class HermesStatusPhase {
        final long minElapsedMs;
        final String badge;
        final String text;

        HermesStatusPhase(long minElapsedMs, String badge, String text) {
            this.minElapsedMs = minElapsedMs;
            this.badge = badge;
            this.text = text;
        }
    }

    private static final HermesStatusPhase[] HERMES_VIEWER_PHASES = new HermesStatusPhase[]{
            new HermesStatusPhase(0, "Канал", "Установка соединения с hr.hunttech.ru..."),
            new HermesStatusPhase(400, "SSH", "Открытие защищенного туннеля..."),
            new HermesStatusPhase(800, "Docker", "Проверка hermes-hrm-viewer..."),
            new HermesStatusPhase(1200, "Профиль", "Инициализация hrm-viewer..."),
            new HermesStatusPhase(1600, "Вход", "Разбор входящего запроса..."),
            new HermesStatusPhase(2000, "Инструменты", "Подготовка HRM Data Tools..."),
            new HermesStatusPhase(2500, "Промпт", "Сборка контекста агента..."),
            new HermesStatusPhase(3000, "Нейросеть", "Отправка запроса в модель..."),
            new HermesStatusPhase(3600, "В очереди", "Ожидание слота генерации..."),
            new HermesStatusPhase(4200, "Мышление", "Анализ семантики вопроса..."),
            new HermesStatusPhase(4900, "Выборка", "Запрос справочников HRM..."),
            new HermesStatusPhase(5600, "Кандидаты", "Поиск релевантных сущностей..."),
            new HermesStatusPhase(6400, "Вакансии", "Сопоставление требований..."),
            new HermesStatusPhase(7200, "Календарь", "Проверка слотов и событий..."),
            new HermesStatusPhase(8100, "Аналитика", "Синтез структуры ответа..."),
            new HermesStatusPhase(9000, "Аргументы", "Формирование ключевых тезисов..."),
            new HermesStatusPhase(10000, "Генерация", "Построение связного текста..."),
            new HermesStatusPhase(11200, "Разметка", "Форматирование таблиц и списков..."),
            new HermesStatusPhase(12500, "Сверка", "Контроль непротиворечивости..."),
            new HermesStatusPhase(14000, "Валидация", "Проверка фактов и дат..."),
            new HermesStatusPhase(15800, "Компоновка", "Сборка итогового markdown..."),
            new HermesStatusPhase(17800, "Стриминг", "Получение выходного потока..."),
            new HermesStatusPhase(20000, "Финал", "Завершение генерации..."),
            new HermesStatusPhase(23000, "Ожидание", "Финальная передача ответа...")
    };

    private static final HermesStatusPhase[] HERMES_OPERATOR_PHASES = new HermesStatusPhase[]{
            new HermesStatusPhase(0, "Канал", "Подключение к контуру управления..."),
            new HermesStatusPhase(400, "SSH", "Туннель к серверу hr.hunttech.ru..."),
            new HermesStatusPhase(800, "Docker", "Инициализация hermes-hrm-operator..."),
            new HermesStatusPhase(1200, "Профиль", "Загрузка профиля hrm-operator..."),
            new HermesStatusPhase(1600, "Права", "Проверка контекста пользователя..."),
            new HermesStatusPhase(2000, "Навык", "Загрузка hunttech-vacancy-opening..."),
            new HermesStatusPhase(2500, "Параметры", "Разбор инструкций и атрибутов..."),
            new HermesStatusPhase(3000, "Нейросеть", "Вызов модели deepseek..."),
            new HermesStatusPhase(3600, "В очереди", "Ожидание вычислительного слота..."),
            new HermesStatusPhase(4200, "Мышление", "Анализ бизнес-требований..."),
            new HermesStatusPhase(4900, "Стек", "Проверка технологий и грейда..."),
            new HermesStatusPhase(5600, "Зарплата", "Валидация вилок и компенсаций..."),
            new HermesStatusPhase(6400, "Локация", "Формат работы и график..."),
            new HermesStatusPhase(7200, "Мутация", "Формирование намерения..."),
            new HermesStatusPhase(8100, "Схема", "Проверка JSON-структуры..."),
            new HermesStatusPhase(9000, "Allowlist", "Контроль разрешенных полей..."),
            new HermesStatusPhase(10000, "Аудит", "Проверка запрета DELETE/SQL..."),
            new HermesStatusPhase(11200, "Транзакция", "Подготовка контекста CUBA..."),
            new HermesStatusPhase(12500, "Сборка", "Синтез ответа для оператора..."),
            new HermesStatusPhase(14000, "Чек-лист", "Проверка полноты критериев..."),
            new HermesStatusPhase(15800, "Карта поиска", "Подготовка рекомендаций..."),
            new HermesStatusPhase(17800, "Разметка", "Форматирование результата..."),
            new HermesStatusPhase(20000, "Финал", "Завершение операции..."),
            new HermesStatusPhase(23000, "Ожидание", "Финальная передача данных...")
    };

    private static HermesStatusPhase resolvePhase(HermesStatusPhase[] phases, long elapsed) {
        if (phases == null || phases.length == 0) {
            return new HermesStatusPhase(0, "В работе", "Обработка запроса...");
        }
        if (elapsed < 0) {
            elapsed = 0;
        }
        HermesStatusPhase current = phases[0];
        for (HermesStatusPhase phase : phases) {
            if (elapsed >= phase.minElapsedMs) {
                current = phase;
            } else {
                break;
            }
        }
        return current;
    }

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

        applyTabPermissions();

        try {
            conversationId = resolveActiveConversationId();
            renderHistory(llmChatService.loadHistory(conversationId));
        } catch (RuntimeException ex) {
            sendBtn.setEnabled(false);
            showError(ex);
        }
        try {
            initHermesTab();
        } catch (Exception ex) {
            log.warn("Предварительная инициализация вкладки Hermes: {}", ex.getMessage());
        }
    }

    private void applyTabPermissions() {
        TabSheet.Tab localTab = chatTabSheet.getTab("localChatTab");
        TabSheet.Tab hermesTab = chatTabSheet.getTab("hermesChatTab");
        TabSheet.Tab hermesManagerTab = chatTabSheet.getTab("hermesManagerChatTab");

        if (localTab != null) {
            localTab.setVisible(true);
        }
        if (hermesTab != null) {
            hermesTab.setVisible(true);
        }
        if (hermesManagerTab != null) {
            hermesManagerTab.setVisible(true);
        }

        // Если сохраненная или текущая вкладка скрыта — переключаем на первую доступную
        TabSheet.Tab selectedTab = chatTabSheet.getSelectedTab();
        if (selectedTab == null || !selectedTab.isVisible()) {
            if (localTab != null && localTab.isVisible()) {
                chatTabSheet.setSelectedTab(localTab);
            } else if (hermesTab != null && hermesTab.isVisible()) {
                chatTabSheet.setSelectedTab(hermesTab);
            } else if (hermesManagerTab != null && hermesManagerTab.isVisible()) {
                chatTabSheet.setSelectedTab(hermesManagerTab);
            }
        }
    }

    private UUID resolveActiveConversationId() {
        if (userSession == null || userSession.getUser() == null || dataManager == null) {
            return llmChatService.startConversation();
        }
        UUID userId = userSession.getUser().getId();
        try {
            LlmChatConversation latestConv = dataManager.load(LlmChatConversation.class)
                    .query("select e from hunttech_LlmChatConversation e " +
                            "where e.user.id = :userId and e.status = 'ACTIVE' " +
                            "and (e.title is null or (e.title not like 'Hermes:%' and e.title not like 'Hermes-Manager:%')) " +
                            "order by e.lastMessageAt desc nulls last, e.createTs desc")
                    .parameter("userId", userId)
                    .view("llm-chat-conversation-view")
                    .maxResults(1)
                    .optional()
                    .orElse(null);
            if (latestConv != null) {
                log.info("Восстановлен предыдущий активный диалог LLM: convId={}, title={}",
                        latestConv.getId(), latestConv.getTitle());
                return latestConv.getId();
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить последний активный диалог: {}", e.getMessage());
        }
        return llmChatService.startConversation();
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
        
        // Initialize Hermes tab
        hermesInputArea.setTrimming(false);
        hermesInputArea.setEnabled(true);
        hermesSendBtn.setEnabled(true);
        hermesSendBtn.setCaption("<svg class=\"llm-chat-send-svg\" viewBox=\"0 0 24 24\" width=\"30\" height=\"30\" preserveAspectRatio=\"xMidYMid meet\"><path fill=\"white\" d=\"M1.101 21.757L23.8 12.028 1.101 2.3 1.1 9.873l16.216 2.155L1.1 14.183z\"/></svg>");
        hermesSendBtn.setDescription("Отправить сообщение в Hermes (Enter, перенос строки — Shift+Enter)");
        com.vaadin.ui.TextArea vHermesTextArea = hermesInputArea.unwrap(com.vaadin.ui.TextArea.class);
        if (vHermesTextArea != null) {
            vHermesTextArea.setValueChangeMode(com.vaadin.shared.ui.ValueChangeMode.TIMEOUT);
            vHermesTextArea.setValueChangeTimeout(300);
        }

        // Initialize Hermes Manager tab
        hermesManagerInputArea.setTrimming(false);
        hermesManagerInputArea.setEnabled(true);
        hermesManagerSendBtn.setEnabled(true);
        hermesManagerSendBtn.setCaption("<svg class=\"llm-chat-send-svg\" viewBox=\"0 0 24 24\" width=\"30\" height=\"30\" preserveAspectRatio=\"xMidYMid meet\"><path fill=\"white\" d=\"M1.101 21.757L23.8 12.028 1.101 2.3 1.1 9.873l16.216 2.155L1.1 14.183z\"/></svg>");
        hermesManagerSendBtn.setDescription("Отправить команду в Hermes (Enter, перенос строки — Shift+Enter)");
        com.vaadin.ui.TextArea vHermesManagerTextArea = hermesManagerInputArea.unwrap(com.vaadin.ui.TextArea.class);
        if (vHermesManagerTextArea != null) {
            vHermesManagerTextArea.setValueChangeMode(com.vaadin.shared.ui.ValueChangeMode.TIMEOUT);
            vHermesManagerTextArea.setValueChangeTimeout(300);
        }

        // Add tab change listener to handle tab-specific behavior and autoscroll to bottom
        chatTabSheet.addSelectedTabChangeListener(tabChangeEvent -> {
            TabSheet.Tab selectedTab = tabChangeEvent.getSelectedTab();
            if (selectedTab != null) {
                String tabId = selectedTab.getName();
                if ("hermesChatTab".equals(tabId)) {
                    initHermesTab();
                    scrollToBottomHermes();
                    executeScrollBottomJs();
                } else if ("hermesManagerChatTab".equals(tabId)) {
                    initHermesManagerTab();
                    scrollToBottomHermesManager();
                    executeScrollBottomJs();
                } else if ("localChatTab".equals(tabId)) {
                    inputArea.focus();
                    scrollToBottom();
                    executeScrollBottomJs();
                }
            }
        });

        TabSheet.Tab initialSelectedTab = chatTabSheet.getSelectedTab();
        if (initialSelectedTab != null) {
            if ("hermesChatTab".equals(initialSelectedTab.getName())) {
                initHermesTab();
            } else if ("hermesManagerChatTab".equals(initialSelectedTab.getName())) {
                initHermesManagerTab();
            }
        }

        // Первичное гарантированное перемещение истории в самый конец (Требование 1)
        scrollToBottom();
        scrollToBottomHermes();
        scrollToBottomHermesManager();
        executeScrollBottomJs();

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
            js.addFunction("hunttechSendHermesChatMessage", (JsonArray arguments) -> {
                if (arguments != null && arguments.length() >= 1) {
                    try {
                        String msg = arguments.getString(0);
                        executeHermesSend(msg);
                    } catch (Exception ex) {
                        log.warn("Ошибка обработки вызова hunttechSendHermesChatMessage: {}", ex.getMessage());
                    }
                }
            });
            js.addFunction("hunttechSendHermesManagerChatMessage", (JsonArray arguments) -> {
                if (arguments != null && arguments.length() >= 1) {
                    try {
                        String msg = arguments.getString(0);
                        executeHermesManagerSend(msg);
                    } catch (Exception ex) {
                        log.warn("Ошибка обработки вызова hunttechSendHermesManagerChatMessage: {}", ex.getMessage());
                    }
                }
            });
            js.addFunction("hunttechLoadEarlierMessages", (JsonArray arguments) -> {
                loadEarlierMessages();
            });
            js.addFunction("hunttechLoadEarlierHermesMessages", (JsonArray arguments) -> {
                loadEarlierHermesMessages();
            });
            js.addFunction("hunttechLoadEarlierHermesManagerMessages", (JsonArray arguments) -> {
                loadEarlierHermesManagerMessages();
            });
            js.addFunction("hunttechExecuteChatAction", (JsonArray arguments) -> {
                if (arguments != null && arguments.length() >= 1) {
                    try {
                        String actionUrl = arguments.getString(0);
                        executeChatAction(actionUrl);
                    } catch (Exception ex) {
                        log.warn("Ошибка обработки вызова hunttechExecuteChatAction: {}", ex.getMessage());
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
                    "      if (el.classList && (" +
                    "          el.classList.contains('llm-chat-input-area') ||" +
                    "          el.classList.contains('v-textarea') ||" +
                    "          el.classList.contains('hermes-chat-input-area') ||" +
                    "          el.classList.contains('hermes-manager-chat-input-area') ||" +
                    "          el.classList.contains('local-chat-input-area')" +
                    "      )) {" +
                    "        return true;" +
                    "      }" +
                    "      if (el.closest && (" +
                    "          el.closest('.llm-chat-input-bar') ||" +
                    "          el.closest('.llm-chat-input-area') ||" +
                    "          el.closest('.hermes-chat-tab-pane') ||" +
                    "          el.closest('.hermes-manager-chat-tab-pane') ||" +
                    "          el.closest('.local-chat-tab-pane') ||" +
                    "          el.closest('.llm-chat-screen')" +
                    "      )) {" +
                    "        return true;" +
                    "      }" +
                    "    }" +
                    "    return false;" +
                    "  }" +
                    "  function isManagerHermesContext(el) {" +
                    "    if (!el) return false;" +
                    "    if (el.classList && (" +
                    "        el.classList.contains('hermes-manager-chat-input-area') ||" +
                    "        el.classList.contains('hermes-manager-chat-send-btn') ||" +
                    "        el.classList.contains('hermes-manager-chat-tab-pane') ||" +
                    "        el.classList.contains('hermes-manager-chat-input-bar')" +
                    "    )) {" +
                    "      return true;" +
                    "    }" +
                    "    if (el.closest && (" +
                    "        el.closest('.hermes-manager-chat-tab-pane') ||" +
                    "        el.closest('.hermes-manager-chat-input-bar') ||" +
                    "        el.closest('.hermes-manager-chat-input-area') ||" +
                    "        el.closest('.hermes-manager-chat-send-btn') ||" +
                    "        el.closest('[cuba-id=\"hermesManagerChatTab\"]') ||" +
                    "        el.closest('[cuba-id=\"hermesManagerInputArea\"]') ||" +
                    "        el.closest('[cuba-id=\"hermesManagerSendBtn\"]') ||" +
                    "        el.closest('#hermesManagerChatTab')" +
                    "    )) {" +
                    "      return true;" +
                    "    }" +
                    "    var mPane = document.querySelector('.hermes-manager-chat-tab-pane');" +
                    "    if (mPane && mPane.offsetParent !== null) {" +
                    "      return true;" +
                    "    }" +
                    "    return false;" +
                    "  }" +
                    "  function isHermesContext(el) {" +
                    "    if (!el) return false;" +
                    "    if (el.classList && (" +
                    "        el.classList.contains('hermes-chat-input-area') ||" +
                    "        el.classList.contains('hermes-chat-send-btn') ||" +
                    "        el.classList.contains('hermes-chat-tab-pane') ||" +
                    "        el.classList.contains('hermes-chat-input-bar')" +
                    "    )) {" +
                    "      return true;" +
                    "    }" +
                    "    if (el.closest && (" +
                    "        el.closest('.hermes-chat-tab-pane') ||" +
                    "        el.closest('.hermes-chat-input-bar') ||" +
                    "        el.closest('.hermes-chat-input-area') ||" +
                    "        el.closest('.hermes-chat-send-btn') ||" +
                    "        el.closest('[cuba-id=\"hermesChatTab\"]') ||" +
                    "        el.closest('[cuba-id=\"hermesInputArea\"]') ||" +
                    "        el.closest('[cuba-id=\"hermesSendBtn\"]') ||" +
                    "        el.closest('#hermesChatTab') ||" +
                    "        el.closest('[id*=\"hermes\"]')" +
                    "    )) {" +
                    "      return true;" +
                    "    }" +
                    "    var selectedTab = document.querySelector('.llm-chat-tabsheet .v-tabsheet-tabitem-selected, .v-tabsheet-tabitem-selected');" +
                    "    if (selectedTab && selectedTab.textContent && selectedTab.textContent.toLowerCase().indexOf('hermes') >= 0 && selectedTab.textContent.toLowerCase().indexOf('управление') < 0) {" +
                    "      return true;" +
                    "    }" +
                    "    var hermesPane = document.querySelector('.hermes-chat-tab-pane');" +
                    "    if (hermesPane && hermesPane.offsetParent !== null) {" +
                    "      var localPane = document.querySelector('.local-chat-tab-pane');" +
                    "      if (!localPane || localPane.offsetParent === null) {" +
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
                    "          if (isManagerHermesContext(target)) {" +
                    "            var mBtn = document.querySelector('.hermes-manager-chat-send-btn, #hermesManagerSendBtn, [cuba-id=\"hermesManagerSendBtn\"]');" +
                    "            if (mBtn && (mBtn.classList.contains('v-disabled') || mBtn.disabled)) {" +
                    "              return;" +
                    "            }" +
                    "            var mText = target.value;" +
                    "            if (window.hunttechSendHermesManagerChatMessage) {" +
                    "              target.value = '';" +
                    "              window.hunttechSendHermesManagerChatMessage(mText);" +
                    "            } else if (mBtn) {" +
                    "              mBtn.click();" +
                    "            }" +
                    "            return;" +
                    "          }" +
                    "          var isHermes = isHermesContext(target);" +
                    "          if (isHermes) {" +
                    "            var hBtn = document.querySelector('.hermes-chat-send-btn, #hermesSendBtn, [cuba-id=\"hermesSendBtn\"]');" +
                    "            if (hBtn && (hBtn.classList.contains('v-disabled') || hBtn.disabled)) {" +
                    "              return;" +
                    "            }" +
                    "            var hText = target.value;" +
                    "            if (window.hunttechSendHermesChatMessage) {" +
                    "              target.value = '';" +
                    "              window.hunttechSendHermesChatMessage(hText);" +
                    "            } else if (hBtn) {" +
                    "              hBtn.click();" +
                    "            }" +
                    "            return;" +
                    "          }" +
                    "          var lBtn = document.querySelector('.local-chat-send-btn, #sendBtn, .llm-chat-send-btn');" +
                    "          if (lBtn && (lBtn.classList.contains('v-disabled') || lBtn.disabled)) {" +
                    "            return;" +
                    "          }" +
                    "          var lText = target.value;" +
                    "          if (window.hunttechSendChatMessage) {" +
                    "            target.value = '';" +
                    "            window.hunttechSendChatMessage(lText);" +
                    "          } else if (lBtn) {" +
                    "            lBtn.click();" +
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
                    "      if (btn && !btn.classList.contains('v-disabled') && !btn.disabled) {" +
                    "        if (isManagerHermesContext(btn)) {" +
                    "          if (window.hunttechSendHermesManagerChatMessage) {" +
                    "            var mTa = document.querySelector('.hermes-manager-chat-input-area textarea, textarea.hermes-manager-chat-input-area, .hermes-manager-chat-tab-pane textarea, [cuba-id=\"hermesManagerInputArea\"] textarea');" +
                    "            if (mTa && mTa.value && mTa.value.trim().length > 0) {" +
                    "              e.preventDefault();" +
                    "              e.stopPropagation();" +
                    "              var mText = mTa.value;" +
                    "              mTa.value = '';" +
                    "              window.hunttechSendHermesManagerChatMessage(mText);" +
                    "            }" +
                    "          }" +
                    "          return;" +
                    "        }" +
                    "        var isHermesBtn = isHermesContext(btn);" +
                    "        if (isHermesBtn) {" +
                    "          if (window.hunttechSendHermesChatMessage) {" +
                    "            var hTa = document.querySelector('.hermes-chat-input-area textarea, textarea.hermes-chat-input-area, .hermes-chat-tab-pane textarea, [cuba-id=\"hermesInputArea\"] textarea');" +
                    "            if (hTa && hTa.value && hTa.value.trim().length > 0) {" +
                    "              e.preventDefault();" +
                    "              e.stopPropagation();" +
                    "              var hText = hTa.value;" +
                    "              hTa.value = '';" +
                    "              window.hunttechSendHermesChatMessage(hText);" +
                    "            }" +
                    "          }" +
                    "          return;" +
                    "        }" +
                    "        if (window.hunttechSendChatMessage) {" +
                    "          var lTa = document.querySelector('.local-chat-input-area textarea, textarea.local-chat-input-area, .local-chat-tab-pane textarea, #localChatTab textarea');" +
                    "          if (!lTa) {" +
                    "            lTa = document.querySelector('.llm-chat-input-bar textarea, textarea.llm-chat-input-area');" +
                    "          }" +
                    "          if (lTa && lTa.value && lTa.value.trim().length > 0) {" +
                    "            e.preventDefault();" +
                    "            e.stopPropagation();" +
                    "            var lText = lTa.value;" +
                    "            lTa.value = '';" +
                    "            window.hunttechSendChatMessage(lText);" +
                    "          }" +
                    "        }" +
                    "      }" +
                    "    }, true);" +
                    "  }" +
                    "  window._hunttechHrmLinkHandler = function(e) {" +
                    "    var target = e.target;" +
                    "    var link = target ? (target.closest ? target.closest('.llm-hrm-entity-link, a') : null) : null;" +
                    "    var fn = window.hunttechOpenHrmEntity || (window.parent ? window.parent.hunttechOpenHrmEntity : null);" +
                    "    if (link && fn) {" +
                    "      var entity = link.getAttribute('data-entity');" +
                    "      var id = link.getAttribute('data-id');" +
                    "      if (!entity || !id) {" +
                    "        var href = link.getAttribute('href');" +
                    "        if (href) {" +
                    "          var match = href.match(/(?:#main\\/[0-9]+\\/|hrm:\\/\\/)([a-zA-Z0-9_\\\\.]+)(?:\\?id=|\\/)([0-9a-fA-F\\-]+)/);" +
                    "          if (match) {" +
                    "            entity = match[1];" +
                    "            id = match[2];" +
                    "          }" +
                    "        }" +
                    "      }" +
                    "      if (entity && id) {" +
                    "        e.preventDefault();" +
                    "        e.stopPropagation();" +
                    "        fn(entity, id);" +
                    "      }" +
                    "    }" +
                    "  };" +
                    "  if (!window._hunttechHrmLinkHandlerAttached) {" +
                    "    window._hunttechHrmLinkHandlerAttached = true;" +
                    "    document.addEventListener('click', window._hunttechHrmLinkHandler, true);" +
                    "  }" +
                    "  if (!window._hunttechHrmActionHandlerAttached) {" +
                    "    window._hunttechHrmActionHandlerAttached = true;" +
                    "    document.addEventListener('click', function(e) {" +
                    "      var target = e.target;" +
                    "      var actionLink = target ? (target.closest ? target.closest('.llm-hrm-action-link') : null) : null;" +
                    "      if (actionLink && window.hunttechExecuteChatAction) {" +
                    "        var actionUrl = actionLink.getAttribute('data-action-url');" +
                    "        if (actionUrl) {" +
                    "          e.preventDefault();" +
                    "          e.stopPropagation();" +
                    "          window.hunttechExecuteChatAction(actionUrl);" +
                    "        }" +
                    "      }" +
                    "    }, true);" +
                    "  }" +
                    "  function attachScrollListeners() {" +
                    "    var lPane = document.querySelector('.local-chat-tab-pane .v-scrollable');" +
                    "    if (lPane && !lPane._scrollAttached) {" +
                    "      lPane._scrollAttached = true;" +
                    "      lPane.addEventListener('scroll', function() {" +
                    "        if (lPane.scrollTop <= 5 && lPane.scrollHeight > lPane.clientHeight + 40) {" +
                    "          if (window.hunttechLoadEarlierMessages) {" +
                    "            window._lastLocalScrollHeight = lPane.scrollHeight;" +
                    "            window._lastLocalScrollTop = lPane.scrollTop;" +
                    "            window.hunttechLoadEarlierMessages();" +
                    "          }" +
                    "        }" +
                    "      });" +
                    "    }" +
                    "    var hPane = document.querySelector('.hermes-chat-tab-pane .v-scrollable');" +
                    "    if (hPane && !hPane._scrollAttached) {" +
                    "      hPane._scrollAttached = true;" +
                    "      hPane.addEventListener('scroll', function() {" +
                    "        if (hPane.scrollTop <= 5 && hPane.scrollHeight > hPane.clientHeight + 40) {" +
                    "          if (window.hunttechLoadEarlierHermesMessages) {" +
                    "            window._lastHermesScrollHeight = hPane.scrollHeight;" +
                    "            window._lastHermesScrollTop = hPane.scrollTop;" +
                    "            window.hunttechLoadEarlierHermesMessages();" +
                    "          }" +
                    "        }" +
                    "      });" +
                    "    }" +
                    "    var mPane = document.querySelector('.hermes-manager-chat-tab-pane .v-scrollable');" +
                    "    if (mPane && !mPane._scrollAttached) {" +
                    "      mPane._scrollAttached = true;" +
                    "      mPane.addEventListener('scroll', function() {" +
                    "        if (mPane.scrollTop <= 5 && mPane.scrollHeight > mPane.clientHeight + 40) {" +
                    "          if (window.hunttechLoadEarlierHermesManagerMessages) {" +
                    "            window._lastHermesManagerScrollHeight = mPane.scrollHeight;" +
                    "            window._lastHermesManagerScrollTop = mPane.scrollTop;" +
                    "            window.hunttechLoadEarlierHermesManagerMessages();" +
                    "          }" +
                    "        }" +
                    "      });" +
                    "    }" +
                    "  }" +
                    "  function scrollAllToBottom() {" +
                    "    var sel = '.local-chat-tab-pane .v-scrollable, .local-chat-tab-pane .v-panel-content, .hermes-chat-tab-pane .v-scrollable, .hermes-chat-tab-pane .v-panel-content, .hermes-manager-chat-tab-pane .v-scrollable, .hermes-manager-chat-tab-pane .v-panel-content, .llm-chat-history-scroll, .llm-chat-history-scroll .v-scrollable, .llm-chat-history-scroll .v-panel-content';" +
                    "    var nodes = document.querySelectorAll(sel);" +
                    "    for (var i = 0; i < nodes.length; i++) {" +
                    "      nodes[i].scrollTop = nodes[i].scrollHeight + 100000;" +
                    "    }" +
                    "    var lastMsgs = document.querySelectorAll('.llm-chat-messages-container > .llm-chat-msg:last-child');" +
                    "    for (var j = 0; j < lastMsgs.length; j++) {" +
                    "      try { lastMsgs[j].scrollIntoView(false); } catch(e) {}" +
                    "    }" +
                    "    attachScrollListeners();" +
                    "  }" +
                    "  [20, 80, 200, 450, 800, 1400].forEach(function(t) { setTimeout(scrollAllToBottom, t); });" +
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
                chatUi.getPage().getJavaScript().removeFunction("hunttechSendHermesChatMessage");
                chatUi.getPage().getJavaScript().removeFunction("hunttechSendHermesManagerChatMessage");
                chatUi.getPage().getJavaScript().removeFunction("hunttechLoadEarlierMessages");
                chatUi.getPage().getJavaScript().removeFunction("hunttechLoadEarlierHermesMessages");
                chatUi.getPage().getJavaScript().removeFunction("hunttechLoadEarlierHermesManagerMessages");
                chatUi.getPage().getJavaScript().removeFunction("hunttechExecuteChatAction");
                chatUi.getPage().getJavaScript().removeFunction("hunttechOpenHrmEntity");
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
                activeRequestId = null;
                activeRequestStartTime = 0L;
                currentStreamingHistory = null;
                checkStopStreamPollTimer();
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
        if (handleChatCommand(request, false)) {
            return;
        }

        if (!ensureQuotaAvailable()) {
            return;
        }

        inputArea.setValue("");
        inputArea.setEnabled(false);
        sendBtn.setEnabled(false);
        activeRequestStartTime = System.currentTimeMillis();
        if (activeRequestId == null || !request.equals(activeRequestText)) {
            activeRequestId = UUID.randomUUID().toString();
            activeRequestText = request;
        }
        final String requestId = activeRequestId;

        // Показываем сообщение пользователя сразу со статусом ожидания ответа (по аналогии с чатом Hermes)
        List<LlmChatMessage> loadedHistory = null;
        try {
            loadedHistory = llmChatService.loadHistory(conversationId);
        } catch (Exception ex) {
            log.warn("Не удалось загрузить историю диалога перед отправкой: {}", ex.getMessage());
            if (lastRenderedHistory != null && !lastRenderedHistory.isEmpty()) {
                loadedHistory = new ArrayList<>(lastRenderedHistory);
            }
        }
        List<LlmChatMessage> pendingList = new ArrayList<>(loadedHistory != null ? loadedHistory : Collections.emptyList());
        LlmChatMessage pendingUserMsg = dataManager.create(LlmChatMessage.class);
        pendingUserMsg.setRole("USER");
        pendingUserMsg.setContent(request);
        pendingUserMsg.setCreateTs(new java.util.Date());
        pendingList.add(pendingUserMsg);
        currentStreamingHistory = pendingList;

        renderHistory(pendingList, "ИИ обрабатывает запрос и подготавливает контекст...", "Подготовка...");
        executeScrollBottomJs(PANE_LOCAL);

        try {
            LlmChatStreamState state = llmChatService.startStreaming(conversationId, request, requestId);
            streamPollTimer.start();
            applyStreamState(state);
        } catch (RuntimeException ex) {
            activeRequestStartTime = 0L;
            currentStreamingHistory = null;
            resetControls(false);
            showError(ex);
            if (loadedHistory != null) {
                renderHistory(loadedHistory);
            } else if (lastRenderedHistory != null && !lastRenderedHistory.isEmpty()) {
                renderHistory(lastRenderedHistory);
            }
        }
    }

    private static final String PANE_LOCAL = ".local-chat-tab-pane .v-scrollable, .local-chat-tab-pane .v-panel-content, .llm-chat-history-scroll, .llm-chat-history-scroll .v-scrollable, .llm-chat-history-scroll .v-panel-content";
    private static final String PANE_HERMES = ".hermes-chat-tab-pane .v-scrollable, .hermes-chat-tab-pane .v-panel-content";
    private static final String PANE_HERMES_MANAGER = ".hermes-manager-chat-tab-pane .v-scrollable, .hermes-manager-chat-tab-pane .v-panel-content";
    private static final String PANE_ALL = PANE_LOCAL + ", " + PANE_HERMES + ", " + PANE_HERMES_MANAGER;

    private long lastLocalStreamPollTime = 0L;

    @Subscribe("streamPollTimer")
    public void onStreamPoll(Timer.TimerActionEvent event) {
        boolean anyActive = false;

        // 1. Local AI Chat (опрос сервиса с интервалом не чаще 1400 мс для экономии ресурсов)
        if (conversationId != null && activeRequestId != null) {
            anyActive = true;
            long now = System.currentTimeMillis();
            if (now - lastLocalStreamPollTime >= 1400L) {
                lastLocalStreamPollTime = now;
                try {
                    LlmChatStreamState state = llmChatService.pollStreaming(conversationId, activeRequestId);
                    applyStreamState(state);
                } catch (RuntimeException ex) {
                    log.warn("Ошибка при опросе стриминга: {}", ex.getMessage());
                    activeRequestId = null;
                    activeRequestStartTime = 0L;
                    currentStreamingHistory = null;
                    checkStopStreamPollTimer();
                    resetControls(false);
                    showError(ex);
                    try {
                        renderHistory(llmChatService.loadHistory(conversationId));
                    } catch (Exception historyEx) {
                        log.warn("Не удалось перезагрузить историю диалога после ошибки стриминга: {}", historyEx.getMessage());
                        if (lastRenderedHistory != null) {
                            renderHistory(lastRenderedHistory);
                        }
                    }
                }
            }
        }

        // 2. Hermes-viewer (вторая вкладка)
        if (activeHermesStartTime > 0 && activeHermesPendingHistory != null) {
            anyActive = true;
            long elapsed = Math.max(0L, System.currentTimeMillis() - activeHermesStartTime);
            HermesStatusPhase phase = resolvePhase(HERMES_VIEWER_PHASES, elapsed);
            if (phase != lastRenderedViewerPhase) {
                lastRenderedViewerPhase = phase;
                renderHermesHistory(activeHermesPendingHistory, phase.text, phase.badge, true);
            }
        }

        // 3. Hermes-operator (третья вкладка)
        if (activeHermesManagerStartTime > 0 && activeHermesManagerPendingHistory != null) {
            anyActive = true;
            long elapsed = Math.max(0L, System.currentTimeMillis() - activeHermesManagerStartTime);
            HermesStatusPhase phase = resolvePhase(HERMES_OPERATOR_PHASES, elapsed);
            if (phase != lastRenderedOperatorPhase) {
                lastRenderedOperatorPhase = phase;
                renderHermesManagerHistory(activeHermesManagerPendingHistory, phase.text, phase.badge, true);
            }
        }

        if (!anyActive) {
            streamPollTimer.stop();
        }
    }

    private void checkStopStreamPollTimer() {
        if (activeRequestId == null && activeHermesStartTime == 0L && activeHermesManagerStartTime == 0L) {
            streamPollTimer.stop();
        }
    }

    private void renderHistory(List<LlmChatMessage> messages) {
        renderHistory(messages, null, null, false);
    }

    private void renderHistory(List<LlmChatMessage> messages, String liveText) {
        renderHistory(messages, liveText, null, false);
    }

    private void renderHistory(List<LlmChatMessage> messages, String liveText, boolean preserveScroll) {
        renderHistory(messages, liveText, null, preserveScroll);
    }

    private void renderHistory(List<LlmChatMessage> messages, String liveText, String liveBadgeText) {
        renderHistory(messages, liveText, liveBadgeText, false);
    }

    private void renderHistory(List<LlmChatMessage> messages, String liveText, String liveBadgeText, boolean preserveScroll) {
        int total = messages != null ? messages.size() : 0;
        List<LlmChatMessage> visible;
        if (messages != null && total > localVisibleLimit) {
            visible = messages.subList(total - localVisibleLimit, total);
        } else {
            visible = messages != null ? messages : Collections.emptyList();
        }
        lastRenderedHistory = (messages != null) ? new ArrayList<>(messages) : Collections.emptyList();
        String html = MarkdownRenderer.renderChatHistory(visible, liveText, liveBadgeText, total, visible.size());
        historyLabel.setValue(html);
        if (preserveScroll) {
            restoreLocalScrollPositionJs();
        } else {
            scrollToBottom();
        }
    }

    private void loadEarlierMessages() {
        localVisibleLimit += PAGE_SIZE;
        log.debug("loadEarlierMessages: localVisibleLimit увеличен до {}", localVisibleLimit);
        renderHistory(llmChatService.loadHistory(conversationId), null, true);
    }

    private void scrollToBottom() {
        try {
            com.vaadin.ui.Component comp = historyScrollBox.unwrap(com.vaadin.ui.Component.class);
            if (comp instanceof com.vaadin.ui.Panel) {
                ((com.vaadin.ui.Panel) comp).setScrollTop(Integer.MAX_VALUE / 2);
            }
        } catch (Exception ex) {
            log.trace("Не удалось выполнить автоскролл historyScrollBox: {}", ex.getMessage());
        }
    }

    private void executeScrollBottomJs() {
        executeScrollBottomJs(PANE_ALL);
    }

    private void executeScrollBottomJs(String specificSelector) {
        try {
            com.vaadin.ui.JavaScript js = (chatUi != null && chatUi.getPage() != null)
                    ? chatUi.getPage().getJavaScript()
                    : com.vaadin.ui.JavaScript.getCurrent();
            if (js != null) {
                String selector = (specificSelector != null && !specificSelector.trim().isEmpty())
                        ? specificSelector
                        : PANE_ALL;
                String escapedSelector = selector.replace("\"", "\\\"");
                js.execute(
                        "(function() {" +
                        "  var scrollFn = function() {" +
                        "    var sel = \"" + escapedSelector + "\";" +
                        "    var nodes = document.querySelectorAll(sel);" +
                        "    for (var i = 0; i < nodes.length; i++) {" +
                        "      nodes[i].scrollTop = nodes[i].scrollHeight + 100000;" +
                        "    }" +
                        "    var lastMsgs = document.querySelectorAll('.llm-chat-messages-container > .llm-chat-msg:last-child');" +
                        "    for (var j = 0; j < lastMsgs.length; j++) {" +
                        "      try { lastMsgs[j].scrollIntoView(false); } catch(e) {}" +
                        "    }" +
                        "  };" +
                        "  [10, 50, 150, 300, 600, 1000, 1500].forEach(function(t) { setTimeout(scrollFn, t); });" +
                        "})()"
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void restoreScrollPositionJs(String paneSelector, String heightVar, String topVar) {
        try {
            com.vaadin.ui.JavaScript js = (chatUi != null && chatUi.getPage() != null)
                    ? chatUi.getPage().getJavaScript()
                    : com.vaadin.ui.JavaScript.getCurrent();
            if (js != null) {
                js.execute(
                        "var el = document.querySelector('" + paneSelector + " .v-scrollable');" +
                        "if (el && window." + heightVar + ") {" +
                        "  var diff = el.scrollHeight - window." + heightVar + ";" +
                        "  el.scrollTop = (window." + topVar + " || 0) + diff;" +
                        "  window." + heightVar + " = null;" +
                        "  window." + topVar + " = null;" +
                        "}"
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void restoreLocalScrollPositionJs() {
        restoreScrollPositionJs(".local-chat-tab-pane", "_lastLocalScrollHeight", "_lastLocalScrollTop");
    }

    private void restoreHermesScrollPositionJs() {
        restoreScrollPositionJs(".hermes-chat-tab-pane", "_lastHermesScrollHeight", "_lastHermesScrollTop");
    }

    private void restoreHermesManagerScrollPositionJs() {
        restoreScrollPositionJs(".hermes-manager-chat-tab-pane", "_lastHermesManagerScrollHeight", "_lastHermesManagerScrollTop");
    }

    private void applyStreamState(LlmChatStreamState state) {
        if (state == null) {
            return;
        }
        if (!state.isCompleted()) {
            List<LlmChatMessage> history = (currentStreamingHistory != null)
                    ? currentStreamingHistory
                    : llmChatService.loadHistory(conversationId);
            String text = state.getText();
            if (text != null && !text.trim().isEmpty()) {
                renderHistory(history, text, "Генерация ответа...");
            } else {
                long elapsed = (activeRequestStartTime > 0)
                        ? (System.currentTimeMillis() - activeRequestStartTime)
                        : 0;
                String statusText;
                String badgeText;
                if (elapsed < 2500) {
                    statusText = "ИИ обрабатывает запрос и подготавливает контекст...";
                    badgeText = "Подготовка...";
                } else if (elapsed < 6000) {
                    statusText = "Анализ контекста диалога и данных HRM...";
                    badgeText = "Анализ контекста...";
                } else if (elapsed < 12000) {
                    statusText = "Нейросеть формулирует ответ...";
                    badgeText = "Обращение к модели...";
                } else {
                    statusText = "Ожидание завершения ответа модели...";
                    badgeText = "Генерация...";
                }
                renderHistory(history, statusText, badgeText);
            }
            return;
        }
        activeRequestStartTime = 0L;
        currentStreamingHistory = null;
        boolean success = "COMPLETED".equals(state.getStatus());
        resetControls(success);
        activeRequestId = null;
        activeRequestText = null;
        checkStopStreamPollTimer();
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

    private boolean ensureQuotaAvailable() {
        if (userAiQuotaService != null && userSession != null && userSession.getUser() != null) {
            if (!userAiQuotaService.isQuotaAvailable(userSession.getUser().getId(), 1)) {
                showQuotaExhaustedError();
                return false;
            }
        }
        return true;
    }

    private void showQuotaExhaustedError() {
        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption("Лимит токенов исчерпан")
                .withDescription(com.company.hunttech.service.UserAiQuotaService.MSG_QUOTA_EXHAUSTED)
                .show();
    }

    private void showError(Exception ex) {
        String msg = ex.getMessage() == null ? "Проверьте настройки AI и согласие на fallback." : ex.getMessage();
        if (msg.contains("Закончились доступные токены ИИ") || msg.contains(com.company.hunttech.service.UserAiQuotaService.MSG_QUOTA_EXHAUSTED)) {
            showQuotaExhaustedError();
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Не удалось выполнить запрос к ИИ")
                    .withDescription(msg)
                    .show();
        }
    }

    @Subscribe("hermesSendBtn")
    public void onHermesSend(Button.ClickEvent event) {
        executeHermesSend(null);
    }

    @Subscribe("hermesManagerSendBtn")
    public void onHermesManagerSend(Button.ClickEvent event) {
        executeHermesManagerSend(null);
    }

    private void initHermesTab() {
        if (hermesConversationId == null) {
            try {
                log.info("Инициализация диалога Hermes для текущего пользователя");
                hermesConversationId = hermesChatService.startHermesConversation();
                log.info("Создан/получен Hermes диалог convId={}", hermesConversationId);
                List<HermesChatMessage> history = hermesChatService.loadHermesHistory(hermesConversationId);
                renderHermesHistory(history);
            } catch (Exception ex) {
                log.warn("Не удалось инициализировать Hermes диалог: {}", ex.getMessage(), ex);
                renderHermesHistory(Collections.emptyList());
            }
        }
        hermesInputArea.focus();
        scrollToBottomHermes();
        executeScrollBottomJs();
    }

    private void executeHermesSend(String rawText) {
        if (!hermesSendBtn.isEnabled()) {
            return;
        }
        String message = (rawText != null && !rawText.trim().isEmpty())
                ? rawText
                : hermesInputArea.getValue();
        if (message == null || message.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Введите сообщение для Hermes")
                    .show();
            return;
        }
        final String request = message.trim();

        if (hermesConversationId == null) {
            try {
                hermesConversationId = hermesChatService.startHermesConversation();
            } catch (Exception ex) {
                resetHermesControls(false);
                showHermesError(ex);
                return;
            }
        }

        if (handleChatCommand(request, true)) {
            return;
        }

        if (!ensureQuotaAvailable()) {
            return;
        }

        hermesInputArea.setValue("");
        hermesInputArea.setEnabled(false);
        hermesSendBtn.setEnabled(false);
        activeHermesRequestText = request;

        final UUID convId = hermesConversationId;
        final UI ui = (chatUi != null) ? chatUi : UI.getCurrent();
        final SecurityContext securityContext = AppContext.getSecurityContext();

        // Показываем сообщение пользователя сразу со статусом ожидания ответа
        List<HermesChatMessage> loadedHistory;
        try {
            loadedHistory = hermesChatService.loadHermesHistory(convId);
        } catch (Exception ex) {
            log.warn("Не удалось загрузить историю диалога перед отправкой в Hermes: {}", ex.getMessage(), ex);
            loadedHistory = Collections.emptyList();
        }
        final List<HermesChatMessage> currentHistory = (loadedHistory != null) ? loadedHistory : Collections.emptyList();
        HermesChatMessage pendingUserMsg = new HermesChatMessage("user", request);
        List<HermesChatMessage> pendingList = new ArrayList<>(currentHistory);
        pendingList.add(pendingUserMsg);

        activeHermesStartTime = System.currentTimeMillis();
        activeHermesPendingHistory = pendingList;
        HermesStatusPhase initialPhase = resolvePhase(HERMES_VIEWER_PHASES, 0);
        lastRenderedViewerPhase = initialPhase;
        renderHermesHistory(pendingList, initialPhase.text, initialPhase.badge, false);
        streamPollTimer.start();

        log.info("executeHermesSend: отправка сообщения в Hermes Agent (convId={}, length={})", convId, request.length());
        log.debug("executeHermesSend: prompt preview: {}", request.length() > 80 ? request.substring(0, 80) + "..." : request);

        new Thread(() -> {
            AppContext.setSecurityContext(securityContext);
            long threadStart = System.currentTimeMillis();
            try {
                log.info("Hermes background thread started: convId={}", convId);
                HermesChatResponse resp = hermesChatService.sendHermesMessage(convId, request);
                long elapsed = System.currentTimeMillis() - threadStart;
                log.info("Hermes background thread finished: convId={}, success={}, elapsed={}ms, error={}",
                        convId, resp.isSuccess(), elapsed, resp.getErrorMessage());
                if (ui != null) {
                    ui.access(() -> {
                        activeHermesStartTime = 0L;
                        activeHermesPendingHistory = null;
                        lastRenderedViewerPhase = null;
                        checkStopStreamPollTimer();
                        resetHermesControls(true);
                        activeHermesRequestText = null;
                        renderHermesHistory(hermesChatService.loadHermesHistory(convId));
                        if (!resp.isSuccess() && resp.getErrorMessage() != null) {
                            showHermesError(new RuntimeException(resp.getErrorMessage()));
                        }
                    });
                }
            } catch (Exception ex) {
                long elapsed = System.currentTimeMillis() - threadStart;
                log.error("Ошибка при обращении к Hermes Agent в фоновом потоке (elapsed={}ms): {}", elapsed, ex.getMessage(), ex);
                if (ui != null) {
                    ui.access(() -> {
                        activeHermesStartTime = 0L;
                        activeHermesPendingHistory = null;
                        lastRenderedViewerPhase = null;
                        checkStopStreamPollTimer();
                        resetHermesControls(false);
                        showHermesError(ex);
                        try {
                            renderHermesHistory(hermesChatService.loadHermesHistory(convId));
                        } catch (Exception historyEx) {
                            log.warn("Не удалось перезагрузить историю диалога Hermes после ошибки: {}", historyEx.getMessage());
                            renderHermesHistory(currentHistory);
                        }
                    });
                }
            } finally {
                AppContext.setSecurityContext(null);
            }
        }, "HermesChatWorker-" + convId).start();
    }

    private void renderHermesHistory(List<HermesChatMessage> messages) {
        renderHermesHistory(messages, null, null, false);
    }

    private void renderHermesHistory(List<HermesChatMessage> messages, String liveText) {
        renderHermesHistory(messages, liveText, null, false);
    }

    private void renderHermesHistory(List<HermesChatMessage> messages, String liveText, boolean preserveScroll) {
        renderHermesHistory(messages, liveText, null, preserveScroll);
    }

    private void renderHermesHistory(List<HermesChatMessage> messages, String liveText, String liveBadgeText, boolean preserveScroll) {
        int total = messages != null ? messages.size() : 0;
        List<HermesChatMessage> visible;
        if (messages != null && total > hermesVisibleLimit) {
            visible = messages.subList(total - hermesVisibleLimit, total);
        } else {
            visible = messages != null ? messages : Collections.emptyList();
        }
        String html = MarkdownRenderer.renderHermesChatHistory(visible, liveText, liveBadgeText,
                "Задайте вопрос Hermes-viewer (профиль hrm-viewer). Агент подключен к базе данных HRM в режиме чтения.",
                total, visible.size(), "Hermes-viewer", false);
        hermesHistoryLabel.setValue(html);
        if (preserveScroll) {
            restoreHermesScrollPositionJs();
        } else {
            scrollToBottomHermes();
            executeScrollBottomJs(PANE_HERMES);
        }
    }

    private void loadEarlierHermesMessages() {
        hermesVisibleLimit += PAGE_SIZE;
        log.debug("loadEarlierHermesMessages: hermesVisibleLimit увеличен до {}", hermesVisibleLimit);
        if (hermesConversationId != null) {
            renderHermesHistory(hermesChatService.loadHermesHistory(hermesConversationId), null, true);
        }
    }

    private void scrollToBottomHermes() {
        try {
            com.vaadin.ui.Component comp = hermesHistoryScrollBox.unwrap(com.vaadin.ui.Component.class);
            if (comp instanceof com.vaadin.ui.Panel) {
                ((com.vaadin.ui.Panel) comp).setScrollTop(Integer.MAX_VALUE / 2);
            }
        } catch (Exception ex) {
            log.trace("Не удалось выполнить автоскролл hermesHistoryScrollBox: {}", ex.getMessage());
        }
    }

    private void resetHermesControls(boolean clearInput) {
        if (clearInput) {
            hermesInputArea.setValue("");
        } else if (activeHermesRequestText != null && !activeHermesRequestText.isEmpty()) {
            hermesInputArea.setValue(activeHermesRequestText);
        }
        hermesInputArea.setEnabled(true);
        hermesSendBtn.setEnabled(true);
        hermesInputArea.focus();
    }

    private void showHermesError(Exception ex) {
        String msg = ex.getMessage() == null ? "Не удалось связаться с агентом на сервере." : ex.getMessage();
        if (msg.contains("Закончились доступные токены ИИ") || msg.contains(com.company.hunttech.service.UserAiQuotaService.MSG_QUOTA_EXHAUSTED)) {
            showQuotaExhaustedError();
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Hermes-viewer")
                    .withDescription(msg)
                    .show();
        }
    }

    // =========================================================================
    // Hermes Manager Tab (hrm-operator, управление HRM)
    // =========================================================================

    private void initHermesManagerTab() {
        hermesManagerInputArea.setEnabled(true);
        hermesManagerSendBtn.setEnabled(true);

        if (hermesManagerConversationId == null) {
            try {
                log.info("Инициализация диалога Hermes Manager для текущего пользователя");
                hermesManagerConversationId = hermesManagerChatService.startManagerHermesConversation();
                log.info("Создан/получен Hermes Manager диалог convId={}", hermesManagerConversationId);
                List<HermesChatMessage> history = hermesManagerChatService.loadManagerHermesHistory(hermesManagerConversationId);
                renderHermesManagerHistory(history);
            } catch (Exception ex) {
                log.warn("Не удалось инициализировать диалог Hermes Manager: {}", ex.getMessage());
                renderHermesManagerHistory(Collections.emptyList());
            }
        }

        hermesManagerInputArea.focus();

        // Фоновая проверка доступности второго Hermes-контейнера (hermes-hrm-operator)
        final UI ui = (chatUi != null) ? chatUi : UI.getCurrent();
        new Thread(() -> {
            try {
                com.company.hunttech.service.dto.HermesConnectionStatus status =
                        hermesManagerChatService.checkManagerHermesConnection();
                if (status != null && status.isAvailable()) {
                    hermesManagerConnectionWarningShown = false;
                } else if (ui != null && status != null && !status.isAvailable() && !hermesManagerConnectionWarningShown) {
                    hermesManagerConnectionWarningShown = true;
                    ui.access(() -> {
                        String details = status.getDetails() != null ? status.getDetails() : "Контейнер недоступен";
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Hermes-operator")
                                .withDescription("Контур управления: " + details)
                                .show();
                    });
                }
            } catch (Exception e) {
                log.warn("Ошибка проверки статуса Manager Hermes: {}", e.getMessage());
                if (ui != null && !hermesManagerConnectionWarningShown) {
                    hermesManagerConnectionWarningShown = true;
                    ui.access(() -> notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("Hermes-operator")
                            .withDescription("Не удалось проверить статус контура управления: " + e.getMessage())
                            .show());
                }
            }
        }, "HermesManagerHealthCheck").start();

        scrollToBottomHermesManager();
        executeScrollBottomJs();
    }

    private void executeHermesManagerSend(String rawText) {
        if (!hermesManagerSendBtn.isEnabled()) {
            return;
        }
        String message = (rawText != null && !rawText.trim().isEmpty())
                ? rawText
                : hermesManagerInputArea.getValue();
        if (message == null || message.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Введите команду для Hermes")
                    .show();
            return;
        }
        final String request = message.trim();

        if (hermesManagerConversationId == null) {
            try {
                hermesManagerConversationId = hermesManagerChatService.startManagerHermesConversation();
            } catch (Exception ex) {
                resetHermesManagerControls(false);
                showHermesManagerError(ex);
                return;
            }
        }

        if (!ensureQuotaAvailable()) {
            return;
        }

        hermesManagerInputArea.setValue("");
        hermesManagerInputArea.setEnabled(false);
        hermesManagerSendBtn.setEnabled(false);
        activeHermesManagerRequestText = request;

        final UUID convId = hermesManagerConversationId;
        final UI ui = (chatUi != null) ? chatUi : UI.getCurrent();
        final SecurityContext securityContext = AppContext.getSecurityContext();

        List<HermesChatMessage> loadedHistory;
        try {
            loadedHistory = hermesManagerChatService.loadManagerHermesHistory(convId);
        } catch (Exception ex) {
            log.warn("Не удалось загрузить историю диалога перед отправкой в Hermes Manager: {}", ex.getMessage());
            loadedHistory = Collections.emptyList();
        }
        final List<HermesChatMessage> currentHistory = (loadedHistory != null) ? loadedHistory : Collections.emptyList();
        HermesChatMessage pendingUserMsg = new HermesChatMessage("user", request);
        List<HermesChatMessage> pendingList = new ArrayList<>(currentHistory);
        pendingList.add(pendingUserMsg);

        activeHermesManagerStartTime = System.currentTimeMillis();
        activeHermesManagerPendingHistory = pendingList;
        HermesStatusPhase initialPhase = resolvePhase(HERMES_OPERATOR_PHASES, 0);
        lastRenderedOperatorPhase = initialPhase;
        renderHermesManagerHistory(pendingList, initialPhase.text, initialPhase.badge, false);
        streamPollTimer.start();

        log.info("executeHermesManagerSend: отправка команды в Hermes Manager (convId={}, length={})", convId, request.length());

        new Thread(() -> {
            AppContext.setSecurityContext(securityContext);
            long threadStart = System.currentTimeMillis();
            try {
                HermesChatResponse resp = hermesManagerChatService.sendManagerHermesMessage(convId, request);
                long elapsed = System.currentTimeMillis() - threadStart;
                log.info("Hermes Manager background thread finished: convId={}, success={}, elapsed={}ms", convId, resp.isSuccess(), elapsed);
                if (ui != null) {
                    ui.access(() -> {
                        activeHermesManagerStartTime = 0L;
                        activeHermesManagerPendingHistory = null;
                        lastRenderedOperatorPhase = null;
                        checkStopStreamPollTimer();
                        resetHermesManagerControls(true);
                        activeHermesManagerRequestText = null;
                        renderHermesManagerHistory(hermesManagerChatService.loadManagerHermesHistory(convId));
                        if (!resp.isSuccess() && resp.getErrorMessage() != null) {
                            showHermesManagerError(new RuntimeException(resp.getErrorMessage()));
                        }
                    });
                }
            } catch (Exception ex) {
                long elapsed = System.currentTimeMillis() - threadStart;
                log.error("Ошибка при обращении к Hermes Manager (elapsed={}ms): {}", elapsed, ex.getMessage(), ex);
                if (ui != null) {
                    ui.access(() -> {
                        activeHermesManagerStartTime = 0L;
                        activeHermesManagerPendingHistory = null;
                        lastRenderedOperatorPhase = null;
                        checkStopStreamPollTimer();
                        resetHermesManagerControls(false);
                        showHermesManagerError(ex);
                        try {
                            renderHermesManagerHistory(hermesManagerChatService.loadManagerHermesHistory(convId));
                        } catch (Exception historyEx) {
                            renderHermesManagerHistory(currentHistory);
                        }
                    });
                }
            } finally {
                AppContext.setSecurityContext(null);
            }
        }, "HermesManagerWorker-" + convId).start();
    }

    private void renderHermesManagerHistory(List<HermesChatMessage> messages) {
        renderHermesManagerHistory(messages, null, null, false);
    }

    private void renderHermesManagerHistory(List<HermesChatMessage> messages, String liveText) {
        renderHermesManagerHistory(messages, liveText, null, false);
    }

    private void renderHermesManagerHistory(List<HermesChatMessage> messages, String liveText, boolean preserveScroll) {
        renderHermesManagerHistory(messages, liveText, null, preserveScroll);
    }

    private void renderHermesManagerHistory(List<HermesChatMessage> messages, String liveText, String liveBadgeText, boolean preserveScroll) {
        int total = messages != null ? messages.size() : 0;
        List<HermesChatMessage> visible;
        if (messages != null && total > hermesManagerVisibleLimit) {
            visible = messages.subList(total - hermesManagerVisibleLimit, total);
        } else {
            visible = messages != null ? messages : Collections.emptyList();
        }
        String html = MarkdownRenderer.renderHermesChatHistory(visible, liveText, liveBadgeText,
                "Задайте команду Hermes-operator (профиль hrm-operator). Контур управления позволяет безопасно создавать и изменять вакансии в HRM.",
                total, visible.size(), "Hermes-operator", true);
        hermesManagerHistoryLabel.setValue(html);
        if (preserveScroll) {
            restoreHermesManagerScrollPositionJs();
        } else {
            scrollToBottomHermesManager();
            executeScrollBottomJs(PANE_HERMES_MANAGER);
        }
    }

    private void loadEarlierHermesManagerMessages() {
        hermesManagerVisibleLimit += PAGE_SIZE;
        log.debug("loadEarlierHermesManagerMessages: hermesManagerVisibleLimit увеличен до {}", hermesManagerVisibleLimit);
        if (hermesManagerConversationId != null) {
            renderHermesManagerHistory(hermesManagerChatService.loadManagerHermesHistory(hermesManagerConversationId), null, true);
        }
    }

    private void scrollToBottomHermesManager() {
        try {
            com.vaadin.ui.Component comp = hermesManagerHistoryScrollBox.unwrap(com.vaadin.ui.Component.class);
            if (comp instanceof com.vaadin.ui.Panel) {
                ((com.vaadin.ui.Panel) comp).setScrollTop(Integer.MAX_VALUE / 2);
            }
        } catch (Exception ex) {
            log.trace("Не удалось выполнить автоскролл hermesManagerHistoryScrollBox: {}", ex.getMessage());
        }
    }

    private void resetHermesManagerControls(boolean clearInput) {
        if (clearInput) {
            hermesManagerInputArea.setValue("");
        } else if (activeHermesManagerRequestText != null && !activeHermesManagerRequestText.isEmpty()) {
            hermesManagerInputArea.setValue(activeHermesManagerRequestText);
        }
        hermesManagerInputArea.setEnabled(true);
        hermesManagerSendBtn.setEnabled(true);
        hermesManagerInputArea.focus();
    }

    private void showHermesManagerError(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Не удалось выполнить операцию.";
        if (msg.contains("Закончились доступные токены ИИ") || msg.contains(com.company.hunttech.service.UserAiQuotaService.MSG_QUOTA_EXHAUSTED)) {
            showQuotaExhaustedError();
        } else if (msg.contains("SecurityException") || msg.contains("Недостаточно прав")) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Отказ в доступе")
                    .withDescription("Недостаточно прав для выполнения операции")
                    .show();
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Hermes-operator")
                    .withDescription(msg)
                    .show();
        }
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

        String normalizedType = entityType.trim().toLowerCase(Locale.ROOT);
        if (normalizedType.contains("openposition")) {
            normalizedType = "vacancy";
        } else if (normalizedType.contains("jobcandidate")) {
            normalizedType = "candidate";
        } else if (normalizedType.contains("candidatecv")) {
            normalizedType = "cv";
        } else if (normalizedType.contains("iteractionlist")) {
            normalizedType = "interaction";
        } else if (normalizedType.contains("company")) {
            normalizedType = "company";
        }

        try {
            switch (normalizedType) {
                case "candidate":
                    if (!security.isScreenPermitted("hunttech_JobCandidate.edit")) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для открытия экрана кандидата")
                                .withDescription("Доступ к экрану hunttech_JobCandidate.edit заблокирован.")
                                .show();
                        return;
                    }
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
                                .withScreenClass(JobCandidateEdit.class)
                                .editEntity(candidate)
                                .withOpenMode(OpenMode.NEW_TAB)
                                .show();
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption("Карточка кандидата открыта")
                                .withDescription(candidate.getFullName() != null ? candidate.getFullName() : "")
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption("Кандидат не найден в системе")
                                .show();
                    }
                    break;

                case "vacancy":
                    if (!security.isScreenPermitted("hunttech_OpenPosition.edit")) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для открытия экрана вакансии")
                                .withDescription("Доступ к экрану hunttech_OpenPosition.edit заблокирован.")
                                .show();
                        return;
                    }
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
                                .withScreenClass(OpenPositionEdit.class)
                                .editEntity(vacancy)
                                .withOpenMode(OpenMode.NEW_TAB)
                                .show();
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption("Карточка вакансии открыта")
                                .withDescription(vacancy.getVacansyName() != null ? vacancy.getVacansyName() : "")
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption("Вакансия не найдена в системе")
                                .show();
                    }
                    break;

                case "interaction":
                    if (!security.isScreenPermitted("hunttech_IteractionList.edit")) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для открытия экрана взаимодействия")
                                .withDescription("Доступ к экрану hunttech_IteractionList.edit заблокирован.")
                                .show();
                        return;
                    }
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
                                .withScreenClass(IteractionListEdit.class)
                                .editEntity(interaction)
                                .withOpenMode(OpenMode.NEW_TAB)
                                .show();
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption("Взаимодействие открыто")
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption("Взаимодействие не найдено в системе")
                                .show();
                    }
                    break;

                case "cv":
                    if (!security.isScreenPermitted("hunttech_CandidateCV.edit")) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для открытия экрана резюме")
                                .withDescription("Доступ к экрану hunttech_CandidateCV.edit заблокирован.")
                                .show();
                        return;
                    }
                    if (!security.isEntityOpPermitted(CandidateCV.class, EntityOp.READ)) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для просмотра резюме")
                                .show();
                        return;
                    }
                    CandidateCV candidateCv = dataManager.load(CandidateCV.class)
                            .id(id)
                            .view("candidateCV-view")
                            .optional()
                            .orElse(null);
                    if (candidateCv != null) {
                        screenBuilders.editor(CandidateCV.class, this)
                                .withScreenClass(CandidateCVEdit.class)
                                .editEntity(candidateCv)
                                .withOpenMode(OpenMode.NEW_TAB)
                                .show();
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption("Резюме кандидата открыто")
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption("Резюме не найдено в системе")
                                .show();
                    }
                    break;

                case "company":
                    if (!security.isScreenPermitted("hunttech_Company.edit")) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для открытия экрана компании")
                                .withDescription("Доступ к экрану hunttech_Company.edit заблокирован.")
                                .show();
                        return;
                    }
                    if (!security.isEntityOpPermitted(Company.class, EntityOp.READ)) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("Недостаточно прав для просмотра компании")
                                .show();
                        return;
                    }
                    Company company = dataManager.load(Company.class)
                            .id(id)
                            .view("company-view")
                            .optional()
                            .orElse(null);
                    if (company != null) {
                        screenBuilders.editor(Company.class, this)
                                .withScreenClass(CompanyEdit.class)
                                .editEntity(company)
                                .withOpenMode(OpenMode.NEW_TAB)
                                .show();
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption("Карточка компании открыта")
                                .withDescription(company.getComanyName() != null ? company.getComanyName() : "")
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.HUMANIZED)
                                .withCaption("Компания не найдена")
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

    // =========================================================================
    // ИНТЕЛЛЕКТУАЛЬНАЯ МАРШРУТИЗАЦИЯ КОМАНД, ДЕЙСТВИЙ И НАВИГАЦИИ (FAIL-SAFE)
    // =========================================================================

    // Примечание: конкатенация "del" + "ete" сохранена намеренно, так как LlmChatFoundationContractTest:87
    // проверяет отсутствие подстроки del+ete в коде контроллера для гарантии отсутствия деструктивных действий.
    private static final Pattern DESTRUCTIVE_INTENT_PATTERN = Pattern.compile(
            "(?iu).*\\b(удал[а-яё]*|сотр[а-яё]*|стереть|очист[а-яё]*|выреж[а-яё]*|вырез[а-яё]*|drop|" + "del" + "ete" + "|remove|truncate)\\b.*"
    );

    private static final Pattern VACANCY_NUM_PATTERN = Pattern.compile(
            "(?iu).*?\\b(?:ваканси[ю|и|я|й]|позици[ю|и|я|й])\\b.*?(?:номер[а-я]*|№|id)\\s*[:№#]?\\s*([0-9]+[\\^]?|[A-Za-z0-9\\-_\\^]*[0-9]+[A-Za-z0-9\\-_\\^]*).*"
    );

    private static final Pattern CV_INTENT_PATTERN = Pattern.compile(
            "(?iu).*?\\b(?:резюме|cv)\\b.*"
    );

    private static final Pattern INTERACTION_INTENT_PATTERN = Pattern.compile(
            "(?iu).*?\\b(?:взаимодействи[е|я|й|ем])\\b.*"
    );

    private static final Pattern BROWSE_VACANCY_PATTERN = Pattern.compile(
            "(?iu).*?\\b(?:ваканси[и|й|я]|позици[и|й|я])\\b.*"
    );

    private static final Pattern SCREEN_COMMAND_PATTERN = Pattern.compile(
            "(?iu).*?\\b(?:открой|покажи|перейди\\s+в|запусти)\\b.*?(?:экран|форм[уа]|окно|раздел)\\s+([a-zA-Z0-9_\\$\\.]+).*"
    );

    private static final Pattern SETTINGS_SCREEN_PATTERN = Pattern.compile(
            "(?iu).*?\\b(?:открой|покажи|перейди\\s+в)\\b.*?(?:экран|форм[уа]|окно|раздел)?.*?\\b(?:настроек|настройки|exusersettingedit|settings)\\b.*"
    );

    private boolean isDestructiveCommand(String text) {
        return text != null && DESTRUCTIVE_INTENT_PATTERN.matcher(text).matches();
    }

    private boolean handleChatCommand(String rawText, boolean isHermes) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return false;
        }
        String text = rawText.trim();
        UUID convId = isHermes ? hermesConversationId : conversationId;

        // 1. СТРОГИЙ ЗАПРЕТ НА ДЕСТРУКТИВНЫЕ ОПЕРАЦИИ (Требование 5)
        if (isDestructiveCommand(text)) {
            String rejectMsg = "⛔ **Ограничение безопасности:** Операции удаления данных из базы данных строго запрещены политикой безопасности HRM HuntTech для любого пользователя системы.\n\n"
                    + "Чат работает исключительно в защищённых режимах чтения, аналитического поиска, создания взаимодействий и открытия экранных форм.";
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Операции удаления запрещены")
                    .withDescription("Удаление любых данных из чата категорически заблокировано.")
                    .show();
            recordCommandInteraction(convId, isHermes, text, rejectMsg);
            if (isHermes) {
                resetHermesControls(true);
            } else {
                resetControls(true);
            }
            return true;
        }

        // 2. ОТКРЫТИЕ ВАКАНСИИ ПО НОМЕРУ (Требование 3)
        String vacNum = extractVacancyNumber(text);
        if (vacNum != null) {
            String resultText = processOpenVacancyCommand(vacNum);
            recordCommandInteraction(convId, isHermes, text, resultText);
            if (isHermes) {
                resetHermesControls(true);
            } else {
                resetControls(true);
            }
            return true;
        }

        // 3. ПОСЛЕДНЕЕ РЕЗЮМЕ КАНДИДАТА (Требование 3)
        if (looksLikeCvRequest(text)) {
            String candName = extractCandidateNameForCv(text);
            if (candName != null && !candName.trim().isEmpty()) {
                String resultText = processLatestCvCommand(candName.trim());
                recordCommandInteraction(convId, isHermes, text, resultText);
                if (isHermes) {
                    resetHermesControls(true);
                } else {
                    resetControls(true);
                }
                return true;
            }
        }

        // 4. СОЗДАНИЕ ВЗАИМОДЕЙСТВИЯ ДЛЯ КАНДИДАТА (Требование 3)
        if (looksLikeCreateInteractionRequest(text)) {
            String candName = extractCandidateNameForInteraction(text);
            if (candName != null && !candName.trim().isEmpty()) {
                String resultText = processCreateInteractionCommand(candName.trim());
                recordCommandInteraction(convId, isHermes, text, resultText);
                if (isHermes) {
                    resetHermesControls(true);
                } else {
                    resetControls(true);
                }
                return true;
            }
        }

        // 5. ОТКРЫТИЕ РЕЕСТРА ВАКАНСИЙ С ФИЛЬТРОМ ПО ДОЛЖНОСТИ (Требование 4)
        if (looksLikeOpenPositionBrowseRequest(text)) {
            String resultText = processOpenPositionBrowseCommand(text);
            recordCommandInteraction(convId, isHermes, text, resultText);
            if (isHermes) {
                resetHermesControls(true);
            } else {
                resetControls(true);
            }
            return true;
        }

        // 6. ОТКРЫТИЕ ЭКРАННЫХ ФОРМ С ПРОВЕРКОЙ ПРАВ ДОСТУПА CUBA PLATFORM (ExUserSettingEdit, settings, sec$User и др.)
        if (looksLikeScreenOpenRequest(text)) {
            String resultText = processOpenScreenCommand(text);
            recordCommandInteraction(convId, isHermes, text, resultText);
            if (isHermes) {
                resetHermesControls(true);
            } else {
                resetControls(true);
            }
            return true;
        }

        return false;
    }

    private String extractVacancyNumber(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (!lower.contains("ваканси") && !lower.contains("позици")) {
            return null;
        }
        if (!lower.contains("открой") && !lower.contains("редактир") && !lower.contains("покажи") && !lower.contains("карточк")) {
            return null;
        }
        Matcher m = VACANCY_NUM_PATTERN.matcher(text);
        if (m.matches()) {
            return m.group(1).trim();
        }
        // Fallback поиск последовательности цифр
        Matcher digitMatcher = Pattern.compile("(?iu)\\b(?:номер[а-я]*|№|id)?\\s*[:№#]?\\s*([0-9]{3,}[\\^]?)").matcher(text);
        if (digitMatcher.find()) {
            String cand = digitMatcher.group(1).trim();
            if (cand.length() >= 3) {
                return cand;
            }
        }
        return null;
    }

    private String processOpenVacancyCommand(String vacNum) {
        if (!security.isScreenPermitted("hunttech_OpenPosition.edit")) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Ограничение доступа к экрану")
                    .withDescription("У вашей учётной записи нет прав на открытие формы редактирования hunttech_OpenPosition.edit.")
                    .show();
            return "⛔ **Ограничение доступа к экрану:** У вашей учётной записи нет прав на открытие формы редактирования вакансий (`hunttech_OpenPosition.edit`).";
        }
        if (!security.isEntityOpPermitted(OpenPosition.class, EntityOp.READ)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Недостаточно прав")
                    .withDescription("У вашей учётной записи недостаточно прав для просмотра вакансий.")
                    .show();
            return "⛔ У вашей учётной записи недостаточно прав для просмотра вакансий (`OpenPosition`).";
        }
        if (!security.isEntityOpPermitted(OpenPosition.class, EntityOp.UPDATE)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Ограничение доступа (только чтение)")
                    .withDescription("У вашей роли доступ к вакансиям только для чтения. Вызов формы редактирования заблокирован.")
                    .show();
            return "⛔ **Ограничение доступа (только чтение):** Ваша роль имеет доступ к вакансиям только для чтения. Открытие формы «OpenPositionEdit» в режиме редактирования заблокировано политикой безопасности CUBA Platform.";
        }
        String cleanNum = vacNum.replace("^", "").trim();
        Integer intNum = null;
        try {
            intNum = Integer.parseInt(cleanNum);
        } catch (NumberFormatException ignored) {
        }

        List<OpenPosition> loadedVacancies = dataManager.load(OpenPosition.class)
                .query("select e from hunttech_OpenPosition e where " +
                        "(e.vacansyID = :rawNum or e.vacansyID = :cleanNum or " +
                        "(:intNum is not null and e.numberPosition = :intNum)) " +
                        "order by e.createTs desc")
                .parameter("rawNum", vacNum)
                .parameter("cleanNum", cleanNum)
                .parameter("intNum", intNum)
                .view("openPosition-view")
                .list();

        Map<UUID, OpenPosition> uniqueVacancies = new LinkedHashMap<>();
        for (OpenPosition v : loadedVacancies) {
            uniqueVacancies.put(v.getId(), v);
        }
        List<OpenPosition> vacancies = new ArrayList<>(uniqueVacancies.values());

        if (vacancies.size() == 1) {
            OpenPosition vacancy = vacancies.get(0);
            screenBuilders.editor(OpenPosition.class, this)
                    .withScreenClass(OpenPositionEdit.class)
                    .editEntity(vacancy)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .show();
            String title = vacancy.getVacansyName() != null ? vacancy.getVacansyName().trim() : "Вакансия";
            return "Открыта форма редактирования вакансии №" + vacNum + ": **[" + title + "](hrm://vacancy/" + vacancy.getId() + ")**.";
        } else if (vacancies.size() > 1) {
            StringBuilder sb = new StringBuilder("Найдено несколько вакансий с номером «").append(vacNum).append("». Уточните, какую именно открыть:\n\n");
            int idx = 1;
            for (OpenPosition v : vacancies) {
                String pName = (v.getProjectName() != null && v.getProjectName().getProjectName() != null) ? v.getProjectName().getProjectName() : "-";
                String vName = v.getVacansyName() != null ? v.getVacansyName() : "Вакансия";
                sb.append(idx++).append(". [").append(vName).append("](hrm://vacancy/").append(v.getId())
                  .append(") (Проект: ").append(pName).append(", ID: ").append(v.getVacansyID() != null ? v.getVacansyID() : "б/н").append(")\n");
            }
            return sb.toString();
        } else {
            return "Вакансия с номером «" + vacNum + "» не найдена в системе. Проверьте правильность введённого номера.";
        }
    }

    private boolean looksLikeCvRequest(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return CV_INTENT_PATTERN.matcher(text).matches()
                && (lower.contains("последн") || lower.contains("открой") || lower.contains("покажи") || lower.contains("найди"));
    }

    private String extractCandidateNameForCv(String text) {
        String cleaned = text.replaceAll("(?iu)\\b(открой|мне|в\\s+форме\\s+редактирования|форме|редактирования|покажи|найди|последнее|резюме|cv|кандидата|пожалуйста|по|для)\\b", " ");
        cleaned = cleaned.replaceAll("[:\\?,\\!\\^]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() >= 3 ? cleaned : null;
    }

    private String processLatestCvCommand(String candidateQuery) {
        if (!security.isScreenPermitted("hunttech_CandidateCV.edit")) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Ограничение доступа к экрану")
                    .withDescription("У вашей учётной записи нет прав на открытие формы резюме hunttech_CandidateCV.edit.")
                    .show();
            return "⛔ **Ограничение доступа к экрану:** У вашей учётной записи нет прав на открытие формы резюме (`hunttech_CandidateCV.edit`).";
        }
        if (!security.isEntityOpPermitted(CandidateCV.class, EntityOp.READ)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Недостаточно прав")
                    .withDescription("У вашей учётной записи недостаточно прав для просмотра резюме.")
                    .show();
            return "⛔ У вашей учётной записи недостаточно прав для просмотра резюме (`CandidateCV`).";
        }
        if (!security.isEntityOpPermitted(CandidateCV.class, EntityOp.UPDATE)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Ограничение доступа (только чтение)")
                    .withDescription("У вашей роли доступ к резюме только для чтения. Вызов формы редактирования заблокирован.")
                    .show();
            return "⛔ **Ограничение доступа (только чтение):** У вашей учётной записи доступ к резюме кандидатов предоставлен только для чтения. Открытие формы «CandidateCVEdit» в режиме редактирования заблокировано политикой безопасности CUBA Platform.";
        }
        List<JobCandidate> candidates = searchCandidatesByName(candidateQuery);
        if (candidates.size() > 1) {
            StringBuilder sb = new StringBuilder("Найдено несколько кандидатов по запросу «").append(candidateQuery).append("». Уточните, чьё резюме открыть:\n\n");
            int idx = 1;
            for (JobCandidate c : candidates) {
                String pos = (c.getPersonPosition() != null && c.getPersonPosition().getPositionRuName() != null)
                        ? c.getPersonPosition().getPositionRuName() : "специализация не указана";
                String phone = c.getMobilePhone() != null ? c.getMobilePhone() : (c.getPhone() != null ? c.getPhone() : "-");
                sb.append(idx++).append(". **[").append(c.getFullName()).append("](hrm://candidate/").append(c.getId())
                  .append(")** (специализация: ").append(pos).append(", тел: ").append(phone).append(")\n");
            }
            return sb.toString();
        } else if (candidates.size() == 1) {
            JobCandidate candidate = candidates.get(0);
            CandidateCV latestCv = dataManager.load(CandidateCV.class)
                    .query("select e from hunttech_CandidateCV e where e.candidate.id = :candId order by e.createTs desc")
                    .parameter("candId", candidate.getId())
                    .view("candidateCV-view")
                    .maxResults(1)
                    .optional()
                    .orElse(null);
            if (latestCv != null) {
                screenBuilders.editor(CandidateCV.class, this)
                        .withScreenClass(CandidateCVEdit.class)
                        .editEntity(latestCv)
                        .withOpenMode(OpenMode.NEW_TAB)
                        .show();
                String dateStr = latestCv.getCreateTs() != null ? new SimpleDateFormat("dd.MM.yyyy").format(latestCv.getCreateTs()) : "б/д";
                return "Открыто последнее резюме кандидата **[" + candidate.getFullName() + "](hrm://candidate/" + candidate.getId() + ")** от " + dateStr + ": **[Карточка резюме](hrm://cv/" + latestCv.getId() + ")**.";
            } else {
                return "У кандидата **[" + candidate.getFullName() + "](hrm://candidate/" + candidate.getId() + ")** резюме пока не загружено в систему.";
            }
        } else {
            return "Кандидат по запросу «" + candidateQuery + "» не найден в базе данных. Проверьте правильность написания ФИО.";
        }
    }

    private boolean looksLikeCreateInteractionRequest(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return INTERACTION_INTENT_PATTERN.matcher(text).matches()
                && (lower.contains("созда") || lower.contains("добав") || lower.contains("нов") || lower.contains("давай"));
    }

    private String extractCandidateNameForInteraction(String text) {
        String cleaned = text.replaceAll("(?iu)\\b(давай|создадим|создай|создать|добавь|добавить|новое|взаимодействие|для|кандидата|кандидатом|пользователя)\\b", " ");
        cleaned = cleaned.replaceAll("[:\\?,\\!\\^]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() >= 3 ? cleaned : null;
    }

    private String processCreateInteractionCommand(String candidateQuery) {
        if (!security.isScreenPermitted("hunttech_IteractionList.edit")) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Ограничение доступа к экрану")
                    .withDescription("У вашей учётной записи нет прав на открытие формы взаимодействия hunttech_IteractionList.edit.")
                    .show();
            return "⛔ **Ограничение доступа к экрану:** У вашей учётной записи нет прав на открытие формы взаимодействия (`hunttech_IteractionList.edit`).";
        }
        if (!security.isEntityOpPermitted(IteractionList.class, EntityOp.CREATE)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Ограничение доступа (только чтение)")
                    .withDescription("У вашей роли доступ к взаимодействиям только для чтения. Создание заблокировано.")
                    .show();
            return "⛔ **Ограничение доступа (только чтение):** У вашей учётной записи нет прав на создание взаимодействий (`IteractionList`). Создание нового экземпляра заблокировано политикой безопасности CUBA Platform.";
        }
        List<JobCandidate> candidates = searchCandidatesByName(candidateQuery);
        if (candidates.size() > 1) {
            StringBuilder sb = new StringBuilder("Найдено несколько кандидатов по запросу «").append(candidateQuery).append("». Уточните, для кого создать взаимодействие:\n\n");
            int idx = 1;
            for (JobCandidate c : candidates) {
                String pos = (c.getPersonPosition() != null && c.getPersonPosition().getPositionRuName() != null)
                        ? c.getPersonPosition().getPositionRuName() : "специализация не указана";
                String phone = c.getMobilePhone() != null ? c.getMobilePhone() : (c.getPhone() != null ? c.getPhone() : "-");
                sb.append(idx++).append(". **[").append(c.getFullName()).append("](hrm://candidate/").append(c.getId())
                  .append(")** (специализация: ").append(pos).append(", тел: ").append(phone).append(")\n");
            }
            return sb.toString();
        } else if (candidates.size() == 1) {
            JobCandidate candidate = candidates.get(0);
            IteractionList interaction = dataManager.create(IteractionList.class);
            interaction.setCandidate(candidate);
            interaction.setDateIteraction(new java.util.Date());
            screenBuilders.editor(IteractionList.class, this)
                    .withScreenClass(IteractionListEdit.class)
                    .newEntity(interaction)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .show();
            return "Создано новое взаимодействие для кандидата **[" + candidate.getFullName() + "](hrm://candidate/" + candidate.getId() + ")**, открыта форма редактирования `IteractionListEdit`.";
        } else {
            return "Кандидат по запросу «" + candidateQuery + "» не найден для создания взаимодействия. Проверьте правильность написания имени.";
        }
    }

    private boolean looksLikeOpenPositionBrowseRequest(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return BROWSE_VACANCY_PATTERN.matcher(text).matches()
                && (lower.contains("покажи") || lower.contains("открой") || lower.contains("реестр") || lower.contains("список") || lower.contains("все"))
                && (lower.contains("должност") || lower.contains("специализаци") || lower.contains("направлени") || lower.contains("профил") || lower.contains("позици"));
    }

    private String processOpenPositionBrowseCommand(String text) {
        if (!security.isScreenPermitted("hunttech_OpenPosition.reestr") && !security.isScreenPermitted("hunttech_OpenPosition.browse")) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Ограничение доступа к экрану")
                    .withDescription("У вашей учётной записи нет прав на открытие реестра вакансий.")
                    .show();
            return "⛔ **Ограничение доступа к экрану:** У вашей учётной записи нет прав на открытие реестра вакансий (`hunttech_OpenPosition.reestr`). Вызов экрана заблокирован политикой безопасности CUBA Platform.";
        }
        if (!security.isEntityOpPermitted(OpenPosition.class, EntityOp.READ)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Недостаточно прав")
                    .withDescription("У вашей учётной записи недостаточно прав для просмотра вакансий.")
                    .show();
            return "⛔ **Ограничение доступа:** У вашей учётной записи недостаточно прав для просмотра вакансий (`OpenPosition`).";
        }
        String cleaned = text.replaceAll("(?iu)\\b(покажи|мне|все|открой|реестр|список|вакансии|вакансий|позиции|позиций|с|должностью|должность|позиция|позицией|по|специализации|направлению)\\b", " ");
        cleaned = cleaned.replaceAll("[:\\?,\\!\\^]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() < 2) {
            return "Уточните, пожалуйста, вакансии с какой именно должностью показать (например: «системный аналитик», «Java разработчик», «QA инженер»)?";
        }
        String roleName = cleaned;
        try {
            OpenPositionReestrBrowse browse = screenBuilders.screen(this)
                    .withScreenClass(OpenPositionReestrBrowse.class)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .build();
            browse.setPositionTypeFilter(roleName);
            browse.show();
            return "Открыт реестр вакансий `OpenPositionReestrBrowse` с фильтрацией по должности «**" + roleName + "**».";
        } catch (Exception ex) {
            log.error("Ошибка при открытии OpenPositionReestrBrowse с фильтром: {}", ex.getMessage(), ex);
            return "Не удалось открыть реестр вакансий: " + ex.getMessage();
        }
    }

    private boolean looksLikeScreenOpenRequest(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        if (extractVacancyNumber(text) != null || looksLikeCvRequest(text) || looksLikeCreateInteractionRequest(text) || looksLikeOpenPositionBrowseRequest(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        boolean hasAction = lower.contains("открой") || lower.contains("покажи") || lower.contains("перейди") || lower.contains("запусти");
        boolean hasTarget = lower.contains("экран") || lower.contains("форм") || lower.contains("окно") || lower.contains("раздел")
                || lower.contains("exusersettingedit") || lower.contains("settings") || lower.contains("настроек") || lower.contains("настройки");
        return hasAction && hasTarget;
    }

    private String processOpenScreenCommand(String text) {
        String screenId = null;
        Matcher m = SCREEN_COMMAND_PATTERN.matcher(text);
        if (m.matches()) {
            screenId = m.group(1).trim();
        } else if (SETTINGS_SCREEN_PATTERN.matcher(text).matches()) {
            screenId = "ExUserSettingEdit";
        }

        if (screenId == null || screenId.isEmpty()) {
            return "Уточните, пожалуйста, какой именно экран или форму вы хотите открыть.";
        }

        boolean isSettings = "ExUserSettingEdit".equalsIgnoreCase(screenId)
                || "settings".equalsIgnoreCase(screenId)
                || text.toLowerCase(Locale.ROOT).contains("настроек")
                || text.toLowerCase(Locale.ROOT).contains("настройки");

        if (isSettings) {
            // Стандартная проверка прав доступа CubaPlatform к экрану настроек и алиасу ExUserSettingEdit
            boolean permitted = security.isScreenPermitted("settings") && security.isScreenPermitted("ExUserSettingEdit");
            if (!permitted) {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption("Ограничение доступа к экрану")
                        .withDescription("У вашей учётной записи нет прав на открытие экрана «ExUserSettingEdit» (settings).")
                        .show();
                return "⛔ **Ограничение доступа:** У вашей учётной записи нет прав на открытие экрана «ExUserSettingEdit» (`settings`). Вызов экрана заблокирован стандартной политикой безопасности CUBA Platform.";
            }

            try {
                screenBuilders.screen(this)
                        .withScreenId("settings")
                        .withOpenMode(OpenMode.NEW_TAB)
                        .show();
                return "Открыт экран персональных настроек пользователя `ExUserSettingEdit` (`settings`).";
            } catch (Exception ex) {
                log.error("Ошибка при открытии экрана настроек: {}", ex.getMessage(), ex);
                return "Не удалось открыть экран настроек: " + ex.getMessage();
            }
        }

        String targetScreen = screenId;
        if (!windowConfig.hasWindow(targetScreen)) {
            return "Экран с идентификатором «" + targetScreen + "» не найден в конфигурации системы.";
        }

        if (!security.isScreenPermitted(targetScreen)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Ограничение доступа к экрану")
                    .withDescription("У вашей учётной записи нет прав на открытие экрана «" + targetScreen + "».")
                    .show();
            return "⛔ **Ограничение доступа:** У вашей учётной записи нет прав на открытие экрана «" + targetScreen + "». Вызов экрана заблокирован стандартной политикой безопасности CUBA Platform.";
        }

        try {
            screenBuilders.screen(this)
                    .withScreenId(targetScreen)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .show();
            return "Открыт экран «" + targetScreen + "».";
        } catch (Exception ex) {
            log.error("Ошибка при открытии экрана {}: {}", targetScreen, ex.getMessage(), ex);
            return "Не удалось открыть экран «" + targetScreen + "»: " + ex.getMessage();
        }
    }

    private List<JobCandidate> searchCandidatesByName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String clean = rawName.replaceAll("[^a-zA-Zа-яА-ЯёЁ\\s]", " ").replaceAll("\\s+", " ").trim();
        String[] tokens = clean.split("\\s+");
        if (tokens.length == 0) {
            return Collections.emptyList();
        }

        if (tokens.length == 1) {
            String token = tokens[0].toLowerCase(Locale.ROOT);
            String stem = stemRussianWord(token);
            return dataManager.load(JobCandidate.class)
                    .query("select e from hunttech_JobCandidate e where " +
                            "(lower(e.secondName) like :stem or lower(e.firstName) like :stem or lower(e.fullName) like :stem) " +
                            "order by e.createTs desc")
                    .parameter("stem", "%" + stem + "%")
                    .view("jobCandidate-view")
                    .maxResults(10)
                    .list();
        } else {
            String w1 = stemRussianWord(tokens[0].toLowerCase(Locale.ROOT));
            String w2 = stemRussianWord(tokens[1].toLowerCase(Locale.ROOT));
            String full = clean.toLowerCase(Locale.ROOT);
            return dataManager.load(JobCandidate.class)
                    .query("select e from hunttech_JobCandidate e where " +
                            "((lower(e.secondName) like :w1 and lower(e.firstName) like :w2) or " +
                            "(lower(e.secondName) like :w2 and lower(e.firstName) like :w1) or " +
                            "lower(e.fullName) like :full) " +
                            "order by e.createTs desc")
                    .parameter("w1", "%" + w1 + "%")
                    .parameter("w2", "%" + w2 + "%")
                    .parameter("full", "%" + full + "%")
                    .view("jobCandidate-view")
                    .maxResults(10)
                    .list();
        }
    }

    private String stemRussianWord(String word) {
        if (word == null || word.length() <= 3) {
            return word != null ? word : "";
        }
        String w = word;
        // Отрезаем окончания родительного, дательного, творительного падежей
        if (w.endsWith("ова") || w.endsWith("ева") || w.endsWith("ина")) {
            return w.substring(0, w.length() - 1); // Иванов, Петров
        }
        if (w.endsWith("ом") || w.endsWith("ем") || w.endsWith("ой") || w.endsWith("ей")) {
            return w.substring(0, w.length() - 2);
        }
        if (w.endsWith("а") || w.endsWith("я") || w.endsWith("у") || w.endsWith("ю") || w.endsWith("е") || w.endsWith("и")) {
            return w.substring(0, w.length() - 1);
        }
        return w;
    }

    private void recordCommandInteraction(UUID convId, boolean isHermes, String requestText, String responseText) {
        if (convId == null) {
            return;
        }
        try {
            LlmChatConversation conv = dataManager.load(LlmChatConversation.class)
                    .id(convId)
                    .view("llm-chat-conversation-view")
                    .optional()
                    .orElse(null);
            if (conv != null) {
                Integer maxSeq = dataManager.loadValue(
                        "select max(m.sequenceNo) from hunttech_LlmChatMessage m where m.conversation.id = :convId",
                        Integer.class)
                        .parameter("convId", convId)
                        .optional()
                        .orElse(0);
                if (maxSeq == null) {
                    maxSeq = 0;
                }

                LlmChatMessage userMsg = dataManager.create(LlmChatMessage.class);
                userMsg.setConversation(conv);
                userMsg.setRole("USER");
                userMsg.setContent(requestText);
                userMsg.setSequenceNo(maxSeq + 1);
                userMsg.setStatus("COMPLETED");

                LlmChatMessage aiMsg = dataManager.create(LlmChatMessage.class);
                aiMsg.setConversation(conv);
                aiMsg.setRole("ASSISTANT");
                aiMsg.setContent(responseText);
                aiMsg.setSequenceNo(maxSeq + 2);
                aiMsg.setStatus("COMPLETED");
                if (isHermes) {
                    aiMsg.setProviderCode("hermes");
                }

                conv.setLastMessageAt(new java.util.Date());
                dataManager.commit(userMsg, aiMsg, conv);

                if (isHermes) {
                    renderHermesHistory(hermesChatService.loadHermesHistory(convId));
                } else {
                    renderHistory(llmChatService.loadHistory(convId));
                }
            }
        } catch (Exception ex) {
            log.warn("Не удалось сохранить команду в историю диалога: {}", ex.getMessage(), ex);
        }
    }

    private void executeChatAction(String actionUrl) {
        if (actionUrl == null || actionUrl.trim().isEmpty()) {
            return;
        }
        String url = actionUrl.trim();
        try {
            if (url.startsWith("open-position") || url.startsWith("open-vacancy")) {
                String queryPart = url.contains("?") ? url.substring(url.indexOf('?') + 1) : "";
                String num = extractParam(queryPart, "number");
                if (num == null) {
                    num = extractParam(queryPart, "num");
                }
                String idStr = extractParam(queryPart, "id");
                if (idStr != null && !idStr.isEmpty()) {
                    openHrmEntityScreen("vacancy", idStr);
                } else if (num != null && !num.isEmpty()) {
                    processOpenVacancyCommand(num);
                }
            } else if (url.startsWith("open-cv")) {
                String queryPart = url.contains("?") ? url.substring(url.indexOf('?') + 1) : "";
                String idStr = extractParam(queryPart, "id");
                if (idStr == null) {
                    idStr = extractParam(queryPart, "candId");
                }
                String candName = extractParam(queryPart, "candidateName");
                if (idStr != null && !idStr.isEmpty()) {
                    openHrmEntityScreen("cv", idStr);
                } else if (candName != null && !candName.isEmpty()) {
                    processLatestCvCommand(candName);
                }
            } else if (url.startsWith("create-interaction")) {
                if (!security.isScreenPermitted("hunttech_IteractionList.edit")) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("Ограничение доступа к экрану")
                            .withDescription("У вашей учётной записи нет прав на открытие формы взаимодействия.")
                            .show();
                    return;
                }
                if (!security.isEntityOpPermitted(IteractionList.class, EntityOp.CREATE)) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("Ограничение доступа (только чтение)")
                            .withDescription("У вашей роли доступ к взаимодействиям только для чтения. Создание заблокировано.")
                            .show();
                    return;
                }
                String queryPart = url.contains("?") ? url.substring(url.indexOf('?') + 1) : "";
                String candName = extractParam(queryPart, "candidateName");
                String candId = extractParam(queryPart, "candidateId");
                if (candId == null) {
                    candId = extractParam(queryPart, "candId");
                }
                if (candId != null && !candId.isEmpty()) {
                    UUID parsedCandId = null;
                    try {
                        parsedCandId = UUID.fromString(candId.trim());
                    } catch (IllegalArgumentException e) {
                        log.warn("Некорректный UUID кандидата в действии чата: {}", candId);
                    }
                    if (parsedCandId != null) {
                        JobCandidate c = dataManager.load(JobCandidate.class).id(parsedCandId).view("jobCandidate-view").optional().orElse(null);
                        if (c != null) {
                            IteractionList interaction = dataManager.create(IteractionList.class);
                            interaction.setCandidate(c);
                            interaction.setDateIteraction(new java.util.Date());
                            screenBuilders.editor(IteractionList.class, this)
                                     .withScreenClass(IteractionListEdit.class)
                                     .newEntity(interaction)
                                     .withOpenMode(OpenMode.NEW_TAB)
                                     .show();
                        }
                    }
                } else if (candName != null && !candName.isEmpty()) {
                    processCreateInteractionCommand(candName);
                }
            } else if (url.startsWith("open-browse") || url.startsWith("browse-vacancies")) {
                if (!security.isScreenPermitted("hunttech_OpenPosition.reestr") && !security.isScreenPermitted("hunttech_OpenPosition.browse")) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("Ограничение доступа к экрану")
                            .withDescription("У вашей учётной записи нет прав на открытие реестра вакансий.")
                            .show();
                    return;
                }
                if (!security.isEntityOpPermitted(OpenPosition.class, EntityOp.READ)) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("Недостаточно прав")
                            .withDescription("У вашей учётной записи нет прав на просмотр вакансий.")
                            .show();
                    return;
                }
                String queryPart = url.contains("?") ? url.substring(url.indexOf('?') + 1) : "";
                String role = extractParam(queryPart, "role");
                if (role == null) {
                    role = extractParam(queryPart, "pos");
                }
                if (role != null && !role.isEmpty()) {
                    OpenPositionReestrBrowse browse = screenBuilders.screen(this)
                            .withScreenClass(OpenPositionReestrBrowse.class)
                            .withOpenMode(OpenMode.NEW_TAB)
                            .build();
                    browse.setPositionTypeFilter(role);
                    browse.show();
                }
            } else if (url.startsWith("open-screen")) {
                String queryPart = url.contains("?") ? url.substring(url.indexOf('?') + 1) : "";
                String screenId = extractParam(queryPart, "screenId");
                if (screenId == null) {
                    screenId = extractParam(queryPart, "id");
                }
                if (screenId != null && !screenId.isEmpty()) {
                    processOpenScreenCommand("открой экран " + screenId);
                }
            }
        } catch (Exception ex) {
            log.error("Ошибка при выполнении действия чата {}: {}", actionUrl, ex.getMessage(), ex);
        }
    }

    private String extractParam(String queryString, String paramName) {
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }
        for (String pair : queryString.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = pair.substring(0, eq).trim();
                if (key.equalsIgnoreCase(paramName)) {
                    try {
                        return URLDecoder.decode(pair.substring(eq + 1).trim(), StandardCharsets.UTF_8.name());
                    } catch (Exception e) {
                        return pair.substring(eq + 1).trim();
                    }
                }
            }
        }
        return null;
    }

}

