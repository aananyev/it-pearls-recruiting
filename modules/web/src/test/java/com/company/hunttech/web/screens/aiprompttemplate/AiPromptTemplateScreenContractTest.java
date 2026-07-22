package com.company.hunttech.web.screens.aiprompttemplate;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * Регрессионный контракт локализации и читаемой компоновки AI-экранов.
 * Тест не запускает Vaadin UI и проверяет XML/message-контракт экранов.
 */
public class AiPromptTemplateScreenContractTest {

    @Test
    public void browseColumnsUseLocalizedCaptions() throws Exception {
        String xml = readSource(
                "modules/web/src/com/company/hunttech/web/screens/aiprompttemplate/ai-prompt-template-browse.xml");
        assertTrue(xml.contains("caption=\"msg://AiPromptTemplate.name\""));
        assertTrue(xml.contains("caption=\"msg://AiPromptTemplate.code\""));
        assertTrue(xml.contains("caption=\"msg://AiPromptTemplate.entityClass\""));
        assertTrue(xml.contains("caption=\"msg://AiPromptTemplate.active\""));
    }

    @Test
    public void russianAndEnglishMessagesContainAllPromptHeaders() throws Exception {
        String en = readSource(
                "modules/web/src/com/company/hunttech/web/screens/aiprompttemplate/messages.properties");
        String ru = readSource(
                "modules/web/src/com/company/hunttech/web/screens/aiprompttemplate/messages_ru.properties");

        assertTrue(en.contains("AiPromptTemplate.name=Name"));
        assertTrue(en.contains("AiPromptTemplate.promptText=Prompt text"));
        assertTrue(ru.contains("AiPromptTemplate.name=Название"));
        assertTrue(ru.contains("AiPromptTemplate.promptText=Текст промпта"));
    }

    @Test
    public void promptEditorKeepsActionsOutsideScrollableContent() throws Exception {
        String xml = readSource(
                "modules/web/src/com/company/hunttech/web/screens/aiprompttemplate/ai-prompt-template-edit.xml");
        assertTrue(xml.contains("captionPosition=\"TOP\""));
        assertTrue(xml.contains("id=\"contentScrollBox\""));
        assertTrue(xml.indexOf("</scrollBox>") < xml.indexOf("id=\"editActions\""));
        assertTrue(xml.contains("action=\"windowCommitAndClose\""));
        assertTrue(xml.contains("action=\"windowClose\""));
    }

    @Test
    public void configurationEditorKeepsActionsOutsideScrollableContent() throws Exception {
        String xml = readSource(
                "modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/user-ai-configuration-edit.xml");
        assertTrue(xml.contains("captionPosition=\"TOP\""));
        assertTrue(xml.contains("id=\"contentScrollBox\""));
        assertTrue(xml.indexOf("</scrollBox>") < xml.indexOf("id=\"editActions\""));
        assertTrue(xml.contains("editable=\"false\""));
    }

    @Test
    public void currentProviderActionUsesLocalizedMessages() throws Exception {
        String xml = readSource(
                "modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/user-ai-configuration-browse.xml");
        String en = readSource(
                "modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/messages.properties");
        String ru = readSource(
                "modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/messages_ru.properties");

        assertTrue(xml.contains("caption=\"msg://makeCurrentBtn.caption\""));
        assertTrue(en.contains("makeCurrentBtn.caption=Use for AI analysis"));
        assertTrue(ru.contains("makeCurrentBtn.caption=Использовать для AI-анализа"));
    }

    private static String readSource(String relativePath) throws Exception {
        String base = System.getProperty("user.dir");
        if (!new File(base, relativePath).exists()) {
            base = new File(base).getParent();
        }
        File file = new File(base, relativePath);
        if (!file.exists()) {
            file = new File("../../" + relativePath);
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
