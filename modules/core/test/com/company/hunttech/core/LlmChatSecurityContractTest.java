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

/** Static security contracts for the chat boundary; no external API or database is used. */
public class LlmChatSecurityContractTest {

    @Test
    public void userContextAllowlistExcludesSecretsAndCandidateEntities() throws IOException {
        String builder = source("modules/global/src/com/company/hunttech/service/UserAiContextBuilder.java");
        String chatService = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");

        assertTrue(builder.contains("getUser().getName()"));
        assertFalse(builder.contains("getPhone()"));
        assertFalse(builder.contains("getMobPhone()"));
        assertFalse(builder.contains("getPassword()"));
        assertFalse(builder.contains("getApiKey()"));
        assertFalse(chatService.contains("CandidateCV"));
        assertFalse(chatService.contains("JobCandidate"));
    }

    @Test
    public void historyAndStreamingKeepOwnerBoundary() throws IOException {
        String service = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String event = source("modules/global/src/com/company/hunttech/LlmChatStreamEvent.java");
        String screen = source("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");

        assertTrue(service.contains("e.id = :id and e.user.id = :userId"));
        assertTrue(service.contains("e.conversation.user.id = :userId"));
        assertTrue(service.contains("active.assertOwner(user.getId(), conversation.getId())"));
        assertTrue(service.contains("session.assertOwner(user.getId(), conversation.getId())"));
        assertTrue(event.contains("getUserId()"));
        assertFalse(event.contains("String text"));
        assertTrue(screen.contains("pollStreaming(conversationId, activeRequestId)"));
    }

    @Test
    public void administrativeOperationsHaveSeparatePermissionGates() throws IOException {
        String contract = source("modules/global/src/com/company/hunttech/service/LlmChatService.java");
        String service = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String permissions = source("modules/web/src/com/company/hunttech/web-permissions.xml");

        assertTrue(contract.contains("hunttech.ai.viewChatHistoryAdmin"));
        assertTrue(contract.contains("hunttech.ai.reconcileChatQuota"));
        assertTrue(service.contains("requireHistoryAdminPermission()"));
        assertTrue(service.contains("requireQuotaReconciliationPermission()"));
        assertTrue(permissions.contains("hunttech.ai.viewChatHistoryAdmin"));
        assertTrue(permissions.contains("hunttech.ai.reconcileChatQuota"));
    }

    private String source(String relativePath) throws IOException {
        Path project = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (project != null && !Files.exists(project.resolve("build.gradle"))) {
            project = project.getParent();
        }
        assertNotNull("Не найден корень проекта", project);
        return new String(Files.readAllBytes(project.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
