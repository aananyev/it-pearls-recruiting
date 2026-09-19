package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesConfig;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.OpenPositionSkill;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.entity.ai.OpenPositionAiExplanationLog;
import com.company.hunttech.service.dto.OpenPositionExplanationResult;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Реализация сервиса интеллектуального объяснения требований вакансий для рекрутеров.
 */
@Service(OpenPositionExplanationService.NAME)
public class OpenPositionExplanationServiceBean implements OpenPositionExplanationService {
    private static final Logger log = LoggerFactory.getLogger(OpenPositionExplanationServiceBean.class);

    private static final String SSH_KNOWN_HOSTS_PATH = "/tmp/hermes_known_hosts";
    private static final String FUNCTION_STANDARD = "VACANCY_EXPLAIN_REQUIREMENTS";

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private AiExecutionService aiExecutionService;
    @Inject
    private Configuration configuration;

    @Override
    public OpenPositionExplanationResult explainRequirements(UUID openPositionId) {
        long startTime = System.currentTimeMillis();
        log.info("explainRequirements: старт анализа требований вакансии id={}", openPositionId);

        OpenPosition openPosition = loadOpenPosition(openPositionId);
        if (openPosition == null) {
            log.error("explainRequirements: вакансия id={} не найдена", openPositionId);
            return OpenPositionExplanationResult.error("Вакансия не найдена", "STANDARD", "AI_EXECUTION_SERVICE", 0L);
        }

        Map<String, Object> context = buildExplanationContext(openPosition, null);
        String requestContextString = formatContextForAudit(context);

        try {
            AiExecutionResult execResult = aiExecutionService.executeText(FUNCTION_STANDARD, context);
            long duration = System.currentTimeMillis() - startTime;

            if (execResult == null || execResult.getText() == null || execResult.getText().trim().isEmpty()) {
                String err = "Сервис AI вернул пустой результат";
                log.warn("explainRequirements: {} для вакансии id={}", err, openPositionId);
                saveExplanationLog(
                        openPosition, "STANDARD", "AI_EXECUTION_SERVICE",
                        execResult != null ? execResult.getModelName() : "unknown",
                        execResult != null ? execResult.getProviderCode() : "unknown",
                        duration,
                        execResult != null ? execResult.getPromptTokens() : null,
                        execResult != null ? execResult.getCompletionTokens() : null,
                        execResult != null ? execResult.getTotalTokens() : null,
                        requestContextString, null, "{\"error\": \"Empty result\"}",
                        "ERROR", err
                );
                return OpenPositionExplanationResult.error(err, "STANDARD", "AI_EXECUTION_SERVICE", duration);
            }

            String responseText = cleanAiOutput(execResult.getText());
            String technicalInfo = String.format("{\"functionCode\":\"%s\",\"credentialOwner\":\"%s\",\"providerRequestId\":\"%s\"}",
                    FUNCTION_STANDARD,
                    execResult.getCredentialOwner() != null ? execResult.getCredentialOwner().name() : "ADMIN",
                    execResult.getProviderRequestId() != null ? execResult.getProviderRequestId() : "");

            OpenPositionAiExplanationLog logEntry = saveExplanationLog(
                    openPosition, "STANDARD", "AI_EXECUTION_SERVICE",
                    execResult.getModelName(),
                    execResult.getProviderCode(),
                    duration,
                    execResult.getPromptTokens(),
                    execResult.getCompletionTokens(),
                    execResult.getTotalTokens(),
                    requestContextString, responseText, technicalInfo,
                    "SUCCESS", null
            );

            UUID logId = logEntry != null ? logEntry.getId() : null;

            log.info("explainRequirements: успешно сгенерировано объяснение для вакансии id={}, duration={}ms, model={}",
                    openPositionId, duration, execResult.getModelName());

            return OpenPositionExplanationResult.success(
                    responseText, "STANDARD", "AI_EXECUTION_SERVICE",
                    execResult.getModelName(), execResult.getProviderCode(),
                    duration,
                    execResult.getPromptTokens(), execResult.getCompletionTokens(), execResult.getTotalTokens(),
                    logId
            );

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("explainRequirements: ошибка выполнения AI-сервиса для вакансии id={}: {}",
                    openPositionId, ex.getMessage(), ex);

            saveExplanationLog(
                    openPosition, "STANDARD", "AI_EXECUTION_SERVICE",
                    null, null, duration, null, null, null,
                    requestContextString, null, "{\"exception\": \"" + escapeJson(ex.getMessage()) + "\"}",
                    "ERROR", ex.getMessage()
            );

            return OpenPositionExplanationResult.error("Ошибка AI сервиса: " + ex.getMessage(),
                    "STANDARD", "AI_EXECUTION_SERVICE", duration);
        }
    }

