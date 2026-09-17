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

/**
 * Защищает fail-closed контракт вкладок LLM-chat до завершения привязки
 * production-ролей и write-Hermes к security context CUBA Platform.
 */
public class LlmChatManagerAccessContractTest {

    @Test
    public void privilegedTabsAreHiddenByDefaultAndPermissionsAreRegistered() throws IOException {
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml");
        String permissions = source("modules/web/src/com/company/hunttech/web-permissions.xml");

        assertTrue(descriptor.contains("id=\"localChatTab\""));
        assertTrue(descriptor.contains("id=\"hermesManagerChatTab\""));
        assertTrue(descriptor.contains("id=\"localChatTab\" caption=\"msg://tabLocalChat\" margin=\"true\" spacing=\"true\" visible=\"false\""));
        assertTrue(descriptor.contains("id=\"hermesManagerChatTab\" caption=\"msg://tabHermesManager\" margin=\"true\" spacing=\"true\" visible=\"false\""));
        assertTrue(permissions.contains("hunttech.ai.useLocalChat"));
        assertTrue(permissions.contains("hunttech.ai.useManagerHermesWrite"));
    }

    @Test
    public void existingHermesRemainsAvailableAndManagerWriteControlsStayDisabled() throws IOException {
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml");

        assertTrue(descriptor.contains("id=\"hermesChatTab\""));
        assertFalse(descriptor.contains("id=\"hermesChatTab\" caption=\"msg://tabHermes\" margin=\"true\" spacing=\"true\" visible=\"false\""));
        assertTrue(descriptor.contains("id=\"hermesManagerInputArea\""));
        assertTrue(descriptor.contains("id=\"hermesManagerSendBtn\""));
        assertTrue(descriptor.contains("id=\"hermesManagerInputArea\" width=\"100%\" rows=\"2\" stylename=\"llm-chat-input-area\" enabled=\"false\""));
        assertTrue(descriptor.contains("id=\"hermesManagerSendBtn\" stylename=\"llm-chat-send-btn primary\" captionAsHtml=\"true\" align=\"MIDDLE_RIGHT\" enabled=\"false\""));
    }

    @Test
    public void descriptorDoesNotHardcodeBusinessRoleNamesAsSecurityBoundary() throws IOException {
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml");
        String permissions = source("modules/web/src/com/company/hunttech/web-permissions.xml");

        assertFalse(descriptor.contains("role=\"Менеджер\""));
        assertFalse(descriptor.contains("role=\"Директор\""));
        assertFalse(permissions.contains("Менеджер"));
        assertFalse(permissions.contains("Директор"));
    }

    private String source(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }
}
