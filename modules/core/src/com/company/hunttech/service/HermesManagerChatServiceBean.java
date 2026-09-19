package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesManagerConfig;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Project;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.service.dto.HermesChatMessage;
import com.company.hunttech.service.dto.HermesChatResponse;
import com.company.hunttech.service.dto.HermesConnectionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.company.hunttech.core.ai.AiCostCalculator;
import com.company.hunttech.entity.ai.AiCallLog;
import com.company.hunttech.service.AiSecuritySanitizer;
import com.company.hunttech.service.UserAiQuotaService;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Серверная реализация сервиса взаимодействия со вторым Hermes Agent для менеджеров и директоров
 * (профиль hrm-operator в Docker-контейнере hermes-hrm-operator).
 *
 * Ключевые гарантии безопасности:
 * 1. Проверка специфического права hunttech.ai.useManagerHermesWrite на уровне бэкенда.
 * 2. Полный запрет raw SQL, DDL, TRUNCATE, DELETE FROM.
 * 3. Полный запрет операции DELETE (DENY BY DEFAULT).
 * 4. Минимальный allowlist сущностей: только OpenPosition.
 * 5. Минимальный allowlist полей OpenPosition.
 * 6. Делегирование прав и ограничений в CUBA Security (isEntityOpPermitted, isEntityAttrPermitted).
 * 7. Мутации выполняются только через DataManager в контексте текущего пользователя.
 */
@Service(HermesManagerChatService.NAME)
public class HermesManagerChatServiceBean implements HermesManagerChatService {
    private static final Logger log = LoggerFactory.getLogger(HermesManagerChatServiceBean.class);

    public static final String PERMISSION_MANAGER_HERMES_WRITE = "hunttech.ai.useManagerHermesWrite";
    private static final String CONVERSATION_TITLE_PREFIX = "Hermes-Manager: ";
    private static final String DEFAULT_PROD_HOST = "hr.hunttech.ru";
    private static final String DEFAULT_PROD_IP = "92.63.101.170";
    private static final String DOCKER_SOCKET_PATH = "/var/run/docker.sock";
    private static final String SSH_KNOWN_HOSTS_PATH = "/tmp/hermes_known_hosts";

    private static final Pattern RAW_SQL_PATTERN = Pattern.compile(
            "(?i)\\b(SELECT\\s+.*\\bFROM|DROP\\s+TABLE|DROP\\s+VIEW|DROP\\s+DATABASE|ALTER\\s+TABLE|TRUNCATE\\s+TABLE|TRUNCATE|DELETE\\s+FROM|INSERT\\s+INTO|UPDATE\\s+[a-z0-9_]+\\s+SET|GRANT\\s+|REVOKE\\s+|EXEC\\s+|EXECUTE\\s+|UNION\\s+ALL|UNION\\s+SELECT)\\b");

    private static final Pattern JSON_MUTATION_PATTERN = Pattern.compile(
            "```(?:json)?\\s*(\\{\\s*\"intent\"\\s*:\\s*\"MUTATION\".*?\\})\\s*```", Pattern.DOTALL);

    private static final Set<String> ALLOWED_ENTITIES = new HashSet<>(Arrays.asList(
            "openposition", "hunttech_openposition"
    ));

