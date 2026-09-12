package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesConfig;
import com.company.hunttech.core.ai.AiCostCalculator;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.entity.ai.AiCallLog;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.service.AiSecuritySanitizer;
import com.company.hunttech.service.dto.AiUserContext;
import com.company.hunttech.service.dto.HermesChatMessage;
import com.company.hunttech.service.dto.HermesChatResponse;
import com.company.hunttech.service.dto.HermesConnectionStatus;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Реализация сервиса взаимодействия с Hermes Agent (профиль hrm-viewer в Docker).
 */
@Service(HermesChatService.NAME)
public class HermesChatServiceBean implements HermesChatService {
    private static final Logger log = LoggerFactory.getLogger(HermesChatServiceBean.class);

    private static final String HERMES_CONVERSATION_TITLE_PREFIX = "Hermes: ";
    private static final String PROVIDER_HERMES = "hermes";
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("session_id:\\s*(\\S+)");
    private static final Pattern TOKEN_LINE_PATTERN = Pattern.compile("(?i)^\\s*(?:tokens?:|token usage:|prompt[_-]?tokens?:|completion[_-]?tokens?:|total[_-]?tokens?:|tokens?\\s*used:).*");
    private static final Pattern PROMPT_TOKENS_PATTERN = Pattern.compile("(?i)(?:prompt[_-]?tokens?|input[_-]?tokens?)\\s*[:=]?\\s*([0-9]+)|([0-9]+)\\s*(?:prompt|input)(?:\\s*tokens?)?");
    private static final Pattern COMPLETION_TOKENS_PATTERN = Pattern.compile("(?i)(?:completion[_-]?tokens?|output[_-]?tokens?)\\s*[:=]?\\s*([0-9]+)|([0-9]+)\\s*(?:completion|output)(?:\\s*tokens?)?");
    private static final Pattern TOTAL_TOKENS_PATTERN = Pattern.compile("(?i)(?:total[_-]?tokens?|tokens?\\s*used)\\s*[:=]?\\s*([0-9]+)|([0-9]+)\\s*total(?:\\s*tokens?)?");

    private static final String USER_CONTEXT_HEADER = "=== Сведения пользователя (не подтверждены HRM) ===";
    private static final String USER_INSTRUCTIONS_HEADER = "=== Предпочтения и инструкции пользователя ===";
    private static final String USER_CONTEXT_PRIORITY_NOTE =
            "Приоритет: системный промпт функции имеет приоритет над сведениями пользователя.\n"
                    + "Инструкции пользователя — это предпочтения стиля и структуры; они не отменяют факты,\n"
                    + "требования, ограничения и политики, заданные системным промптом.";

    @Inject
    private Configuration configuration;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private AiSecretService aiSecretService;
    @Inject
    private UserAiContextService userAiContextService;

