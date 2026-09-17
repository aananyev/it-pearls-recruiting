package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesManagerConfig;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Project;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
                .list();

        List<HermesChatMessage> result = new ArrayList<>();
        for (LlmChatMessage msg : messages) {
            HermesChatMessage dto = new HermesChatMessage();
            dto.setId(msg.getId());
            dto.setRole(msg.getRole());
            dto.setContent(msg.getContent());
            dto.setCreateTs(msg.getCreateTs());
            dto.setSequenceNo(msg.getSequenceNo());
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

        // Загружаем или создаем беседу
        LlmChatConversation conversation = dataManager.load(LlmChatConversation.class)
                .id(conversationId)
                .optional()
                .orElse(null);

        if (conversation == null) {
            conversationId = startManagerHermesConversation();
            conversation = dataManager.load(LlmChatConversation.class).id(conversationId).one();
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

        // Формируем системный запрос для Manager Hermes
        String prompt = buildManagerPrompt(messageText.trim());

        String rawResponseText;
        try {
            rawResponseText = executeHermesCli(prompt);
        } catch (Exception e) {
            log.error("[MANAGER_HERMES] Сбой при вызове Hermes Agent", e);
            String errorMsg = "Ошибка связи с Hermes-контуром управления: " + e.getMessage();
            saveAssistantMessage(conversation, errorMsg, nextSeq + 1);
            return HermesChatResponse.error(conversationId, errorMsg, 0);
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

        return new HermesChatResponse(conversationId, processedResponseText, null, 0);
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
        HunttechHermesManagerConfig config = getManagerConfig();
        List<String> command = new ArrayList<>();

        List<String> hermesArgs = new ArrayList<>();
        hermesArgs.add("hermes");
        hermesArgs.add("-p");
        hermesArgs.add(config.getProfile());
        hermesArgs.add("chat");
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
            remoteCmd.append(" ").append(shellEscape(config.getContainerName()));
            for (String arg : hermesArgs) {
                remoteCmd.append(" ").append(shellEscape(arg));
            }
            command.add(remoteCmd.toString());
        } else {
            command.add("docker");
            command.add("exec");
            command.add("-i");
            command.add(config.getContainerName());
            command.addAll(hermesArgs);
        }

        log.info("[MANAGER_HERMES] Запуск процесса Hermes CLI (timeout={}s): {}", config.getTimeoutSeconds(), String.join(" ", command));

        long cliStart = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        try (OutputStream os = process.getOutputStream()) {
            os.write(prompt.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

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
        // Очищаем служебный префикс сессии если есть
        String cleaned = raw.replaceAll("(?m)^session_id:\\s*\\S+.*$", "").trim();
        return cleaned;
    }
}
