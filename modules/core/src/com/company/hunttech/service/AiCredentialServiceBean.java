package com.company.hunttech.service;

import com.company.hunttech.core.ai.AIProvider;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.ai.AiProviderCatalog;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Security;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Реализация защищённых административных операций с корпоративными AI credentials.
 *
 * Specific permission проверяется на middleware, поэтому один только screen grant
 * не позволяет вызвать шифрование/тест корпоративного credential через сервис.
 */
@Service(AiCredentialService.NAME)
public class AiCredentialServiceBean implements AiCredentialService {
    public static final String MANAGE_CORPORATE_CREDENTIALS_PERMISSION =
            "hunttech.ai.manageCorporateCredentials";

    @Inject
    private DataManager dataManager;
    @Inject
    private Security security;
    @Inject
    private AIProviderRegistry aiProviderRegistry;
    @Inject
    private AiSecretService aiSecretService;

    @Override
    public String encryptAdminSecret(String plainText) {
        requireAdminPermission();
        if (!isConfigured(plainText)) {
            throw new DevelopmentException("Новый API-ключ не задан.");
        }
        return aiSecretService.encrypt(plainText);
    }

    @Override
    public String encryptUserSecret(String plainText) {
        if (!isConfigured(plainText)) {
            throw new DevelopmentException("Новый персональный API-ключ не задан.");
        }
        return aiSecretService.encrypt(plainText);
    }

    @Override
    public int migrateLegacyUserSecrets() {
        requireAdminPermission();
        List<UserAiConfiguration> legacyConfigurations = dataManager.load(UserAiConfiguration.class)
                .query("select e from hunttech_UserAiConfiguration e "
                        + "where e.apiKey is not null and trim(e.apiKey) <> ''")
                .view("user-ai-configuration-ai-execution-view")
                .list();
        int migrated = 0;
        for (UserAiConfiguration configuration : legacyConfigurations) {
            String legacySecret = configuration.getApiKey();
            if (!isConfigured(legacySecret)) {
                continue;
            }
            configuration.setApiKeyEncrypted(aiSecretService.encrypt(legacySecret));
            configuration.setApiKey(null);
            dataManager.commit(configuration);
            migrated++;
        }
        return migrated;
    }

    @Override
    public int rotateSecrets() {
        requireAdminPermission();
        int rotated = 0;
        List<UserAiConfiguration> userConfigurations = dataManager.load(UserAiConfiguration.class)
                .query("select e from hunttech_UserAiConfiguration e "
                        + "where e.apiKeyEncrypted is not null and trim(e.apiKeyEncrypted) <> ''")
                .view("user-ai-configuration-ai-execution-view")
                .list();
        for (UserAiConfiguration configuration : userConfigurations) {
            String current = configuration.getApiKeyEncrypted();
            String rotatedValue = aiSecretService.rotate(current);
            if (!rotatedValue.equals(current)) {
                configuration.setApiKeyEncrypted(rotatedValue);
                dataManager.commit(configuration);
                rotated++;
            }
        }

        List<AdminAiConfiguration> adminConfigurations = dataManager.load(AdminAiConfiguration.class)
                .query("select e from hunttech_AdminAiConfiguration e "
                        + "where e.apiKeyEncrypted is not null and trim(e.apiKeyEncrypted) <> ''")
                .view("admin-ai-configuration-secret-view")
                .list();
        for (AdminAiConfiguration configuration : adminConfigurations) {
            String current = configuration.getApiKeyEncrypted();
            String rotatedValue = aiSecretService.rotate(current);
            if (!rotatedValue.equals(current)) {
                configuration.setApiKeyEncrypted(rotatedValue);
                dataManager.commit(configuration);
                rotated++;
            }
        }
        return rotated;
    }

    @Override
    public void testAdminConnection(UUID configurationId) {
        requireAdminPermission();
        if (configurationId == null) {
            throw new DevelopmentException("Не выбрано корпоративное AI-подключение.");
        }

        AdminAiConfiguration configuration = dataManager.load(AdminAiConfiguration.class)
                .id(configurationId)
                .view("admin-ai-configuration-secret-view")
                .one();
        validateConfiguration(configuration);

        AiConnectionTestResult result = testConnection(
                configuration.getProviderCode(),
                configuration.getDefaultModelName(),
                null,
                configuration.getApiKeyEncrypted(),
                configuration.getBaseApiUrl()
        );

        configuration.setLastTestStatus(result.getStatus());
        configuration.setLastTestAt(new Date());
        configuration.setLastError(result.isSuccess() ? null : result.getMessage());
        dataManager.commit(configuration);

        if (!result.isSuccess()) {
            throw new DevelopmentException("Корпоративное AI-подключение не прошло проверку: " + result.getMessage());
        }
    }