    @Override
    public OpenPositionExplanationResult explainSimplifiedWithWebSearch(UUID openPositionId, String previousExplanation) {
        long startTime = System.currentTimeMillis();
        log.info("explainSimplifiedWithWebSearch: старт упрощенного объяснения через Hermes hrm-viewer для вакансии id={}", openPositionId);

        OpenPosition openPosition = loadOpenPosition(openPositionId);
        if (openPosition == null) {
            log.error("explainSimplifiedWithWebSearch: вакансия id={} не найдена", openPositionId);
            return OpenPositionExplanationResult.error("Вакансия не найдена", "SIMPLIFIED_ANALOGY", "HERMES_HRM_VIEWER", 0L);
        }

        Map<String, Object> context = buildExplanationContext(openPosition, previousExplanation);
        String prompt = buildHermesSimplifiedPrompt(context);

        try {
            HermesCliResult cliResult = executeHermesHrmViewer(prompt);
            long duration = System.currentTimeMillis() - startTime;

            if (cliResult == null || cliResult.text == null || cliResult.text.trim().isEmpty()) {
                String err = cliResult != null && cliResult.errorMessage != null
                        ? cliResult.errorMessage : "Hermes hrm-viewer не вернул результат";
                log.warn("explainSimplifiedWithWebSearch: {} для вакансии id={}", err, openPositionId);

                saveExplanationLog(
                        openPosition, "SIMPLIFIED_ANALOGY", "HERMES_HRM_VIEWER",
                        "deepseek/deepseek-chat", "hermes", duration,
                        null, null, null,
                        prompt, null, "{\"error\": \"" + escapeJson(err) + "\"}",
                        "ERROR", err
                );
                return OpenPositionExplanationResult.error(err, "SIMPLIFIED_ANALOGY", "HERMES_HRM_VIEWER", duration);
            }

            String responseText = cleanAiOutput(cliResult.text);
            String technicalInfo = String.format("{\"container\":\"%s\",\"profile\":\"%s\",\"exitCode\":%d}",
                    getHermesConfig().getViewerContainerName(),
                    getHermesConfig().getViewerProfile(),
                    cliResult.exitCode);

            int estimatedPromptTokens = estimateTokens(prompt);
            int estimatedCompletionTokens = estimateTokens(responseText);

            OpenPositionAiExplanationLog logEntry = saveExplanationLog(
                    openPosition, "SIMPLIFIED_ANALOGY", "HERMES_HRM_VIEWER",
                    "deepseek/deepseek-chat", "hermes", duration,
                    estimatedPromptTokens, estimatedCompletionTokens, estimatedPromptTokens + estimatedCompletionTokens,
                    prompt, responseText, technicalInfo,
                    "SUCCESS", null
            );

            UUID logId = logEntry != null ? logEntry.getId() : null;

            log.info("explainSimplifiedWithWebSearch: успешно получено житейское объяснение для вакансии id={}, duration={}ms",
                    openPositionId, duration);

            return OpenPositionExplanationResult.success(
                    responseText, "SIMPLIFIED_ANALOGY", "HERMES_HRM_VIEWER",
                    "deepseek/deepseek-chat", "hermes", duration,
                    estimatedPromptTokens, estimatedCompletionTokens, estimatedPromptTokens + estimatedCompletionTokens,
                    logId
            );

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("explainSimplifiedWithWebSearch: сбой при вызове Hermes hrm-viewer для вакансии id={}: {}",
                    openPositionId, ex.getMessage(), ex);

            saveExplanationLog(
                    openPosition, "SIMPLIFIED_ANALOGY", "HERMES_HRM_VIEWER",
                    "deepseek/deepseek-chat", "hermes", duration,
                    null, null, null,
                    prompt, null, "{\"exception\": \"" + escapeJson(ex.getMessage()) + "\"}",
                    "ERROR", ex.getMessage()
            );

            return OpenPositionExplanationResult.error("Ошибка вызова Hermes: " + ex.getMessage(),
                    "SIMPLIFIED_ANALOGY", "HERMES_HRM_VIEWER", duration);
        }
    }

