package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Contract checks for the additive chat foundation and its privacy gates. */
public class LlmChatFoundationContractTest {

    @Test
    public void chatFunctionAndMigrationsAreRegistered() throws IOException {
        String master = source("modules/core/db/changelog/db.changelog-master.xml");
        String liquibase = source("modules/core/db/changelog/260904-2-addLlmChatFoundation.xml");
        String cubaSql = source("modules/core/db/update/postgres/26/260904-2-addLlmChatFoundation.sql");

        assertTrue(master.contains("260904-2-addLlmChatFoundation.xml"));
        assertTrue(liquibase.contains("HUNTTECH_LLM_CHAT_CONVERSATION"));
        assertTrue(liquibase.contains("HUNTTECH_LLM_CHAT_MESSAGE"));
        assertTrue(liquibase.contains("CODE = 'LLM_CHAT'"));
        assertTrue(cubaSql.contains("CODE = 'LLM_CHAT'"));
        assertTrue(liquibase.contains("WHERE NOT EXISTS"));
        assertTrue(cubaSql.contains("WHERE NOT EXISTS"));
    }

    @Test
    public void fallbackConsentIsSeparateAndSafeByDefault() throws IOException {
        String profile = source("modules/global/src/com/company/hunttech/entity/UserAiProfile.java");
        String migration = source("modules/core/db/changelog/260904-1-addAdminFallbackConsent.xml");

        assertTrue(profile.contains("ADMIN_FALLBACK_CONSENT"));
        assertTrue(profile.contains("private Boolean adminFallbackConsent = false"));
        assertTrue(migration.contains("SET ADMIN_FALLBACK_CONSENT = FALSE"));
        assertFalse("Fallback migration must not alter the separate profile consent",
                migration.contains("EXTERNAL_PROCESSING_ALLOWED"));
        assertFalse("Consent fields must not contain a phone/password mapping",
                profile.contains("PHONE") || profile.contains("PASSWORD"));
        String execution = source("modules/core/src/com/company/hunttech/service/AiExecutionServiceBean.java");
        assertTrue(execution.contains("LLM_CHAT_FUNCTION_CODE"));
        assertTrue(execution.contains("getAdminFallbackConsent"));
    }

    @Test
    public void chatServiceDoesNotResolveCandidateContext() throws IOException {
        String service = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        assertTrue(service.contains("context.put(\"message\", message.trim())"));
        assertFalse(service.contains("CandidateCV"));
        assertFalse(service.contains("JobCandidate"));
        assertTrue(service.contains("user.id = :userId"));
    }

    @Test
    public void quotaUsesCalendarMonthAndProviderUsageWhenAvailable() throws IOException {
        String service = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String result = source("modules/global/src/com/company/hunttech/service/AiExecutionResult.java");
        String function = source("modules/global/src/com/company/hunttech/entity/ai/AiFunctionConfiguration.java");
        String migration = source("modules/core/db/changelog/260904-2-addLlmChatFoundation.xml")
                + source("modules/core/db/changelog/260904-3-addLlmChatQuotaTables.xml");

        assertTrue(function.contains("DEFAULT_MONTHLY_TOKEN_QUOTA"));
        assertTrue(service.contains("Calendar.DAY_OF_MONTH"));
        assertTrue(service.contains("UNKNOWN_PENDING"));
        assertTrue(service.contains("result.getTotalTokens()"));
        assertTrue(result.contains("getTotalTokens()"));
        assertTrue(migration.contains("EFFECTIVE_TO date"));
        assertTrue(migration.contains("REASON varchar(2000)"));
        assertTrue(migration.contains("UK_HUNTTECH_LLM_CHAT_QUOTA_PERIOD"));
    }

    @Test
    public void floatingChatShellIsRegisteredAndHasNoDeleteAction() throws IOException {
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml");
        String controller = source("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");
        String menu = source("modules/web/src/com/company/hunttech/web-menu.xml");

        assertTrue(descriptor.contains("dialogMode width=\"420\" height=\"640\""));
        assertTrue(controller.contains("llmChatService.startStreaming"));
        assertTrue(controller.contains("pollStreaming"));
        assertTrue(descriptor.contains("streamPollTimer"));
        assertFalse("Пользователю нельзя удалять бессрочную историю", controller.contains("delete"));
        assertTrue(menu.contains("screen=\"hunttech_LlmChatScreen\""));
    }

    @Test
    public void adminHistoryIsPermissionGatedAndUserHistoryRemainsScoped() throws IOException {
        String contract = source("modules/global/src/com/company/hunttech/service/LlmChatService.java")
                + source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String permissions = source("modules/web/src/com/company/hunttech/web-permissions.xml");
        assertTrue(contract.contains("viewChatHistoryAdmin"));
        assertTrue(contract.contains("security.isSpecificPermitted"));
        assertTrue(contract.contains("loadHistoryAsAdmin"));
        assertTrue(contract.contains("e.conversation.user.id = :userId"));
        assertTrue(permissions.contains("hunttech.ai.viewChatHistoryAdmin"));
    }