    @Override
    public AiConnectionTestResult testConnection(String providerCode, String modelName,
                                                  String plainApiKey, String encryptedApiKey,
                                                  String baseApiUrl) {
        if (!isConfigured(providerCode)) {
            return AiConnectionTestResult.fail(
                    providerCode, modelName,
                    "Не указан провайдер AI.",
                    "<p><b>Причина:</b> Не выбран провайдер нейросети.</p>"
                            + "<p><b>Рекомендация:</b> Выберите AI-провайдера (например, OpenAI, Anthropic, GigaChat, YandexGPT) из выпадающего списка.</p>",
                    "providerCode is empty");
        }

        String apiKey = null;
        if (isConfigured(plainApiKey)) {
            apiKey = plainApiKey.trim();
        } else if (isConfigured(encryptedApiKey)) {
            try {
                apiKey = aiSecretService.decrypt(encryptedApiKey);
            } catch (Exception e) {
                return AiConnectionTestResult.fail(
                        providerCode, modelName,
                        "Ошибка расшифровки сохранённого API-ключа.",
                        "<p><b>Причина:</b> Не удалось расшифровать сохранённый в системе ключ API.</p>"
                                + "<p><b>Рекомендация:</b> Введите API-ключ заново в поле ввода ключа и сохраните настройки.</p>",
                        e.getMessage());
            }
        }

        if (!isConfigured(apiKey)) {
            return AiConnectionTestResult.fail(
                    providerCode, modelName,
                    "API-ключ не задан.",
                    "<p><b>Причина:</b> API-ключ для подключения к AI-провайдеру отсутствует.</p>"
                            + "<p><b>Рекомендация:</b> Введите действующий API-ключ в поле ввода ключа перед проверкой соединения.</p>",
                    "API key is empty");
        }

        String resolvedModel = isConfigured(modelName)
                ? modelName.trim()
                : AiProviderCatalog.getDefaultModel(providerCode);

        AIProvider provider;
        try {
            provider = aiProviderRegistry.getProvider(providerCode);
        } catch (IllegalArgumentException e) {
            return AiConnectionTestResult.fail(
                    providerCode, resolvedModel,
                    "Провайдер «" + providerCode + "» не поддерживается.",
                    "<p><b>Причина:</b> Указанный провайдер не зарегистрирован в системе или не поддерживается текущей версией HRM HuntTech.</p>"
                            + "<p><b>Рекомендация:</b> Выберите поддерживаемого провайдера из каталога.</p>",
                    e.getMessage());
        }

        long startTime = System.currentTimeMillis();
        try {
            String response = provider.generateText(
                    "Ответь одним словом: ok",
                    "Тест подключения HRM HuntTech.",
                    apiKey,
                    resolvedModel,
                    Collections.<String, Object>singletonMap("temperature", 0.0));
            long latencyMs = System.currentTimeMillis() - startTime;
            if (!isConfigured(response)) {
                return AiConnectionTestResult.fail(
                        providerCode, resolvedModel,
                        "AI-провайдер вернул пустой ответ.",
                        "<p><b>Причина:</b> Провайдер успешно принял запрос (HTTP 200), но в ответе отсутствовал текст генерации.</p>"
                                + "<p><b>Рекомендации:</b><ul>"
                                + "<li>Проверьте корректность указанной модели («" + escapeHtml(resolvedModel) + "»).</li>"
                                + "<li>Убедитесь, что модель поддерживает режим чат-генерации (Chat Completions).</li>"
                                + "</ul></p>",
                        "Empty response body");
            }
            return AiConnectionTestResult.success(providerCode, resolvedModel, latencyMs, response.trim());
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return buildConnectionFailureResult(providerCode, resolvedModel, e, latencyMs, baseApiUrl);
        }
    }