    @Override
    public OpenPositionExplanationResult getLatestExplanation(UUID openPositionId, String explanationType) {
        if (openPositionId == null) {
            return null;
        }

        try {
            String queryStr = "select e from hunttech_OpenPositionAiExplanationLog e " +
                    "where e.openPosition.id = :opId and e.status = 'SUCCESS' " +
                    (explanationType != null ? "and e.explanationType = :expType " : "") +
                    "order by e.callTime desc";

            FluentLoader.ByQuery<OpenPositionAiExplanationLog, UUID> query = dataManager.load(OpenPositionAiExplanationLog.class)
                    .query(queryStr)
                    .parameter("opId", openPositionId);

            if (explanationType != null) {
                query.parameter("expType", explanationType);
            }

            OpenPositionAiExplanationLog logEntry = query.view(View.LOCAL)
                    .maxResults(1)
                    .optional()
                    .orElse(null);

            if (logEntry == null || logEntry.getResponseContent() == null || logEntry.getResponseContent().trim().isEmpty()) {
                return null;
            }

            OpenPositionExplanationResult result = OpenPositionExplanationResult.success(
                    logEntry.getResponseContent(),
                    logEntry.getExplanationType(),
                    logEntry.getServiceType(),
                    logEntry.getModelName(),
                    logEntry.getProviderCode(),
                    logEntry.getDurationMs(),
                    logEntry.getPromptTokens(),
                    logEntry.getCompletionTokens(),
                    logEntry.getTotalTokens(),
                    logEntry.getId()
            );
            result.setFromCache(true);
            result.setCallTime(logEntry.getCallTime());
            log.info("getLatestExplanation: найдено ранее сгенерированное объяснение в БД для вакансии id={}, logId={}, type={}, time={}",
                    openPositionId, logEntry.getId(), logEntry.getExplanationType(), logEntry.getCallTime());
            return result;
        } catch (Exception e) {
            log.error("getLatestExplanation: сбой поиска сохраненного объяснения для вакансии id={}: {}",
                    openPositionId, e.getMessage(), e);
            return null;
        }
    }