    @Override
    public UUID startHermesConversation() {
        ExtUser currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        // Ищем активный диалог Hermes для данного пользователя
        String title = HERMES_CONVERSATION_TITLE_PREFIX + getHermesConfig().getProfile();
        LlmChatConversation conversation = dataManager.load(LlmChatConversation.class)
                .query("select e from hunttech_LlmChatConversation e " +
                        "where e.user.id = :userId and e.title = :title and e.status = 'ACTIVE' and e.deleteTs is null " +
                        "order by e.createTs desc")
                .parameter("userId", currentUser.getId())
                .parameter("title", title)
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
    public List<HermesChatMessage> loadHermesHistory(UUID conversationId) {
        if (conversationId == null) {
            return new ArrayList<>();
        }

        ExtUser currentUser = getCurrentUser();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;

        log.debug("loadHermesHistory: convId={}, userId={}", conversationId, currentUserId);
        List<LlmChatMessage> messages = dataManager.load(LlmChatMessage.class)
                .query("select e from hunttech_LlmChatMessage e " +
                        "where e.conversation.id = :conversationId and e.conversation.user.id = :userId and e.deleteTs is null " +
                        "order by e.sequenceNo asc")
                .parameter("conversationId", conversationId)
                .parameter("userId", currentUserId)
                .view("llm-chat-message-view")
                .list();

        List<HermesChatMessage> dtoList = new ArrayList<>();
        for (LlmChatMessage msg : messages) {
            HermesChatMessage dto = new HermesChatMessage();
            dto.setId(msg.getId());
            dto.setRole("USER".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant");
            dto.setContent(msg.getContent());
            dto.setCreateTs(msg.getCreateTs());
            dto.setSequenceNo(msg.getSequenceNo());
            dto.setHermesSessionId(msg.getProviderRequestId());
            dtoList.add(dto);
        }

        log.debug("loadHermesHistory: загружено {} сообщений для convId={}", dtoList.size(), conversationId);
        return dtoList;
    }

    @Override
    public HermesChatResponse sendHermesMessage(UUID conversationId, String message) {
        long startTime = System.currentTimeMillis();
        if (conversationId == null) {
            throw new IllegalArgumentException("Идентификатор диалога не указан");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }

        ExtUser currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        LlmChatConversation conversation = dataManager.load(LlmChatConversation.class)
                .id(conversationId)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Диалог не найден: " + conversationId));

        // Вычисляем следующий sequenceNo
        Integer maxSeq = dataManager.loadValue("select max(e.sequenceNo) from hunttech_LlmChatMessage e " +
                "where e.conversation.id = :convId and e.deleteTs is null", Integer.class)
                .parameter("convId", conversationId)
                .optional()
                .orElse(0);

        // Ищем последнюю сессию Hermes для непрерывности контекста
        String lastHermesSessionId = dataManager.loadValue(
                "select e.providerRequestId from hunttech_LlmChatMessage e " +
                        "where e.conversation.id = :convId and e.providerRequestId is not null and e.deleteTs is null " +
                        "order by e.sequenceNo desc", String.class)
                .parameter("convId", conversationId)
                .optional()
                .orElse(null);

        log.info("sendHermesMessage: convId={}, пользователь={}, длина сообщения={}",
                conversationId, currentUser.getLogin(), message.length());

        // 1. Выполняем запрос к Hermes Agent с перебором моделей по утвержденному сценарию:
        //    Сначала подключается модель из пользовательских настроек (UserAiConfiguration).
        //    Если ни одна пользовательская модель не настроена или не отвечает — административные модели (AdminAiConfiguration).
        List<HermesExecutionCandidate> candidates = resolveExecutionCandidates(currentUser);
        log.info("Определено {} кандидатов для выполнения запроса к Hermes: {}", candidates.size(), candidates);

        String effectivePrompt = buildHermesUserPrompt(message.trim(), currentUser);

        String assistantText = null;
        String newSessionId = lastHermesSessionId;
        HermesExecutionCandidate successfulCandidate = null;
        HermesExecutionResult successfulResult = null;
        Exception lastException = null;

        for (int i = 0; i < candidates.size(); i++) {
            HermesExecutionCandidate candidate = candidates.get(i);
            try {
                log.info("Попытка #{}/{} выполнения запроса к Hermes через [{}]: provider={}, model={}, baseUrl={}",
                        i + 1, candidates.size(), candidate.getSource(), candidate.getProviderCode(),
                        candidate.getModelName(), candidate.getBaseUrl());
                HermesExecutionResult execResult = executeHermesCli(effectivePrompt, lastHermesSessionId, candidate);
                assistantText = execResult.cleanedText;
                if (execResult.sessionId != null && !execResult.sessionId.isEmpty()) {
                    newSessionId = execResult.sessionId;
                }
                successfulCandidate = candidate;
                successfulResult = execResult;
                log.info("Hermes успешно ответил через [{}] (provider={}, model={}, sessionId={}, responseLength={})",
                        candidate.getSource(), candidate.getProviderCode(), candidate.getModelName(),
                        newSessionId, assistantText != null ? assistantText.length() : 0);
                break;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Подключение к модели Hermes #{}/{} [{}] не ответило: {}. Пробуем следующий вариант...",
                        i + 1, candidates.size(), candidate.getSource(), ex.getMessage(), ex);
            }
        }

        if (assistantText == null) {
            long duration = System.currentTimeMillis() - startTime;
            String errorDetail = lastException != null ? lastException.getMessage() : "нет ответа от моделей";
            log.error("Все варианты подключения к модели Hermes завершились ошибкой (всего {} вариантов): {}",
                    candidates.size(), errorDetail, lastException);

            HermesExecutionCandidate lastCandidate = !candidates.isEmpty() ? candidates.get(candidates.size() - 1) : null;
            String failProvider = (lastCandidate != null && lastCandidate.getProviderCode() != null)
                    ? lastCandidate.getProviderCode() : PROVIDER_HERMES;
            String failModel = (lastCandidate != null && lastCandidate.getModelName() != null)
                    ? lastCandidate.getModelName() : getHermesConfig().getProfile();
            String failOwner = (lastCandidate != null && "USER".equalsIgnoreCase(lastCandidate.getSource()))
                    ? "USER" : "ADMIN";

            int fallbackPromptTokens = estimateTokens(message);
            AiCostCalculator.CostResult costResult = AiCostCalculator.calculateCost(
                    failProvider, failModel, fallbackPromptTokens, 0);
            logAiCall(currentUser, conversationId, duration, failProvider, failModel, failOwner,
                    fallbackPromptTokens, 0, fallbackPromptTokens, costResult.getCost(), costResult.getCurrency(),
                    "ERROR", errorDetail);

            return HermesChatResponse.error(conversationId, "Ошибка Hermes Agent: " + errorDetail, duration);
        }

        // 2. Сохраняем сообщение пользователя, ответ ассистента и состояние диалога в основном CommitContext
        String effectiveProvider = (successfulCandidate != null && successfulCandidate.getProviderCode() != null)
                ? successfulCandidate.getProviderCode() : PROVIDER_HERMES;
        String effectiveModel = (successfulCandidate != null && successfulCandidate.getModelName() != null)
                ? successfulCandidate.getModelName() : getHermesConfig().getProfile();
        String credentialOwner = (successfulCandidate != null && "USER".equalsIgnoreCase(successfulCandidate.getSource()))
                ? "USER" : "ADMIN";

        int promptTokens = (successfulResult != null && successfulResult.promptTokens != null)
                ? successfulResult.promptTokens : estimateTokens(message);
        int completionTokens = (successfulResult != null && successfulResult.completionTokens != null)
                ? successfulResult.completionTokens : estimateTokens(assistantText);
        int totalTokens = (successfulResult != null && successfulResult.totalTokens != null)
                ? successfulResult.totalTokens : (promptTokens + completionTokens);

        AiCostCalculator.CostResult costResult = AiCostCalculator.calculateCost(
                effectiveProvider, effectiveModel, promptTokens, completionTokens);

        LlmChatMessage userMsg = metadata.create(LlmChatMessage.class);
        userMsg.setConversation(conversation);
        userMsg.setRole("USER");
        userMsg.setContent(message.trim());
        userMsg.setSequenceNo(maxSeq + 1);
        userMsg.setStatus("COMPLETED");
        userMsg.setProviderCode(effectiveProvider);
        userMsg.setModelName(effectiveModel);

        LlmChatMessage assistantMsg = metadata.create(LlmChatMessage.class);
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(assistantText);
        assistantMsg.setSequenceNo(maxSeq + 2);
        assistantMsg.setStatus("COMPLETED");
        assistantMsg.setProviderCode(effectiveProvider);
        assistantMsg.setModelName(effectiveModel);
        assistantMsg.setProviderRequestId(newSessionId);

        conversation.setLastMessageAt(new Date());

        CommitContext commitContext = new CommitContext();
        commitContext.addInstanceToCommit(userMsg);
        commitContext.addInstanceToCommit(assistantMsg);
        commitContext.addInstanceToCommit(conversation);
        dataManager.commit(commitContext);

        long duration = System.currentTimeMillis() - startTime;

        // Отдельно и безопасно фиксируем технический аудит вызовов AI (AiCallLog), чтобы сбой аудита не отменял диалог
        logAiCall(currentUser, conversationId, duration, effectiveProvider, effectiveModel, credentialOwner,
                promptTokens, completionTokens, totalTokens, costResult.getCost(), costResult.getCurrency(),
                "SUCCESS", null);

        HermesChatResponse response = new HermesChatResponse(conversationId, assistantText, newSessionId, duration);
        response.setModelName(effectiveModel);
        response.setProviderCode(effectiveProvider);
        response.setPromptTokens(promptTokens);
        response.setCompletionTokens(completionTokens);
        response.setTotalTokens(totalTokens);
        response.setEstimatedCost(costResult.getCost());
        response.setCurrency(costResult.getCurrency());
        return response;
    }

    private void logAiCall(ExtUser user, UUID conversationId, long durationMs,
                           String providerCode, String modelName, String credentialOwner,
                           Integer promptTokens, Integer completionTokens, Integer totalTokens,
                           java.math.BigDecimal estimatedCost, String currency,
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
            callLog.setFunctionCode("HERMES_CHAT");
            callLog.setFunctionName("Чат с Hermes");
            callLog.setCapability("TEXT_GENERATION");
            callLog.setProviderCode(providerCode);
            callLog.setModelName(modelName);
            callLog.setCredentialOwner(credentialOwner);
            callLog.setPromptTokens(promptTokens);
            callLog.setCompletionTokens(completionTokens);
            callLog.setTotalTokens(totalTokens);
            callLog.setEstimatedCost(estimatedCost);
            callLog.setCurrency(currency);
            callLog.setCallerSource(conversationId != null ? "HermesChat:" + conversationId : "HermesChat");
            callLog.setStatus(status);
            callLog.setErrorMessage(AiSecuritySanitizer.sanitizeError(errorMessage));
            dataManager.commit(new CommitContext(callLog));
        } catch (Exception e) {
            log.error("Не удалось сохранить запись AiCallLog для Hermes: {}", e.getMessage(), e);
        }
    }

