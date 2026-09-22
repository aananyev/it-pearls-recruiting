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

/** Регрессия BL-2026-021: новая персональная AI-конфигурация не сохраняется без владельца. */
public class UserAiConfigurationOwnerContractTest {

    @Test
    public void everyUiCreationPathAssignsOwnerBeforeCommit() throws IOException {
        String extUserEdit = source("modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEdit.java");
        String settings = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java");
        String browse = source("modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/UserAiConfigurationBrowse.java");
        String browseXml = source("modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/user-ai-configuration-browse.xml");

        // Три доступных UI-пути обязаны явно передать бизнес-владельца новой записи.
        assertTrue(extUserEdit.contains("entity.setUser(user)"));
        assertTrue(settings.contains("entity.setUser(currentUser)"));
        assertTrue(browse.contains("configuration.setUser(userSessionSource.getUserSession().getUser())"));
        assertTrue(browseXml.contains("invoke=\"onCreateBtnClick\""));
        assertFalse("Generic create-action снова допускает запись без владельца",
                browseXml.contains("<action id=\"create\" type=\"create\"/>"));
    }

    @Test
    public void editorBlocksOwnerlessCommitAndDatabaseConstraintRemains() throws IOException {
        String editor = source("modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/UserAiConfigurationEdit.java");
        String entity = source("modules/global/src/com/company/hunttech/entity/UserAiConfiguration.java");

        assertTrue(editor.contains("getEditedEntity().getUser() == null"));
        assertTrue(editor.contains("event.preventCommit()"));
        assertTrue(editor.contains("Не удалось определить владельца AI-конфигурации"));
        assertFalse("createdBy не является бизнес-основанием для FK", editor.contains("getCreatedBy()"));
        assertTrue(entity.contains("@ManyToOne(fetch = FetchType.LAZY, optional = false)"));
        assertTrue(entity.contains("@NotNull"));
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