    private OpenPosition loadOpenPosition(UUID id) {
        try {
            return dataManager.load(OpenPosition.class)
                    .id(id)
                    .view("openPosition-view")
                    .one();
        } catch (Exception e) {
            log.error("loadOpenPosition: ошибка загрузки вакансии id={}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> buildExplanationContext(OpenPosition op, String previousExplanation) {
        Map<String, Object> context = new HashMap<>();
        context.put("vacancyName", safeStr(op.getVacansyName()));
        context.put("grade", op.getGrade() != null ? safeStr(op.getGrade().getGradeName()) : "");
        context.put("projectName", op.getProjectName() != null ? safeStr(op.getProjectName().getProjectName()) : "");
        context.put("skills", resolveSkillsText(op));
        context.put("comment", safeStr(op.getComment()));
        context.put("interviewChecklist", safeStr(op.getInterviewChecklist()));
        context.put("searchMap", safeStr(op.getSearchMap()));
        if (previousExplanation != null) {
            context.put("previousExplanation", previousExplanation);
        }
        return context;
    }

    private String resolveSkillsText(OpenPosition op) {
        try {
            List<OpenPositionSkill> posSkills = dataManager.load(OpenPositionSkill.class)
                    .query("select e from hunttech_OpenPositionSkill e where e.openPosition = :op order by e.priority, e.skill.skillName")
                    .parameter("op", op)
                    .view("openPositionSkill-view")
                    .list();

            if (!posSkills.isEmpty()) {
                return posSkills.stream()
                        .filter(s -> s.getSkill() != null && s.getSkill().getSkillName() != null)
                        .map(s -> s.getSkill().getSkillName())
                        .collect(Collectors.joining(", "));
            }
        } catch (Exception e) {
            log.warn("resolveSkillsText: не удалось загрузить OpenPositionSkill для вакансии {}: {}", op.getId(), e.getMessage());
        }

        if (op.getSkillsList() != null && !op.getSkillsList().isEmpty()) {
            return op.getSkillsList().stream()
                    .filter(s -> s.getSkillName() != null)
                    .map(SkillTree::getSkillName)
                    .collect(Collectors.joining(", "));
        }

        return safeStr(op.getShortDescription());
    }

    private String buildHermesSimplifiedPrompt(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты — мастер простых бытовых объяснений и ментор для IT-рекрутеров HRM HuntTech.\n");
        sb.append("Твоя задача — объяснить рекрутеру сложнейшие технические требования вакансии, технологии и стек на ярких житейских примерах, бытовых аналогиях и реальном интернет-контексте (как если бы ты объяснял бабушке или гуманитарию, но без потери технической сути).\n\n");

        sb.append("ДАННЫЕ ВАКАНСИИ:\n");
        sb.append("Название вакансии: ").append(context.get("vacancyName")).append("\n");
        sb.append("Грейд: ").append(context.get("grade")).append("\n");
        sb.append("Проект: ").append(context.get("projectName")).append("\n");
        sb.append("Стек и навыки: ").append(context.get("skills")).append("\n\n");

        sb.append("Описание вакансии:\n").append(context.get("comment")).append("\n\n");

        if (context.get("searchMap") != null && !((String) context.get("searchMap")).isEmpty()) {
            sb.append("Карта поиска:\n").append(context.get("searchMap")).append("\n\n");
        }

        if (context.get("previousExplanation") != null && !((String) context.get("previousExplanation")).isEmpty()) {
            sb.append("Предыдущее объяснение (сделай его еще более образным, простым и наглядным):\n")
                    .append(context.get("previousExplanation")).append("\n\n");
        }

        sb.append("СТРУКТУРА ОТВЕТА:\n");
        sb.append("1. 🏠 Житейская аналогия роли: представь эту специальность через понятную бытовую профессию или жизненную ситуацию (например: архитектор как проектировщик многоэтажки, backend-разработчик как шеф-повар на кухне ресторана, DevOps как служба доставки и логистики).\n");
        sb.append("2. 🧰 Технологический стек на пальцах: для каждой ключевой технологии и термина из описания вакансии и карты поиска приведи простую бытовую метафору (что это за инструмент в реальной жизни и зачем он нужен).\n");
        sb.append("3. 🌐 Контекст из индустрии и интернета: найди свежую информацию о том, где и как эта технология применяется в 2026 году, какие продукты на ней создают и почему за таких людей борются компании.\n");
        sb.append("4. 💡 Памятка рекрутеру на скрининге: 3-4 простых бытовых вопроса кандидату, по ответам на которые сразу ясно, понимает ли он реальную работу или просто заучил термины.\n\n");
        sb.append("Будь дружелюбен, остроумен, выражайся кристально ясно и понятно!");

        return sb.toString();
    }

    private HermesCliResult executeHermesHrmViewer(String prompt) throws Exception {
        HunttechHermesConfig config = getHermesConfig();
        List<String> command = new ArrayList<>();

        List<String> hermesArgs = new ArrayList<>();
        hermesArgs.add("hermes");
        hermesArgs.add("-p");
        hermesArgs.add(config.getViewerProfile());
        hermesArgs.add("chat");
        hermesArgs.add("--oneshot");
        hermesArgs.add("-Q");
        hermesArgs.add("--query-file");
        hermesArgs.add("-");

        if (config.getSshEnabled()) {
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
            remoteCmd.append("docker exec -i ").append(shellEscape(config.getViewerContainerName()));
            for (String arg : hermesArgs) {
                remoteCmd.append(" ").append(shellEscape(arg));
            }
            command.add(remoteCmd.toString());
        } else {
            command.add("docker");
            command.add("exec");
            command.add("-i");
            command.add(config.getViewerContainerName());
            command.addAll(hermesArgs);
        }

        log.info("executeHermesHrmViewer: запуск Hermes CLI в контейнере {}", config.getViewerContainerName());

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

        try (OutputStream os = process.getOutputStream()) {
            os.write(prompt.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int timeoutSec = config.getViewerTimeoutSeconds() > 0 ? config.getViewerTimeoutSeconds() : 120;
        boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            stdoutFuture.cancel(true);
            stderrFuture.cancel(true);
            return new HermesCliResult(false, null, -1, "Превышен таймаут ожидания Hermes (" + timeoutSec + " с)");
        }

        int exitCode = process.exitValue();
        String stdout = "";
        String stderr = "";
        try {
            stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Не удалось дождаться чтения stdout: {}", e.getMessage());
        }
        try {
            stderr = stderrFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Не удалось дождаться чтения stderr: {}", e.getMessage());
        }

        if (exitCode == 0 && stdout != null && !stdout.trim().isEmpty()) {
            return new HermesCliResult(true, stdout, exitCode, null);
        } else {
            String err = (stderr != null && !stderr.trim().isEmpty()) ? stderr : stdout;
            return new HermesCliResult(false, stdout, exitCode, err);
        }
    }

    private OpenPositionAiExplanationLog saveExplanationLog(
            OpenPosition openPosition,
            String explanationType,
            String serviceType,
            String modelName,
            String providerCode,
            Long durationMs,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String requestContext,
            String responseContent,
            String technicalInfo,
            String status,
            String errorMessage) {
        try {
            OpenPositionAiExplanationLog logEntry = metadata.create(OpenPositionAiExplanationLog.class);
            logEntry.setOpenPosition(openPosition);

            User currentUser = null;
            try {
                if (userSessionSource != null && userSessionSource.checkCurrentUserSession()) {
                    currentUser = userSessionSource.getUserSession().getUser();
                }
            } catch (Exception ignored) {
            }

            if (currentUser != null) {
                logEntry.setUser(currentUser);
                logEntry.setUserLogin(currentUser.getLogin());
                logEntry.setUserName(currentUser.getName());
            }

            logEntry.setCallTime(new Date());
            logEntry.setExplanationType(explanationType);
            logEntry.setServiceType(serviceType);
            logEntry.setModelName(modelName);
            logEntry.setProviderCode(providerCode);
            logEntry.setDurationMs(durationMs);
            logEntry.setPromptTokens(promptTokens);
            logEntry.setCompletionTokens(completionTokens);
            logEntry.setTotalTokens(totalTokens);
            logEntry.setRequestContext(requestContext);
            logEntry.setResponseContent(responseContent);
            logEntry.setTechnicalInfo(technicalInfo);
            logEntry.setStatus(status);
            logEntry.setErrorMessage(errorMessage != null && errorMessage.length() > 1000
                    ? errorMessage.substring(0, 1000) : errorMessage);

            return dataManager.commit(logEntry);
        } catch (Exception e) {
            log.error("saveExplanationLog: сбой сохранения записи в HUNTTECH_OP_AI_EXPLANATION_LOG: {}", e.getMessage(), e);
            return null;
        }
    }

    private HunttechHermesConfig getHermesConfig() {
        return configuration.getConfig(HunttechHermesConfig.class);
    }

    private String shellEscape(String s) {
        if (s == null) return "''";
        return "'" + s.replace("'", "'\\''") + "'";
    }

    private String readStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "";
        }
    }

    private String cleanAiOutput(String text) {
        if (text == null) return "";
        return text.trim();
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    private String formatContextForAudit(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        context.forEach((k, v) -> {
            sb.append(k).append(": ").append(v != null ? v.toString() : "").append("\n");
        });
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.max(1, text.length() / 4);
    }

    private static class HermesCliResult {
        final boolean success;
        final String text;
        final int exitCode;
        final String errorMessage;

        HermesCliResult(boolean success, String text, int exitCode, String errorMessage) {
            this.success = success;
            this.text = text;
            this.exitCode = exitCode;
            this.errorMessage = errorMessage;
        }
    }
}
