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
 * Защищает регрессию, при которой кнопка смены пароля была визуально доступна,
 * но не была связана ни с action, ни с invoke-методом legacy-экрана CUBA.
 */
public class ExtUserChangePasswordContractTest {

    @Test
    public void buttonInvokesStandardCubaPasswordDialog() throws IOException {
        String descriptor = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml");
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java");

        assertTrue(descriptor.contains(
                "class=\"com.company.hunttech.web.screens.extuser.ExtUserEditor\""));
        assertTrue(descriptor.contains("id=\"changePasswordBtn\""));
        assertTrue(descriptor.contains("invoke=\"changePassword\""));
        assertTrue(controller.contains("extends UserEditor"));
        assertTrue(controller.contains("\"sec$User.changePassword\""));
        assertTrue(controller.contains("ParamsMap.of(\"user\", user)"));
    }

    @Test
    public void fixDoesNotReplaceStandardUserEditorSaveLogic() throws IOException {
        String controller = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java");

        assertFalse(controller.contains("userManagementService.changePassword"));
        assertFalse(controller.contains("passwordEncryption"));
        assertFalse(controller.contains("dataManager.commit"));
        assertFalse(controller.contains("rolesDs"));
        assertFalse(controller.contains("substitutionsDs"));
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