    private static final Set<String> ALLOWED_OPEN_POSITION_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "vacansyName", "shortDescription", "comment", "commentEn",
            "salaryMin", "salaryMax", "salaryComment", "salaryFixLimit", "salaryCandidateRequest", "salaryIE",
            "remoteWork", "remoteComment", "registrationForWork",
            "priority", "priorityComment", "commandCandidate", "numberPosition",
            "workExperience", "commandExperience", "openClose", "signDraft"
    )));

    @Inject
    private Configuration configuration;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private Security security;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private UserAiQuotaService userAiQuotaService;
    @Inject
    private AiSecretService aiSecretService;

    private static class HermesExecutionCandidate {
        private final String source; // "USER", "ADMIN"
        private final String providerCode;
        private final String modelName;
        private final String apiKey;
        private final String baseUrl;

        public HermesExecutionCandidate(String source, String providerCode, String modelName,
                                        String apiKey, String baseUrl) {
            this.source = source;
            this.providerCode = providerCode;
            this.modelName = modelName;
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
        }

        public String getSource() { return source; }
        public String getProviderCode() { return providerCode; }
        public String getModelName() { return modelName; }
        public String getApiKey() { return apiKey; }
        public String getBaseUrl() { return baseUrl; }
    }

    private List<HermesExecutionCandidate> resolveExecutionCandidates(ExtUser currentUser) {
        List<HermesExecutionCandidate> candidates = new ArrayList<>();
        if (currentUser != null) {
            try {
                List<UserAiConfiguration> userConfigs = dataManager.load(UserAiConfiguration.class)
                        .query("select c from hunttech_UserAiConfiguration c " +
                                "where c.user.id = :userId and (c.isActive is null or c.isActive = true) " +
                                "order by c.isPrimary desc, c.priority desc")
                        .parameter("userId", currentUser.getId())
                        .list();
                for (UserAiConfiguration uc : userConfigs) {
                    String apiKey = resolveUserApiKey(uc);
                    if (apiKey != null && !apiKey.trim().isEmpty() && uc.getProviderCode() != null) {
                        candidates.add(new HermesExecutionCandidate("USER", uc.getProviderCode().trim(),
                                uc.getDefaultModelName(), apiKey.trim(), null));
                    }
                }
            } catch (Exception e) {
                log.warn("Ошибка загрузки пользовательских AI-конфигураций для Hermes-operator: {}", e.getMessage());
            }
        }

        try {
            List<AdminAiConfiguration> adminConfigs = dataManager.load(AdminAiConfiguration.class)
                    .query("select c from hunttech_AdminAiConfiguration c " +
                            "where (c.active is null or c.active = true) " +
                            "order by c.priority desc")
                    .list();
            for (AdminAiConfiguration ac : adminConfigs) {
                String apiKey = null;
                if (ac.getApiKeyEncrypted() != null && !ac.getApiKeyEncrypted().trim().isEmpty() && aiSecretService != null) {
                    try {
                        apiKey = aiSecretService.decrypt(ac.getApiKeyEncrypted());
                    } catch (Exception e) {
                        log.warn("Не удалось расшифровать ключ AdminAiConfiguration: {}", e.getMessage());
                    }
                }
                if (apiKey != null && !apiKey.trim().isEmpty() && ac.getProviderCode() != null) {
                    candidates.add(new HermesExecutionCandidate("ADMIN", ac.getProviderCode().trim(),
                            ac.getDefaultModelName(), apiKey.trim(), ac.getBaseApiUrl()));
                }
            }
        } catch (Exception e) {
            log.warn("Ошибка загрузки административных AI-конфигураций для Hermes-operator: {}", e.getMessage());
        }

        candidates.add(new HermesExecutionCandidate("ADMIN", null, null, null, null));
        return candidates;
    }

    private String resolveUserApiKey(UserAiConfiguration configuration) {
        if (configuration.getApiKeyEncrypted() != null && !configuration.getApiKeyEncrypted().trim().isEmpty() && aiSecretService != null) {
            try {
                return aiSecretService.decrypt(configuration.getApiKeyEncrypted());
            } catch (Exception e) {
                log.warn("Не удалось расшифровать ключ UserAiConfiguration: {}", e.getMessage());
            }
        }
        return configuration.getApiKey();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HunttechHermesManagerConfig getManagerConfig() {
        return configuration.getConfig(HunttechHermesManagerConfig.class);
    }

    private ExtUser getCurrentUser() {
        if (userSessionSource == null || userSessionSource.getUserSession() == null) {
            return null;
        }
        return (ExtUser) userSessionSource.getUserSession().getUser();
    }

    private void checkManagerPermission() {
        if (security == null || !security.isSpecificPermitted(PERMISSION_MANAGER_HERMES_WRITE)) {
            log.warn("Попытка обращения к Manager Hermes без права {}", PERMISSION_MANAGER_HERMES_WRITE);
            throw new SecurityException("Недостаточно прав для выполнения операции");
        }
    }

    @Override
    public UUID startManagerHermesConversation() {
        checkManagerPermission();
        ExtUser currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        String title = CONVERSATION_TITLE_PREFIX + getManagerConfig().getProfile();
        LlmChatConversation conversation = dataManager.load(LlmChatConversation.class)
                .query("select e from hunttech_LlmChatConversation e " +
                        "where e.user.id = :userId and e.title = :title and e.status = 'ACTIVE' and e.deleteTs is null " +
                        "order by e.createTs desc")
                .parameter("userId", currentUser.getId())
                .parameter("title", title)
                .view("llm-chat-conversation-view")
                .optional()
                .orElse(null);

        if (conversation == null) {
            conversation = metadata.create(LlmChatConversation.class);
            conversation.setUser(currentUser);
            conversation.setTitle(title);
            conversation.setStatus("ACTIVE");
            conversation.setLastMessageAt(new Date());
            conversation = dataManager.commit(conversation);
        }

        return conversation.getId();
    }

    @Override
    public List<HermesChatMessage> loadManagerHermesHistory(UUID conversationId) {
        checkManagerPermission();
        if (conversationId == null) {
            return new ArrayList<>();
        }

        ExtUser currentUser = getCurrentUser();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;

        List<LlmChatMessage> messages = dataManager.load(LlmChatMessage.class)
                .query("select e from hunttech_LlmChatMessage e " +
                        "where e.conversation.id = :conversationId and e.conversation.user.id = :userId and e.deleteTs is null " +
                        "order by e.sequenceNo asc")
                .parameter("conversationId", conversationId)
                .parameter("userId", currentUserId)
                .view("llm-chat-message-view")
                .list();

        List<HermesChatMessage> result = new ArrayList<>();
        for (LlmChatMessage msg : messages) {
            HermesChatMessage dto = new HermesChatMessage();
            dto.setId(msg.getId());
            dto.setRole("USER".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant");
            dto.setContent(msg.getContent());
            dto.setCreateTs(msg.getCreateTs());
            dto.setSequenceNo(msg.getSequenceNo());
            dto.setHermesSessionId(msg.getProviderRequestId());
            result.add(dto);
        }
        return result;
    }

    @Override
    public void clearManagerHermesHistory(UUID conversationId) {
        checkManagerPermission();
        if (conversationId == null) {
            return;
        }

        ExtUser currentUser = getCurrentUser();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;

        List<LlmChatMessage> messages = dataManager.load(LlmChatMessage.class)
                .query("select e from hunttech_LlmChatMessage e " +
                        "where e.conversation.id = :conversationId and e.conversation.user.id = :userId and e.deleteTs is null")
                .parameter("conversationId", conversationId)
                .parameter("userId", currentUserId)
                .view("llm-chat-message-view")
                .list();

        for (LlmChatMessage msg : messages) {
            dataManager.remove(msg);
        }
    }

    @Override
    public HermesConnectionStatus checkManagerHermesConnection() {
        checkManagerPermission();
        long startTime = System.currentTimeMillis();
        HunttechHermesManagerConfig config = getManagerConfig();
        try {
            List<String> cmd = new ArrayList<>();
            if (shouldUseSsh(config)) {
                cmd.add("ssh");
                cmd.add("-o");
                cmd.add("BatchMode=yes");
                cmd.add("-o");
                cmd.add("ConnectTimeout=5");
                cmd.add("-o");
                cmd.add("StrictHostKeyChecking=accept-new");
                cmd.add("-o");
                cmd.add("UserKnownHostsFile=" + SSH_KNOWN_HOSTS_PATH);
                if (config.getSshPort() != 22) {
                    cmd.add("-p");
                    cmd.add(String.valueOf(config.getSshPort()));
                }
                cmd.add(config.getSshUser() + "@" + config.getSshHost());
                cmd.add("docker inspect -f '{{.State.Status}}' " + shellEscape(config.getContainerName()));
            } else {
                cmd.add("docker");
                cmd.add("inspect");
                cmd.add("-f");
                cmd.add("{{.State.Status}}");
                cmd.add(config.getContainerName());
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new HermesConnectionStatus(false, config.getSshHost(), config.getContainerName(),
                        config.getProfile(), "Таймаут подключения (10 с)", System.currentTimeMillis() - startTime);
            }

            int exitCode = process.exitValue();
            String stdout = readStream(process.getInputStream()).trim();
            long duration = System.currentTimeMillis() - startTime;

            if (exitCode == 0 && "running".equalsIgnoreCase(stdout)) {
                return new HermesConnectionStatus(true, config.getSshHost(), config.getContainerName(),
                        config.getProfile(), "Контейнер активен (статус: running)", duration);
            } else {
                String stderr = readStream(process.getErrorStream()).trim();
                return new HermesConnectionStatus(false, config.getSshHost(), config.getContainerName(),
                        config.getProfile(), "Контейнер недоступен (код " + exitCode + "): " + stderr + " " + stdout, duration);
            }
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            return new HermesConnectionStatus(false, config.getSshHost(), config.getContainerName(),
                    config.getProfile(), "Ошибка соединения: " + ex.getMessage(), duration);
        }
    }

    @Override
    public HermesChatResponse sendManagerHermesMessage(UUID conversationId, String messageText) {
        checkManagerPermission();
        if (messageText == null || messageText.trim().isEmpty()) {
            throw new IllegalArgumentException("Текст сообщения не может быть пустым");
        }

        // Проверка на попытки передачи raw SQL
        if (RAW_SQL_PATTERN.matcher(messageText).find()) {
            log.warn("[MANAGER_HERMES] Отклонена попытка отправки сырого SQL: '{}'", messageText);
            throw new SecurityException("Прямые SQL-запросы запрещены политикой безопасности. " +
                    "Управление данными осуществляется исключительно через безопасные функции HRM.");
        }

        ExtUser currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        long startTime = System.currentTimeMillis();
        String prompt = buildManagerPrompt(messageText.trim());
        int estimatedPromptTokens = estimateTokens(prompt);

        boolean hasPersonalModel = userAiQuotaService != null && userAiQuotaService.hasActivePersonalModel(currentUser.getId());
        if (!hasPersonalModel && userAiQuotaService != null) {
            try {
                userAiQuotaService.checkQuotaAvailable(currentUser.getId(), estimatedPromptTokens);
            } catch (DevelopmentException de) {
                long duration = System.currentTimeMillis() - startTime;
                String quotaErrorMsg = de.getMessage();
                log.warn("[MANAGER_HERMES] Пользователь {} исчерпал лимит токенов ИИ: {}",
                        currentUser.getLogin(), quotaErrorMsg);
                String model = getManagerConfig() != null ? getManagerConfig().getProfile() : "hrm-operator";
                logAiCall(currentUser, conversationId, duration, "hermes", model,
                        "ADMIN", estimatedPromptTokens, 0, estimatedPromptTokens, BigDecimal.ZERO, "USD",
                        "ERROR", quotaErrorMsg);
                return HermesChatResponse.error(conversationId, quotaErrorMsg, duration);
            }
        }

        // Загружаем или создаем беседу
        LlmChatConversation conversation = dataManager.load(LlmChatConversation.class)
                .id(conversationId)
                .view("llm-chat-conversation-view")
                .optional()
                .orElse(null);

        if (conversation == null) {
            conversationId = startManagerHermesConversation();
            conversation = dataManager.load(LlmChatConversation.class)
                    .id(conversationId)
                    .view("llm-chat-conversation-view")
                    .one();
        }

        // Вычисляем следующий порядковый номер
        Integer maxSeq = dataManager.loadValue(
                "select max(e.sequenceNo) from hunttech_LlmChatMessage e where e.conversation.id = :convId",
                Integer.class)
                .parameter("convId", conversationId)
                .optional()
                .orElse(0);
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;

        // Сохраняем сообщение пользователя
        LlmChatMessage userMsg = metadata.create(LlmChatMessage.class);
        userMsg.setConversation(conversation);
        userMsg.setRole("user");
        userMsg.setContent(messageText.trim());
        userMsg.setSequenceNo(nextSeq);
        userMsg.setCreateTs(new Date());
        dataManager.commit(userMsg);

        List<HermesExecutionCandidate> candidates = resolveExecutionCandidates(currentUser);
        String rawResponseText = null;
        HermesExecutionCandidate successfulCandidate = null;
        Exception lastException = null;

        for (int i = 0; i < candidates.size(); i++) {
            HermesExecutionCandidate candidate = candidates.get(i);
            try {
                if (userAiQuotaService != null && userAiQuotaService.isAdminModel(candidate.getSource())) {
                    userAiQuotaService.checkQuotaAvailable(currentUser.getId(), estimatedPromptTokens);
                }
                rawResponseText = executeHermesCli(prompt, candidate);
                successfulCandidate = candidate;
                break;
            } catch (DevelopmentException de) {
                lastException = de;
                log.warn("[MANAGER_HERMES] Лимит токенов исчерпан для кандидата [{}]: {}", candidate.getSource(), de.getMessage());
                break;
            } catch (Exception e) {
                lastException = e;
                log.warn("[MANAGER_HERMES] Сбой при вызове кандидата #{}/{} [{}]: {}", i + 1, candidates.size(), candidate.getSource(), e.getMessage());
            }
        }

        if (rawResponseText == null) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = lastException instanceof DevelopmentException
                    ? lastException.getMessage()
                    : "Ошибка связи с Hermes-контуром управления: " + (lastException != null ? lastException.getMessage() : "нет ответа");
            saveAssistantMessage(conversation, errorMsg, nextSeq + 1);

            int promptTokens = estimateTokens(prompt);
            String model = getManagerConfig() != null ? getManagerConfig().getProfile() : "hrm-operator";
            AiCostCalculator.CostResult costResult = AiCostCalculator.calculateCost(
                    "hermes", model, promptTokens, 0);
            String failOwner = (candidates != null && !candidates.isEmpty() && "USER".equalsIgnoreCase(candidates.get(candidates.size() - 1).getSource()))
                    ? "USER" : "ADMIN";
            logAiCall(currentUser, conversationId, duration, "hermes", model,
                    failOwner, promptTokens, 0, promptTokens, costResult.getCost(), costResult.getCurrency(),
                    "ERROR", errorMsg);

            return HermesChatResponse.error(conversationId, errorMsg, duration);
        }

        // Обработка возможного намерения на изменение данных
        String processedResponseText;
        try {
            processedResponseText = processHermesResponse(rawResponseText);
        } catch (SecurityException se) {
            log.warn("[MANAGER_HERMES] Отказ в доступе CUBA Security: {}", se.getMessage());
            processedResponseText = "⚠️ " + se.getMessage();
        } catch (Exception ex) {
            log.error("[MANAGER_HERMES] Ошибка обработки ответа: {}", ex.getMessage(), ex);
            processedResponseText = "⚠️ Не удалось выполнить операцию: " + ex.getMessage();
        }

        // Сохраняем ответ ассистента
        saveAssistantMessage(conversation, processedResponseText, nextSeq + 1);

        long duration = System.currentTimeMillis() - startTime;
        int promptTokens = estimateTokens(prompt);
        int completionTokens = estimateTokens(processedResponseText);
        int totalTokens = promptTokens + completionTokens;

        String providerCode = (successfulCandidate != null && successfulCandidate.getProviderCode() != null)
                ? successfulCandidate.getProviderCode() : "hermes";
        String modelName = (successfulCandidate != null && successfulCandidate.getModelName() != null)
                ? successfulCandidate.getModelName() : (getManagerConfig() != null ? getManagerConfig().getProfile() : "hrm-operator");
        String credentialOwner = (successfulCandidate != null && "USER".equalsIgnoreCase(successfulCandidate.getSource()))
                ? "USER" : "ADMIN";

        AiCostCalculator.CostResult costResult = AiCostCalculator.calculateCost(
                providerCode, modelName, promptTokens, completionTokens);

        // Списываем токены только если использовалась административная модель
        if (userAiQuotaService != null && totalTokens > 0) {
            userAiQuotaService.recordTokenConsumption(currentUser.getId(), totalTokens, credentialOwner);
        }

        // Логируем в AiCallLog
        logAiCall(currentUser, conversationId, duration, providerCode, modelName, credentialOwner,
                promptTokens, completionTokens, totalTokens, costResult.getCost(), costResult.getCurrency(),
                "SUCCESS", null);

        HermesChatResponse resp = new HermesChatResponse(conversationId, processedResponseText, null, duration);
        resp.setPromptTokens(promptTokens);
        resp.setCompletionTokens(completionTokens);
        resp.setTotalTokens(totalTokens);
        resp.setEstimatedCost(costResult.getCost());
        resp.setCurrency(costResult.getCurrency());
        resp.setModelName(modelName);
        resp.setProviderCode(providerCode);
        return resp;
    }

    private void saveAssistantMessage(LlmChatConversation conversation, String content, int seq) {
        LlmChatMessage assistantMsg = metadata.create(LlmChatMessage.class);
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(content);
        assistantMsg.setSequenceNo(seq);
        assistantMsg.setCreateTs(new Date());
        dataManager.commit(assistantMsg);

        conversation.setLastMessageAt(new Date());
        dataManager.commit(conversation);
    }

    private String buildManagerPrompt(String userMessage) {
        return "=== СИСТЕМНАЯ ИНСТРУКЦИЯ (УПРАВЛЕНИЕ HRM ДЛЯ МЕНЕДЖЕРОВ И ДИРЕКТОРОВ) ===\n" +
                "Ты — Hermes Agent (профиль hrm-operator) для руководителей в HRM HuntTech.\n" +
                "Твоя задача — консультировать пользователя и формировать структурированные намерения на создание или обновление вакансий.\n\n" +
                "ПРАВИЛА БЕЗОПАСНОСТИ:\n" +
                "1. Операция DELETE СТРОГО ЗАПРЕЩЕНА. Никогда не предлагай удаление данных.\n" +
                "2. Любые SQL команды, DDL, TRUNCATE запрещены.\n" +
                "3. Разрешенные сущности: ТОЛЬКО OpenPosition (вакансия).\n" +
                "4. Разрешенные поля OpenPosition: vacansyName, shortDescription, comment, commentEn, salaryMin, salaryMax, salaryComment, salaryFixLimit, salaryCandidateRequest, salaryIE, remoteWork, remoteComment, registrationForWork, priority, priorityComment, commandCandidate, numberPosition, workExperience, commandExperience, openClose, signDraft.\n\n" +
                "ФОРМАТ НАМЕРЕНИЯ:\n" +
                "Если пользователь просит создать или изменить вакансию, сформируй блок JSON строго в формате:\n" +
                "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"OpenPosition\",\n" +
                "  \"operation\": \"CREATE\", // или \"UPDATE\"\n" +
                "  \"entityId\": \"<UUID_если_UPDATE>\",\n" +
                "  \"summary\": \"Краткое понятное описание действия\",\n" +
                "  \"attributes\": {\n" +
                "    \"vacansyName\": \"...\",\n" +
                "    \"salaryMin\": 200000,\n" +
                "    \"salaryMax\": 350000\n" +
                "  }\n" +
                "}\n" +
                "```\n" +
                "Перед или после JSON-блока добавь пояснение для пользователя на русском языке.\n" +
                "Если данных недостаточно, просто задай уточняющий вопрос без генерации блока JSON.\n\n" +
                "ЗАПРОС ПОЛЬЗОВАТЕЛЯ:\n" + userMessage;
    }

    /**
     * Анализирует ответ Hermes и при наличии намерения MUTATION выполняет
     * безопасное изменение в CUBA под текущим пользователем.
     */
    String processHermesResponse(String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "Получен пустой ответ от Hermes.";
        }

        Matcher matcher = JSON_MUTATION_PATTERN.matcher(rawResponse);
        if (!matcher.find()) {
            // Обычный текстовый ответ без намерения на изменение данных
            return rawResponse.trim();
        }

        String jsonBlock = matcher.group(1);
        JsonNode root = objectMapper.readTree(jsonBlock);

        String entity = root.path("entity").asText("").trim();
        String operation = root.path("operation").asText("").trim().toUpperCase(Locale.ROOT);
        String entityIdStr = root.path("entityId").asText("").trim();
        String summary = root.path("summary").asText("").trim();
        JsonNode attributesNode = root.path("attributes");

        // 1. Проверка DELETE — абсолютный запрет
        if ("DELETE".equals(operation)) {
            log.error("[MANAGER_HERMES] Попытка операции DELETE заблокирована!");
            throw new SecurityException("Операция DELETE безусловно запрещена в Hermes. Удаление данных в HRM через AI недопустимо.");
        }

        // 2. Проверка операции — только CREATE или UPDATE
        if (!"CREATE".equals(operation) && !"UPDATE".equals(operation)) {
            throw new IllegalArgumentException("Операция '" + operation + "' не поддерживается. Разрешены только CREATE и UPDATE.");
        }

        // 3. Проверка сущности — только OpenPosition
        if (!ALLOWED_ENTITIES.contains(entity.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Сущность '" + entity + "' не поддерживается (DENY BY DEFAULT). Разрешено управление только вакансиями (OpenPosition).");
        }

        // 4. Проверка полей attributes по allowlist
        if (attributesNode.isObject()) {
            Iterator<String> fieldNames = attributesNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (!ALLOWED_OPEN_POSITION_FIELDS.contains(field)) {
                    throw new IllegalArgumentException("Поле '" + field + "' не входит в список разрешенных атрибутов (DENY BY DEFAULT).");
                }
            }
        }

        // 5. Проверка прав CUBA Security текущего пользователя
        MetaClass metaClass = metadata.getClassNN(OpenPosition.class);
        if ("CREATE".equals(operation)) {
            if (security != null && !security.isEntityOpPermitted(OpenPosition.class, EntityOp.CREATE)) {
                throw new SecurityException("Недостаточно прав для выполнения операции");
            }
        } else {
            if (security != null && !security.isEntityOpPermitted(OpenPosition.class, EntityOp.UPDATE)) {
                throw new SecurityException("Недостаточно прав для выполнения операции");
            }
        }

        if (attributesNode.isObject()) {
            Iterator<String> fieldNames = attributesNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (security != null && !security.isEntityAttrPermitted(metaClass, field, EntityAttrAccess.MODIFY)) {
                    throw new SecurityException("Недостаточно прав для выполнения операции");
                }
            }
        }

        // 6. Выполнение мутации в CUBA DataManager
        OpenPosition targetPosition;
        if ("CREATE".equals(operation)) {
            targetPosition = metadata.create(OpenPosition.class);
            // Заполнение обязательных системных полей дефолтами
            targetPosition.setCommandCandidate(1);
            targetPosition.setRemoteWork(1);
            targetPosition.setWorkExperience(1);
            targetPosition.setOpenClose(false);
            targetPosition.setSignDraft(true);
            targetPosition.setPriority(1);

            // Находим дефолтный активный проект для вакансии, если проект обязателен
            Project defaultProject = dataManager.load(Project.class)
                    .query("select p from hunttech_Project p where p.deleteTs is null order by p.createTs desc")
                    .maxResults(1)
                    .optional()
                    .orElse(null);
            if (defaultProject != null) {
                targetPosition.setProjectName(defaultProject);
            }
        } else {
            if (entityIdStr.isEmpty()) {
                throw new IllegalArgumentException("Для операции UPDATE необходимо указать идентификатор entityId.");
            }
            UUID posId = UUID.fromString(entityIdStr);
            targetPosition = dataManager.load(OpenPosition.class)
                    .id(posId)
                    .optional()
                    .orElseThrow(() -> new IllegalArgumentException("Вакансия с идентификатором " + entityIdStr + " не найдена."));
        }

        // Применяем атрибуты
        applyAttributes(targetPosition, attributesNode);

        // Коммитим изменения в контексте текущего пользователя
        OpenPosition committedPosition = dataManager.commit(targetPosition);

        String resultPrefix = "CREATE".equals(operation)
                ? "✅ **Вакансия успешно создана:**"
                : "✅ **Вакансия успешно обновлена:**";

        String link = "[" + committedPosition.getVacansyName() + "](hrm://openPosition/" + committedPosition.getId() + ")";
        String previewText = summary.isEmpty()
                ? resultPrefix + " " + link
                : resultPrefix + " " + link + "\n*" + summary + "*";

        // Заменяем JSON-блок в исходном ответе на красивую карточку результата
        return matcher.replaceFirst(Matcher.quoteReplacement(previewText));
    }

    private void applyAttributes(OpenPosition pos, JsonNode attributes) {
        if (attributes == null || !attributes.isObject()) {
            return;
        }

        if (attributes.hasNonNull("vacansyName")) {
            pos.setVacansyName(attributes.get("vacansyName").asText().trim());
        }
        if (attributes.hasNonNull("shortDescription")) {
            pos.setShortDescription(attributes.get("shortDescription").asText().trim());
        }
        if (attributes.hasNonNull("comment")) {
            pos.setComment(attributes.get("comment").asText().trim());
        }
        if (attributes.hasNonNull("commentEn")) {
            pos.setCommentEn(attributes.get("commentEn").asText().trim());
        }
        if (attributes.hasNonNull("salaryMin")) {
            pos.setSalaryMin(new BigDecimal(attributes.get("salaryMin").asText().trim()));
        }
        if (attributes.hasNonNull("salaryMax")) {
            pos.setSalaryMax(new BigDecimal(attributes.get("salaryMax").asText().trim()));
        }
        if (attributes.hasNonNull("salaryComment")) {
            pos.setSalaryComment(attributes.get("salaryComment").asText().trim());
        }
        if (attributes.hasNonNull("salaryFixLimit")) {
            pos.setSalaryFixLimit(attributes.get("salaryFixLimit").asBoolean());
        }
        if (attributes.hasNonNull("salaryCandidateRequest")) {
            pos.setSalaryCandidateRequest(attributes.get("salaryCandidateRequest").asBoolean());
        }
        if (attributes.hasNonNull("salaryIE")) {
            pos.setSalaryIE(new BigDecimal(attributes.get("salaryIE").asText().trim()));
        }
        if (attributes.hasNonNull("remoteWork")) {
            pos.setRemoteWork(attributes.get("remoteWork").asInt());
        }
        if (attributes.hasNonNull("remoteComment")) {
            pos.setRemoteComment(attributes.get("remoteComment").asText().trim());
        }
        if (attributes.hasNonNull("registrationForWork")) {
            pos.setRegistrationForWork(attributes.get("registrationForWork").asInt());
        }
        if (attributes.hasNonNull("priority")) {
            pos.setPriority(attributes.get("priority").asInt());
        }
        if (attributes.hasNonNull("priorityComment")) {
            pos.setPriorityComment(attributes.get("priorityComment").asText().trim());
        }
        if (attributes.hasNonNull("commandCandidate")) {
            pos.setCommandCandidate(attributes.get("commandCandidate").asInt());
        }
        if (attributes.hasNonNull("numberPosition")) {
            pos.setNumberPosition(attributes.get("numberPosition").asInt());
        }
        if (attributes.hasNonNull("workExperience")) {
            pos.setWorkExperience(attributes.get("workExperience").asInt());
        }
        if (attributes.hasNonNull("commandExperience")) {
            pos.setCommandExperience(attributes.get("commandExperience").asInt());
        }
        if (attributes.hasNonNull("openClose")) {
            pos.setOpenClose(attributes.get("openClose").asBoolean());
        }
        if (attributes.hasNonNull("signDraft")) {
            pos.setSignDraft(attributes.get("signDraft").asBoolean());
        }
    }

    private String executeHermesCli(String prompt) throws Exception {
        return executeHermesCli(prompt, null);
    }

    private String executeHermesCli(String prompt, HermesExecutionCandidate candidate) throws Exception {
        HunttechHermesManagerConfig config = getManagerConfig();
        List<String> command = new ArrayList<>();

        Map<String, String> envVars = new HashMap<>();
        if (candidate != null && candidate.getApiKey() != null && !candidate.getApiKey().isEmpty()) {
            String provider = candidate.getProviderCode() != null
                    ? candidate.getProviderCode().toLowerCase(Locale.ROOT) : "";
            switch (provider) {
                case "deepseek":
                    envVars.put("DEEPSEEK_API_KEY", candidate.getApiKey());
                    if (candidate.getBaseUrl() != null && !candidate.getBaseUrl().isEmpty()) {
                        envVars.put("DEEPSEEK_BASE_URL", candidate.getBaseUrl());
                    }
                    break;
                case "openai":
                    envVars.put("OPENAI_API_KEY", candidate.getApiKey());
                    if (candidate.getBaseUrl() != null && !candidate.getBaseUrl().isEmpty()) {
                        envVars.put("OPENAI_BASE_URL", candidate.getBaseUrl());
                    }
                    break;
                case "openrouter":
                    envVars.put("OPENROUTER_API_KEY", candidate.getApiKey());
                    break;
                case "anthropic":
                    envVars.put("ANTHROPIC_API_KEY", candidate.getApiKey());
                    break;
                default:
                    String normalized = provider.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "");
                    if (!normalized.isEmpty()) {
                        envVars.put(normalized + "_API_KEY", candidate.getApiKey());
                    }
                    break;
            }
        }

        List<String> hermesArgs = new ArrayList<>();
        hermesArgs.add("hermes");
        hermesArgs.add("-p");
        hermesArgs.add(config.getProfile());
        hermesArgs.add("chat");

        if (candidate != null && candidate.getProviderCode() != null && !candidate.getProviderCode().isEmpty()) {
            hermesArgs.add("--provider");
            hermesArgs.add(candidate.getProviderCode());
        }
        if (candidate != null && candidate.getModelName() != null && !candidate.getModelName().isEmpty()) {
            hermesArgs.add("-m");
            hermesArgs.add(candidate.getModelName());
        }

        hermesArgs.add("--query-file");
        hermesArgs.add("-");
        hermesArgs.add("--oneshot");
        hermesArgs.add("-Q");

        if (shouldUseSsh(config)) {
            command.add("ssh");
            command.add("-o");
            command.add("BatchMode=yes");
            command.add("-o");
            command.add("ConnectTimeout=15");
            command.add("-o");
            command.add("StrictHostKeyChecking=accept-new");
            command.add("-o");
            command.add("UserKnownHostsFile=" + SSH_KNOWN_HOSTS_PATH);
            if (config.getSshPort() != 22) {
                command.add("-p");
                command.add(String.valueOf(config.getSshPort()));
            }
            command.add(config.getSshUser() + "@" + config.getSshHost());

            StringBuilder remoteCmd = new StringBuilder();
            remoteCmd.append("docker exec -i");
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                remoteCmd.append(" -e ").append(entry.getKey()).append("=").append(shellEscape(entry.getValue()));
            }
            remoteCmd.append(" ").append(shellEscape(config.getContainerName()));
            for (String arg : hermesArgs) {
                remoteCmd.append(" ").append(shellEscape(arg));
            }
            command.add(remoteCmd.toString());
        } else {
            command.add("docker");
            command.add("exec");
            command.add("-i");
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                command.add("-e");
                command.add(entry.getKey() + "=" + entry.getValue());
            }
            command.add(config.getContainerName());
            command.addAll(hermesArgs);
        }

        List<String> maskedCommand = new ArrayList<>();
        for (String part : command) {
            maskedCommand.add(part.replaceAll("(?i)(key=)('[^']*'|[^'\\s]+)", "$1***"));
        }
        log.info("[MANAGER_HERMES] Запуск процесса Hermes CLI (timeout={}s): {}", config.getTimeoutSeconds(), String.join(" ", maskedCommand));

        long cliStart = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readStream(process.getInputStream());
            } catch (Exception e) {
                log.warn("Ошибка чтения stdout процесса Hermes Manager: {}", e.getMessage());
                return "";
            }
        });
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readStream(process.getErrorStream());
            } catch (Exception e) {
                log.warn("Ошибка чтения stderr процесса Hermes Manager: {}", e.getMessage());
                return "";
            }
        });

        try (OutputStream os = process.getOutputStream()) {
            os.write(prompt.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int timeoutSeconds = config.getTimeoutSeconds();
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - cliStart;

        if (!completed) {
            process.destroyForcibly();
            stdoutFuture.cancel(true);
            stderrFuture.cancel(true);
            log.error("[MANAGER_HERMES] Таймаут выполнения Hermes CLI после {} мс (лимит {} с)", duration, timeoutSeconds);
            throw new RuntimeException("Превышено время ожидания ответа от Hermes Agent (" + timeoutSeconds + " с)");
        }

        String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
        String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
        int exitCode = process.exitValue();

        log.info("[MANAGER_HERMES] Процесс завершился: exitCode={}, duration={} мс", exitCode, duration);

        if (exitCode != 0 && stdout.trim().isEmpty()) {
            throw new RuntimeException("Hermes CLI завершился с ошибкой (код " + exitCode + "): " + stderr);
        }

        return cleanHermesOutput(stdout);
    }

    private boolean shouldUseSsh(HunttechHermesManagerConfig config) {
        if (config == null || !config.getSshEnabled()) {
            return false;
        }
        String host = config.getSshHost();
        if (host == null || host.trim().isEmpty()) {
            return false;
        }
        String trimmedHost = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(trimmedHost) || "127.0.0.1".equals(trimmedHost) || "::1".equals(trimmedHost)) {
            return false;
        }
        if (DEFAULT_PROD_HOST.equalsIgnoreCase(trimmedHost) || DEFAULT_PROD_IP.equals(trimmedHost)) {
            File dockerSock = new File(DOCKER_SOCKET_PATH);
            if (dockerSock.exists()) {
                return false;
            }
        }
        return true;
    }

    private static String shellEscape(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String readStream(InputStream is) throws Exception {
        byte[] buffer = new byte[4096];
        StringBuilder sb = new StringBuilder();
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String cleanHermesOutput(String raw) {
        if (raw == null) {
            return "";
        }
        // Очищаем служебные предупреждения о нормализации модели если есть
        String cleaned = raw.replaceAll("(?m)^⚠️\\s+Normalized model.*$\\n?", "");
        // Очищаем служебный префикс сессии если есть
        cleaned = cleaned.replaceAll("(?m)^session_id:\\s*\\S+.*$", "").trim();
        return cleaned;
    }

    static int estimateTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private void logAiCall(ExtUser user, UUID conversationId, long durationMs,
                           String providerCode, String modelName, String credentialOwner,
                           Integer promptTokens, Integer completionTokens, Integer totalTokens,
                           BigDecimal estimatedCost, String currency,
                           String status, String errorMessage) {
        try {
            AiCallLog callLog = metadata.create(AiCallLog.class);
            callLog.setUser(user);
            if (user != null) {
                callLog.setUserLogin(user.getLogin());
                callLog.setUserName(user.getName());
            }
            callLog.setCallTime(new Date());
            callLog.setDurationMs(durationMs);
            callLog.setFunctionCode("HERMES_OPERATOR");
            callLog.setFunctionName("Hermes-operator (управление HRM)");
            callLog.setCapability("TEXT_GENERATION");
            callLog.setProviderCode(providerCode);
            callLog.setModelName(modelName);
            callLog.setCredentialOwner(credentialOwner);
            callLog.setPromptTokens(promptTokens);
            callLog.setCompletionTokens(completionTokens);
            callLog.setTotalTokens(totalTokens);
            callLog.setEstimatedCost(estimatedCost);
            callLog.setCurrency(currency != null ? currency : "USD");
            callLog.setCallerSource(conversationId != null ? "HermesManager:" + conversationId : "HermesManager");
            callLog.setStatus(status);
            callLog.setErrorMessage(AiSecuritySanitizer.sanitizeError(errorMessage));
            dataManager.commit(new CommitContext(callLog));
        } catch (Exception e) {
            log.error("[MANAGER_HERMES] Не удалось сохранить запись AiCallLog: {}", e.getMessage(), e);
        }
    }
}