    @Test
    public void personalCredentialsHaveCiphertextPathAndNoSecretInPickerView() throws IOException {
        String entity = source("modules/global/src/com/company/hunttech/entity/UserAiConfiguration.java");
        String views = source("modules/global/src/com/company/hunttech/ai-control-plane-views.xml");
        String migration = source("modules/core/db/changelog/260904-4-addUserAiEncryptedKey.xml")
                + source("modules/core/db/update/postgres/26/260904-4-addUserAiEncryptedKey.sql");
        String editor = source("modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/UserAiConfigurationEdit.java");
        assertTrue(entity.contains("API_KEY_ENCRYPTED"));
        assertTrue(views.contains("<property name=\"apiKeyEncrypted\"/>"));
        assertTrue(migration.contains("ADD COLUMN IF NOT EXISTS API_KEY_ENCRYPTED"));
        assertTrue(editor.contains("encryptUserSecret"));
        assertTrue(editor.contains("setApiKey(null)"));
    }

    @Test
    public void mainScreenHasPersistentChatLauncher() throws IOException {
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/ext-main-screen.xml");
        String controller = source("modules/web/src/com/company/hunttech/web/screens/mainscreen/ExtMainScreen.java");
        String styles = source("modules/web/themes/hunttech-modern-light/com.company.hunttech/chat-style.css");
        assertTrue(descriptor.contains("id=\"llmChatLauncher\""));
        assertTrue(descriptor.contains("invoke=\"openLlmChat\""));
        assertTrue(controller.contains("screens.create(LlmChatScreen.class).show()"));
        assertTrue(styles.contains(".llm-chat-launcher-bar"));
    }

    @Test
    public void chatRetriesAreIdempotentByRequestId() throws IOException {
        String service = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String api = source("modules/global/src/com/company/hunttech/service/LlmChatService.java");
        String message = source("modules/global/src/com/company/hunttech/entity/ai/LlmChatMessage.java");
        String migration = source("modules/core/db/changelog/260904-5-addLlmChatRequestId.xml")
                + source("modules/core/db/update/postgres/26/260904-5-addLlmChatRequestId.sql");
        assertTrue(api.contains("sendMessage(UUID conversationId, String message, String requestId)"));
        assertTrue(service.contains("resolveExistingRequest"));
        assertTrue(service.contains("UNKNOWN_PENDING"));
        assertTrue(message.contains("REQUEST_ID"));
        assertTrue(migration.contains("IDX_HUNTTECH_LLM_CHAT_MESSAGE_REQUEST"));
    }

    @Test
    public void chatSupportsCooperativeCancellationWithoutDuplicateProviderCall() throws IOException {
        String api = source("modules/global/src/com/company/hunttech/service/LlmChatService.java");
        String service = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String screen = source("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");
        assertTrue(api.contains("cancelMessage(UUID conversationId, String requestId)"));
        assertTrue(service.contains("CANCEL_REQUESTED"));
        assertTrue(service.contains("CANCELLED"));
        assertTrue(service.contains("новый requestId"));
        assertTrue(screen.contains("cancelBtn"));
        assertTrue(screen.contains("cancelMessage"));
        assertTrue(service.contains("CANCEL_REQUESTED"));
    }

    @Test
    public void unknownUsageHasPermissionGatedAdminReconciliation() throws IOException {
        String api = source("modules/global/src/com/company/hunttech/service/LlmChatService.java");
        String service = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String reservation = source("modules/global/src/com/company/hunttech/entity/ai/LlmChatQuotaReservation.java");
        String permissions = source("modules/web/src/com/company/hunttech/web-permissions.xml");
        String views = source("modules/global/src/com/company/hunttech/ai-control-plane-views.xml");
        String migration = source("modules/core/db/changelog/260904-6-addLlmChatReconciliationAudit.xml")
                + source("modules/core/db/update/postgres/26/260904-6-addLlmChatReconciliationAudit.sql");

        assertTrue(api.contains("reconcileUnknown(String requestId, Integer actualTokens, boolean providerCharged)"));
        assertTrue(api.contains("RECONCILE_CHAT_QUOTA_PERMISSION"));
        assertTrue(service.contains("UNKNOWN_PENDING"));
        assertTrue(service.contains("RECONCILE_CHAT_QUOTA_PERMISSION"));
        assertTrue(service.contains("setReconciledBy"));
        assertTrue(service.contains("providerCharged"));
        assertTrue(reservation.contains("PROVIDER_REQUEST_ID"));
        assertTrue(reservation.contains("RECONCILED_AT"));
        assertTrue(permissions.contains("hunttech.ai.reconcileChatQuota"));
        assertTrue(views.contains("providerRequestId"));
        assertTrue(migration.contains("PROVIDER_REQUEST_ID"));
        assertTrue(migration.contains("RECONCILED_BY"));
        assertTrue(migration.contains("RECONCILED_AT"));
    }