    static int estimateTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private List<HermesExecutionCandidate> resolveExecutionCandidates(ExtUser currentUser) {
        List<HermesExecutionCandidate> candidates = new ArrayList<>();

        // 1. Сначала пользовательские настройки (isPrimary DESC, priority DESC)
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
                log.warn("Ошибка загрузки пользовательских AI-конфигураций: {}", e.getMessage());
            }
        }

        // 2. Если ни одна пользовательская модель не отвечает — административные модели (priority DESC)
        try {
            List<AdminAiConfiguration> adminConfigs = dataManager.load(AdminAiConfiguration.class)
                    .query("select c from hunttech_AdminAiConfiguration c " +
                            "where (c.active is null or c.active = true) " +
                            "order by c.priority desc")
                    .list();
            for (AdminAiConfiguration ac : adminConfigs) {
                String apiKey = null;
                if (ac.getApiKeyEncrypted() != null && !ac.getApiKeyEncrypted().trim().isEmpty()) {
                    try {
                        apiKey = aiSecretService.decrypt(ac.getApiKeyEncrypted());
                    } catch (Exception e) {
                        log.warn("Не удалось расшифровать ключ AdminAiConfiguration {}: {}", ac.getName(), e.getMessage());
                    }
                }
                if (apiKey != null && !apiKey.trim().isEmpty() && ac.getProviderCode() != null) {
                    candidates.add(new HermesExecutionCandidate("ADMIN", ac.getProviderCode().trim(),
                            ac.getDefaultModelName(), apiKey.trim(), ac.getBaseApiUrl()));
                }
            }
        } catch (Exception e) {
            log.warn("Ошибка загрузки административных AI-конфигураций: {}", e.getMessage());
        }

        // 3. Резервное подключение по умолчанию из профиля контейнера
        candidates.add(new HermesExecutionCandidate("CONTAINER_DEFAULT", null, null, null, null));

        return candidates;
    }

    private String resolveUserApiKey(UserAiConfiguration configuration) {
        if (configuration.getApiKeyEncrypted() != null && !configuration.getApiKeyEncrypted().trim().isEmpty()) {
            try {
                return aiSecretService.decrypt(configuration.getApiKeyEncrypted());
            } catch (Exception e) {
                log.warn("Не удалось расшифровать ключ UserAiConfiguration: {}", e.getMessage());
            }
        }
        return configuration.getApiKey();
    }

    @Override
    public HermesConnectionStatus checkHermesConnection() {
        long startTime = System.currentTimeMillis();
        HunttechHermesConfig config = getHermesConfig();
        try {
            // Быстрая проверка через docker ps / inspect контейнера
            List<String> cmd = new ArrayList<>();
            if (config.getSshEnabled()) {
                cmd.add("ssh");
                cmd.add("-o");
                cmd.add("BatchMode=yes");
                cmd.add("-o");
                cmd.add("ConnectTimeout=5");
                cmd.add("-o");
                cmd.add("StrictHostKeyChecking=accept-new");
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

    private HermesExecutionResult executeHermesCli(String prompt, String resumeSessionId,
                                                   HermesExecutionCandidate candidate) throws Exception {
        HunttechHermesConfig config = getHermesConfig();
        List<String> command = new ArrayList<>();

        // Формируем переменные окружения для передачи в docker
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
                    envVars.put(provider.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "") + "_API_KEY", candidate.getApiKey());
                    break;
            }
        }

        // Собираем аргументы вызова hermes chat
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
        if (resumeSessionId != null && !resumeSessionId.trim().isEmpty()) {
            hermesArgs.add("--resume");
            hermesArgs.add(resumeSessionId.trim());
        }
        hermesArgs.add("--query-file");
        hermesArgs.add("-");
        hermesArgs.add("--oneshot");
        hermesArgs.add("-Q");

        if (config.getSshEnabled()) {
            command.add("ssh");
            command.add("-o");
            command.add("BatchMode=yes");
            command.add("-o");
            command.add("ConnectTimeout=15");
            command.add("-o");
            command.add("StrictHostKeyChecking=accept-new");
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
        log.info("Запуск процесса Hermes CLI (timeout={}s): {}", config.getTimeoutSeconds(), String.join(" ", maskedCommand));

        long cliStart = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        // Безопасная передача промпта через STDIN (исключает шелл-инъекции и поломку спецсимволов)
        try (OutputStream os = process.getOutputStream()) {
            os.write(prompt.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // Асинхронное чтение stdout и stderr во избежание deadlock при переполнении буфера пайпа OS
        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readStream(process.getInputStream());
            } catch (Exception e) {
                log.warn("Ошибка чтения stdout процесса Hermes: {}", e.getMessage());
                return "";
            }
        });
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readStream(process.getErrorStream());
            } catch (Exception e) {
                log.warn("Ошибка чтения stderr процесса Hermes: {}", e.getMessage());
                return "";
            }
        });

        int timeoutSeconds = config.getTimeoutSeconds();
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - cliStart;

        if (!completed) {
            process.destroyForcibly();
            stdoutFuture.cancel(true);
            stderrFuture.cancel(true);
            log.error("Таймаут выполнения Hermes CLI после {} мс (лимит {} с)", duration, timeoutSeconds);
            throw new RuntimeException("Превышено время ожидания ответа от Hermes Agent (" + timeoutSeconds + " с)");
        }

        String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
        String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
        int exitCode = process.exitValue();

        log.info("Процесс Hermes CLI завершился: exitCode={}, время={} мс, длина stdout={}, длина stderr={}",
                exitCode, duration, stdout != null ? stdout.length() : 0, stderr != null ? stderr.length() : 0);
        if (stderr != null && !stderr.trim().isEmpty()) {
            log.warn("Hermes CLI stderr (первые 300 символов): {}",
                    stderr.length() > 300 ? stderr.substring(0, 300) + "..." : stderr);
        }

        if (exitCode != 0) {
            String combinedErr = (stderr != null && !stderr.trim().isEmpty())
                    ? stderr.trim()
                    : (stdout != null && !stdout.trim().isEmpty() ? stdout.trim() : "нет вывода");
            boolean sessionNotFound = (stderr != null && stderr.contains("Session not found"))
                    || (stdout != null && stdout.contains("Session not found"));
            // Если сессия не найдена на сервере, повторяем запрос без флага --resume
            if (sessionNotFound && resumeSessionId != null) {
                log.warn("Сессия Hermes {} не найдена на сервере, повторяем запрос без --resume", resumeSessionId);
                return executeHermesCli(prompt, null, candidate);
            }
            log.warn("Hermes CLI завершился с ошибкой (exitCode={}): {}", exitCode, combinedErr);
            throw new RuntimeException("Hermes CLI завершился с ошибкой (код " + exitCode + "): " + combinedErr);
        }

        HermesExecutionResult result = parseHermesOutput(stdout);
        if (stderr != null) {
            parseTokensFromText(stderr, result);
        }
        if (result.promptTokens == null || result.promptTokens <= 0) {
            result.promptTokens = estimateTokens(prompt);
        }
        if (result.completionTokens == null || result.completionTokens <= 0) {
            result.completionTokens = estimateTokens(result.cleanedText);
        }
        if (result.totalTokens == null || result.totalTokens <= 0) {
            result.totalTokens = result.promptTokens + result.completionTokens;
        }

        if (isErrorResponse(result.cleanedText)) {
            log.warn("Ответ Hermes CLI содержит ошибку провайдера или безопасности: {}", result.cleanedText);
            throw new RuntimeException("Провайдер модели вернул ошибку: " + result.cleanedText);
        }

        log.info("Результат парсинга ответа Hermes: длина ответа={}, sessionId={}, tokens={}/{}",
                result.cleanedText != null ? result.cleanedText.length() : 0, result.sessionId,
                result.promptTokens, result.completionTokens);
        return result;
    }

    private String shellEscape(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    boolean isErrorResponse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase();
        boolean hasJsonError = lower.startsWith("{")
                && lower.contains("\"error\"")
                && (lower.contains("\"success\":false") || lower.contains("\"success\": false") || lower.contains("\"status\":\"error\""));

        return trimmed.startsWith("HTTP 403:")
                || trimmed.startsWith("HTTP 401:")
                || trimmed.startsWith("HTTP 429:")
                || trimmed.startsWith("HTTP 500:")
                || trimmed.startsWith("HTTP 502:")
                || trimmed.startsWith("HTTP 503:")
                || hasJsonError
                || lower.startsWith("access denied by security policy")
                || trimmed.startsWith("AuthenticationError:")
                || trimmed.startsWith("Error: No API key found");
    }

    public static class HermesExecutionCandidate {
        private final String source;
        private final String providerCode;
        private final String modelName;
        private final String apiKey;
        private final String baseUrl;

        public HermesExecutionCandidate(String source, String providerCode, String modelName, String apiKey, String baseUrl) {
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

        @Override
        public String toString() {
            return "[" + source + "] " + (providerCode != null ? providerCode : "default") +
                    (modelName != null ? " (" + modelName + ")" : "");
        }
    }

    private HermesExecutionResult parseHermesOutput(String rawOutput) {
        HermesExecutionResult result = new HermesExecutionResult();
        if (rawOutput == null) {
            result.cleanedText = "";
            return result;
        }

        String[] lines = rawOutput.split("\r?\n");
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            Matcher m = SESSION_ID_PATTERN.matcher(line);
            if (m.find()) {
                result.sessionId = m.group(1).trim();
                continue;
            }

            // Фильтрация технических баннеров Hermes
            String trimmed = line.trim();
            if (trimmed.startsWith("⚠️") || trimmed.startsWith("⚠") || trimmed.startsWith("↻")) {
                continue;
            }
            if (trimmed.startsWith("Warning:") || trimmed.startsWith("Normalized model")) {
                continue;
            }

            // Парсинг информации о токенах и фильтрация строк служебной статистики токенов
            boolean isTokenLine = parseTokensFromLine(line, result);
            if (isTokenLine || trimmed.toLowerCase().startsWith("tokens:") || trimmed.toLowerCase().startsWith("token usage:")) {
                continue;
            }

            sb.append(line).append("\n");
        }

        result.cleanedText = sb.toString().trim();
        return result;
    }

    private boolean parseTokensFromLine(String line, HermesExecutionResult result) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        String trimmed = line.trim();
        boolean isExplicitTokenLine = TOKEN_LINE_PATTERN.matcher(trimmed).matches();

        boolean matched = false;
        Matcher mPrompt = PROMPT_TOKENS_PATTERN.matcher(line);
        if (mPrompt.find()) {
            matched = true;
            if (result.promptTokens == null) {
                try {
                    String val = mPrompt.group(1) != null ? mPrompt.group(1) : mPrompt.group(2);
                    if (val != null) {
                        result.promptTokens = Integer.parseInt(val.trim());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        Matcher mComp = COMPLETION_TOKENS_PATTERN.matcher(line);
        if (mComp.find()) {
            matched = true;
            if (result.completionTokens == null) {
                try {
                    String val = mComp.group(1) != null ? mComp.group(1) : mComp.group(2);
                    if (val != null) {
                        result.completionTokens = Integer.parseInt(val.trim());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        Matcher mTotal = TOTAL_TOKENS_PATTERN.matcher(line);
        if (mTotal.find()) {
            matched = true;
            if (result.totalTokens == null) {
                try {
                    String val = mTotal.group(1) != null ? mTotal.group(1) : mTotal.group(2);
                    if (val != null) {
                        result.totalTokens = Integer.parseInt(val.trim());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return isExplicitTokenLine || (matched && trimmed.toLowerCase().contains("token"));
    }

    private void parseTokensFromText(String text, HermesExecutionResult result) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        for (String line : text.split("\r?\n")) {
            parseTokensFromLine(line, result);
        }
    }

    private String readStream(java.io.InputStream is) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    private ExtUser getCurrentUser() {
        if (userSessionSource == null || userSessionSource.getUserSession() == null) {
            return null;
        }
        User user = userSessionSource.getUserSession().getUser();
        if (user instanceof ExtUser) {
            return (ExtUser) user;
        }
        return dataManager.load(ExtUser.class).id(user.getId()).optional().orElse(null);
    }

    private HunttechHermesConfig getHermesConfig() {
        return configuration.getConfig(HunttechHermesConfig.class);
    }

    /**
     * Формирует промпт для Hermes Agent с включением персонализированного контекста
     * текущего пользователя (ExtUser и UserAiProfile), чтобы Hermes знал с кем общается
     * и давал ответы исходя из персональных предпочтений пользователя.
     * Строго соблюдает политику согласий и бюджет токенов через единый UserAiContextService.
     */
    String buildHermesUserPrompt(String userMessage, ExtUser currentUser) {
        if (currentUser == null) {
            return userMessage;
        }

        AiUserContext userCtx = null;
        try {
            userCtx = userAiContextService.buildCurrentUserContext();
        } catch (Exception e) {
            log.warn("Не удалось получить AiUserContext через UserAiContextService: {}", e.getMessage(), e);
        }

        if (userCtx == null || userCtx.isEmpty()) {
            return userMessage;
        }

        StringBuilder contextBlock = new StringBuilder();
        contextBlock.append(USER_CONTEXT_HEADER).append("\n");
        String userName = userCtx.getProfileData().get("userName");
        if (userName != null && !userName.trim().isEmpty()) {
            contextBlock.append("- Имя пользователя: ").append(userName.trim()).append("\n");
        }
        for (Map.Entry<String, String> entry : userCtx.getProfileData().entrySet()) {
            if ("userName".equals(entry.getKey())) {
                continue;
            }
            contextBlock.append("- ").append(translateProfileKey(entry.getKey())).append(": ").append(entry.getValue()).append("\n");
        }
        contextBlock.append("\n").append(USER_INSTRUCTIONS_HEADER).append("\n");
        if (userCtx.getCustomInstructions().isEmpty()) {
            contextBlock.append("- не заданы\n");
        } else {
            for (String instruction : userCtx.getCustomInstructions()) {
                contextBlock.append("- ").append(instruction).append("\n");
            }
        }

        contextBlock.append("\n").append(USER_CONTEXT_PRIORITY_NOTE).append("\n\n");

        contextBlock.append("=== ПРАВИЛА БЕЗОПАСНОСТИ И НАВИГАЦИИ В HRM ===\n");
        contextBlock.append("1. СТРОГИЙ ЗАПРЕТ НА УДАЛЕНИЕ: Тебе категорически запрещено удалять любые данные из базы данных (любые операции DELETE, DROP, TRUNCATE, soft-delete, удаление записей) для любого пользователя системы. На любые запросы об удалении отвечай вежливым отказом и пояснением, что операции удаления в чате строго заблокированы политикой безопасности.\n");
        contextBlock.append("2. НАВИГАЦИЯ: Если требуется дать ссылку на карточку в системе HRM, используй формат ссылок: [Текст](hrm://vacancy/<UUID>), [Текст](hrm://candidate/<UUID>), [Текст](hrm://cv/<UUID>), [Текст](hrm://interaction/<UUID>).\n\n");

        contextBlock.append("=== Запрос пользователя ===\n");
        contextBlock.append(userMessage);

        return contextBlock.toString();
    }

    private String translateProfileKey(String key) {
        if (key == null) {
            return "";
        }
        switch (key) {
            case "preferredLanguage": return "Предпочитаемый язык ответов";
            case "responseDetailLevel": return "Уровень детализации ответов";
            case "communicationStyle": return "Стиль общения";
            case "terminologyLevel": return "Уровень терминологии";
            case "preferredAnswerStructure": return "Предпочитаемая структура ответов";
            case "communicationConstraints": return "Ограничения в общении";
            case "currentPosition": return "Должность";
            case "functionalRole": return "Функциональная роль";
            case "seniorityLevel": return "Уровень квалификации (Seniority)";
            case "professionalExperienceYears": return "Опыт работы (лет)";
            case "recruitingExperienceYears": return "Опыт в рекрутинге (лет)";
            case "candidateLevels": return "Уровни кандидатов";
            case "aboutMe": return "О себе";
            case "currentResponsibilities": return "Текущие обязанности";
            case "decisionPriorities": return "Приоритеты принятия решений";
            case "targetRoles": return "Целевые роли";
            case "hiringGeographies": return "Географии найма";
            case "clientAndProjectContext": return "Контекст клиентов и проектов";
            case "domainExpertise": return "Экспертиза";
            case "industries": return "Отрасли";
            case "recruitingSpecializations": return "Специализации рекрутинга";
            case "professionalGoals": return "Профессиональные цели";
            case "professionalInterests": return "Профессиональные интересы";
            case "developmentAreas": return "Зоны развития";
            case "currentPriorities": return "Текущие приоритеты";
            case "education": return "Образование";
            case "certifications": return "Сертификаты";
            default: return key;
        }
    }

    static class HermesExecutionResult {
        String cleanedText = "";
        String sessionId = null;
        Integer promptTokens;
        Integer completionTokens;
        Integer totalTokens;
    }
}