    private AiConnectionTestResult buildConnectionFailureResult(String providerCode, String modelName,
                                                                Exception e, long latencyMs,
                                                                String baseApiUrl) {
        Throwable root = e;
        StringBuilder chain = new StringBuilder();
        while (root != null) {
            if (chain.length() > 0) {
                chain.append(" -> ");
            }
            chain.append(root.getClass().getSimpleName());
            if (root.getMessage() != null && !root.getMessage().trim().isEmpty()) {
                chain.append(": ").append(root.getMessage().trim());
            }
            if (root.getCause() == null || root.getCause() == root) {
                break;
            }
            root = root.getCause();
        }
        String fullDetails = chain.toString();
        String lower = fullDetails.toLowerCase();

        String message;
        StringBuilder detailed = new StringBuilder();

        if (lower.contains("401") || lower.contains("unauthorized")
                || lower.contains("403") || lower.contains("forbidden")
                || lower.contains("invalid api key") || lower.contains("invalid_api_key")
                || lower.contains("authentication")) {
            message = "Ошибка авторизации: неверный или отозванный API-ключ (HTTP 401/403).";
            detailed.append("<p><b>Причина:</b> AI-провайдер отклонил запрос из-за ошибки авторизации (HTTP 401 Unauthorized / 403 Forbidden).</p>")
                    .append("<p><b>Возможные причины и рекомендации:</b></p><ul>")
                    .append("<li><b>Неверный ключ:</b> Проверьте, что ключ скопирован полностью, без случайных пробелов или переносов строк в начале или в конце.</li>")
                    .append("<li><b>Отозванный или просроченный ключ:</b> Проверьте статус ключа в личном кабинете провайдера («").append(escapeHtml(providerCode)).append("»). Возможно, истёк срок его действия или он был удалён.</li>")
                    .append("<li><b>Ограничения токена:</b> Убедитесь, что токен имеет права доступа к Chat Completions / Text Generation и привязан к активному проекту.</li>");
            if ("gigachat".equalsIgnoreCase(providerCode)) {
                detailed.append("<li><b>Специфика GigaChat:</b> Для Сбер GigaChat ключ должен быть в формате Authorization Key (Base64-строка <code>Client_ID:Client_Secret</code>) либо иметь префикс scope, например <code>GIGACHAT_API_PERS|токен</code> или <code>GIGACHAT_API_CORP|токен</code>.</li>");
            }
            detailed.append("</ul>");
        } else if (lower.contains("429") || lower.contains("too many requests")
                || lower.contains("insufficient_quota") || lower.contains("quota exceeded")
                || lower.contains("rate limit") || lower.contains("rate_limit")) {
            message = "Превышен лимит запросов или исчерпан баланс аккаунта (HTTP 429 Quota Exceeded).";
            detailed.append("<p><b>Причина:</b> Исчерпан лимит запросов или баланс/квота аккаунта у AI-провайдера (HTTP 429 Too Many Requests / Insufficient Quota).</p>")
                    .append("<p><b>Возможные причины и рекомендации:</b></p><ul>")
                    .append("<li><b>Нулевой баланс или исчерпана квота:</b> На счёте аккаунта у провайдера закончились средства или бесплатные токены (Insufficient Quota). Пополните баланс в консоли провайдера.</li>")
                    .append("<li><b>Превышение частоты запросов (Rate Limit):</b> Превышен лимит запросов в минуту (RPM) или токенов в минуту (TPM) по вашему тарифному плану. Подождите 1–2 минуты и повторите проверку.</li>")
                    .append("<li><b>Лимиты организации:</b> Проверьте настройки бюджетных ограничений (Usage limits) в панели управления AI-провайдера.</li>")
                    .append("</ul>");
        } else if (lower.contains("404") || lower.contains("not found")
                || lower.contains("model not found") || lower.contains("model_not_found")) {
            message = "Модель или эндпоинт API не найдены (HTTP 404 Not Found).";
            detailed.append("<p><b>Причина:</b> Запрашиваемый ресурс или модель не найдены на сервере провайдера (HTTP 404 Not Found).</p>")
                    .append("<p><b>Возможные причины и рекомендации:</b></p><ul>")
                    .append("<li><b>Неверное имя модели:</b> Проверьте написание модели («").append(escapeHtml(modelName)).append("»). Возможно, модель устарела, снята с поддержки или пишется иначе.</li>")
                    .append("<li><b>Доступность модели для аккаунта:</b> Убедитесь, что у вашего аккаунта есть доступ к данной модели.</li>");
            if (isConfigured(baseApiUrl)) {
                detailed.append("<li><b>Неверный Base API URL:</b> Проверьте базовый адрес API: <code>").append(escapeHtml(baseApiUrl)).append("</code>.</li>");
            }
            detailed.append("</ul>");
        } else if (lower.contains("400") || lower.contains("bad request")) {
            message = "Некорректный формат запроса (HTTP 400 Bad Request).";
            detailed.append("<p><b>Причина:</b> Сервер провайдера отклонил запрос как некорректный (HTTP 400 Bad Request).</p>")
                    .append("<p><b>Возможные причины и рекомендации:</b></p><ul>")
                    .append("<li><b>Несовместимость модели:</b> Модель («").append(escapeHtml(modelName)).append("») может требовать другой формат сообщений или не поддерживать переданные параметры.</li>")
                    .append("<li><b>Формат токена:</b> Проверьте корректность формата переданного ключа авторизации.</li>")
                    .append("</ul>");
        } else if (lower.contains("500") || lower.contains("502")
                || lower.contains("503") || lower.contains("504")
                || lower.contains("internal server error") || lower.contains("bad gateway")
                || lower.contains("service unavailable") || lower.contains("gateway timeout")) {
            message = "Сервис AI-провайдера временно недоступен (HTTP 5xx Server Error).";
            detailed.append("<p><b>Причина:</b> Сервер AI-провайдера вернул внутреннюю ошибку или временно перегружен (HTTP 5xx).</p>")
                    .append("<p><b>Возможные причины и рекомендации:</b></p><ul>")
                    .append("<li><b>Технические неполадки у провайдера:</b> На стороне сервиса ведутся технические работы или наблюдается высокая нагрузка.</li>")
                    .append("<li><b>Статус сервиса:</b> Проверьте статус работы API на официальной странице статуса провайдера.</li>")
                    .append("<li><b>Повторная попытка:</b> Подождите 1–2 минуты и повторите попытку подключения.</li>")
                    .append("</ul>");
        } else if (lower.contains("ssl") || lower.contains("cert")
                || lower.contains("handshake") || lower.contains("pkix")
                || lower.contains("validator")) {
            message = "Ошибка защищённого соединения SSL/TLS.";
            detailed.append("<p><b>Причина:</b> Не удалось установить защищённое HTTPS-соединение (ошибка проверки SSL-сертификата).</p>")
                    .append("<p><b>Возможные причины и рекомендации:</b></p><ul>")
                    .append("<li><b>Сертификаты Минцифры:</b> Для российских сервисов (Сбер GigaChat, Yandex) требуется установка сертификатов Russian Trusted Root CA в хранилище доверенных сертификатов JVM (cacerts).</li>")
                    .append("<li><b>Системное время:</b> Проверьте системные часы на сервере приложения — рассинхронизация времени приводит к ошибкам валидации сертификатов.</li>")
                    .append("<li><b>Корпоративный прокси/DPI:</b> Если сеть использует SSL-фильтрацию, добавьте корневой сертификат прокси в Java truststore.</li>")
                    .append("</ul>");
        } else if (lower.contains("timeout") || lower.contains("timed out")
                || lower.contains("connectexception") || lower.contains("connection refused")
                || lower.contains("unknownhostexception") || lower.contains("noroute")
                || lower.contains("unreachable")) {
            message = "Сетевая ошибка или таймаут подключения к AI-серверу.";
            detailed.append("<p><b>Причина:</b> Сервер приложения не смог установить сетевое соединение с сервером AI-провайдера (таймаут или сетевой сбой).</p>")
                    .append("<p><b>Возможные причины и рекомендации:</b></p><ul>")
                    .append("<li><b>Доступ в сеть Интернет:</b> Проверьте наличие интернет-соединения на сервере приложения.</li>")
                    .append("<li><b>DNS-разрешение:</b> Проверьте, корректно ли DNS-сервер разрешает доменные имена провайдера.</li>")
                    .append("<li><b>Геоблокировки и прокси:</b> Доступ к серверам зарубежных провайдеров (OpenAI, Anthropic Claude) может блокироваться провайдером связи либо ограничиваться со стороны сервиса. Настройте прокси/VPN при необходимости.</li>")
                    .append("<li><b>Сетевой экран:</b> Проверьте, разрешены ли исходящие подключения по порту 443.</li>")
                    .append("</ul>");
        } else {
            message = "Ошибка при подключении к AI-провайдеру.";
            detailed.append("<p><b>Причина:</b> При обращении к API произошла ошибка: <code>")
                    .append(escapeHtml(root != null ? root.getClass().getSimpleName() : "Exception"))
                    .append("</code>.</p>")
                    .append("<p><b>Рекомендации:</b></p><ul>")
                    .append("<li>Проверьте корректность введённого API-ключа.</li>")
                    .append("<li>Убедитесь, что модель («").append(escapeHtml(modelName)).append("») поддерживается и доступна.</li>")
                    .append("<li>Обратитесь к администратору с техническим текстом ошибки, приведённым ниже.</li>")
                    .append("</ul>");
        }

        detailed.append("<div style=\"margin-top: 14px; padding: 8px 12px; background: rgba(0,0,0,0.05); border-radius: 4px; font-size: 11px;\">")
                .append("<b>Технические детали:</b><br/><code>")
                .append(escapeHtml(fullDetails))
                .append("</code></div>");

        return AiConnectionTestResult.fail(providerCode, modelName, message, detailed.toString(), fullDetails);
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void validateConfiguration(AdminAiConfiguration configuration) {
        if (!Boolean.TRUE.equals(configuration.getActive())) {
            throw new DevelopmentException("Корпоративное AI-подключение отключено.");
        }
        if (!isConfigured(configuration.getProviderCode())) {
            throw new DevelopmentException("Не указан провайдер AI.");
        }
        if (!isConfigured(configuration.getApiKeyEncrypted())) {
            throw new DevelopmentException("Корпоративный API-ключ не настроен.");
        }
    }

    private void requireAdminPermission() {
        if (!security.isSpecificPermitted(MANAGE_CORPORATE_CREDENTIALS_PERMISSION)) {
            throw new DevelopmentException("Нет права управления корпоративными AI-подключениями.");
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