    @Test
    public void adminReconciliationScreenExposesOnlyPendingReservations() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/llmchatquota/LlmChatQuotaReconciliationBrowse.java");
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/llmchatquota/llm-chat-quota-reconciliation-browse.xml");
        String menu = source("modules/web/src/com/company/hunttech/web-menu.xml");

        assertTrue(controller.contains("LlmChatService"));
        assertTrue(controller.contains("reconcileUnknown"));
        assertTrue(controller.contains("Dialogs"));
        assertTrue(controller.contains("isSpecificPermitted"));
        assertTrue(controller.contains("RECONCILE_CHAT_QUOTA_PERMISSION"));
        assertTrue(descriptor.contains("UNKNOWN_PENDING"));
        assertTrue(descriptor.contains("providerRequestId"));
        assertTrue(descriptor.contains("settledBtn"));
        assertTrue(descriptor.contains("releasedBtn"));
        assertFalse("Экран сверки не должен удалять reservation или историю",
                descriptor.contains("type=\"remove\"") || descriptor.contains("action=\"remove\""));
        assertTrue(menu.contains("hunttech_LlmChatQuotaReconciliation.browse"));
    }

    @Test
    public void providerAdapterSupportsRequestIdStreamingAndHardCancellation() throws IOException {
        String provider = source("modules/core/src/com/company/hunttech/core/ai/AIProvider.java");
        String registry = source("modules/core/src/com/company/hunttech/core/ai/AIProviderRegistry.java");
        String adapter = source("modules/core/src/com/company/hunttech/core/ai/AbstractOpenAiCompatibleProvider.java");
        String response = source("modules/core/src/com/company/hunttech/core/ai/AiProviderResponse.java");
        String result = source("modules/global/src/com/company/hunttech/service/AiExecutionResult.java");
        String execution = source("modules/core/src/com/company/hunttech/service/AiExecutionServiceBean.java");
        String chat = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String message = source("modules/global/src/com/company/hunttech/entity/ai/LlmChatMessage.java");

        assertTrue(provider.contains("supportsStreaming"));
        assertTrue(provider.contains("cancelRequest(String requestId)"));
        assertTrue(registry.contains("cancelRequest"));
        assertTrue(registry.contains("registerRequest"));
        assertTrue(registry.contains("unregisterRequest"));
        assertTrue(adapter.contains("text/event-stream"));
        assertTrue(adapter.contains("PROVIDER_REQUEST_ID") || adapter.contains("providerRequestId"));
        assertTrue(adapter.contains("disconnect()"));
        assertTrue(response.contains("getProviderRequestId"));
        assertTrue(result.contains("getProviderRequestId"));
        assertTrue(execution.contains("response.getProviderRequestId()"));
        assertTrue(chat.contains("context.put(\"requestId\""));
        assertTrue(chat.contains("aiProviderRegistry.cancelRequest"));
        assertTrue(chat.contains("reservation.setProviderRequestId"));
        assertTrue(message.contains("setProviderRequestId"));
    }

    @Test
    public void chatStreamingUsesOwnerScopedPollingFacade() throws IOException {
        String api = source("modules/global/src/com/company/hunttech/service/LlmChatService.java");
        String state = source("modules/global/src/com/company/hunttech/service/LlmChatStreamState.java");
        String service = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String executionApi = source("modules/global/src/com/company/hunttech/service/AiExecutionService.java");
        String execution = source("modules/core/src/com/company/hunttech/service/AiExecutionServiceBean.java");
        String screen = source("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml");

        assertTrue(api.contains("startStreaming(UUID conversationId, String message, String requestId)"));
        assertTrue(api.contains("pollStreaming(UUID conversationId, String requestId)"));
        assertTrue(state.contains("isCompleted()"));
        assertTrue(state.contains("getText()"));
        assertTrue(service.contains("TaskScheduler"));
        assertTrue(service.contains("SecurityContextAwareRunnable"));
        assertTrue(service.contains("resolveConversation(conversationId, user)"));
        assertTrue(executionApi.contains("executeTextStreaming"));
        assertTrue(execution.contains("supportsStreaming"));
        assertTrue(screen.contains("streamPollTimer"));
        assertTrue(screen.contains("pollStreaming"));
        assertTrue(descriptor.contains("id=\"streamPollTimer\""));
    }

    private String source(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) root = root.getParent();
        assertNotNull("Не найден корень проекта", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
