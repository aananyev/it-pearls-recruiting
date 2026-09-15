package com.company.hunttech.web.screens.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контрактный автотест экрана диалога выбора фотографий Telegram (TelegramPhotoSelectDialog):
 * 1. Проверка структуры XML-дескриптора (модальный режим 640x480, scrollBox, flowBox, кнопка отмены).
 * 2. Проверка Java-контроллера (аннотации @UiController, @UiDescriptor, геттеры/сеттеры, FileDescriptorImageHelper).
 * 3. Проверка регистрации в web-screens.xml.
 * 4. Проверка бандлов локализации (ru/en).
 * 5. Проверка интеграции в ExtUserEditor (вызов saveUserProfilePhotosToFileStorage, лимит и очистка невыбранных фото).
 */
public class TelegramPhotoSelectDialogContractTest {

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        if (path.startsWith("modules/web/")) {
            File sub = new File(path.substring("modules/web/".length()));
            if (sub.exists()) {
                return sub;
            }
        }
        return file;
    }

    @Test
    @DisplayName("Проверка структуры XML-дескриптора telegram-photo-select-dialog.xml")
    void testXmlDescriptorIntegrity() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/telegram/telegram-photo-select-dialog.xml");
        assertTrue(xmlFile.exists(), "XML-дескриптор telegram-photo-select-dialog.xml обязан существовать");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        Element window = doc.getDocumentElement();
        assertEquals("window", window.getTagName());
        assertEquals("msg://telegramPhotoSelectDialog.caption", window.getAttribute("caption"));
        assertEquals("com.company.hunttech.web.screens.telegram", window.getAttribute("messagesPack"));

        NodeList dialogModeList = doc.getElementsByTagName("dialogMode");
        assertTrue(dialogModeList.getLength() > 0, "Тег dialogMode должен присутствовать");
        Element dialogMode = (Element) dialogModeList.item(0);
        assertEquals("640px", dialogMode.getAttribute("width"));
        assertEquals("true", dialogMode.getAttribute("forceDialog"));
        assertEquals("false", dialogMode.getAttribute("closeOnClickOutside"));

        NodeList flowBoxes = doc.getElementsByTagName("flowBox");
        Element photosFlowBox = null;
        for (int i = 0; i < flowBoxes.getLength(); i++) {
            Element el = (Element) flowBoxes.item(i);
            if ("photosFlowBox".equals(el.getAttribute("id"))) {
                photosFlowBox = el;
                break;
            }
        }
        assertNotNull(photosFlowBox, "Контейнер photosFlowBox обязан присутствовать в XML");

        NodeList buttons = doc.getElementsByTagName("button");
        boolean cancelFound = false;
        for (int i = 0; i < buttons.getLength(); i++) {
            Element btn = (Element) buttons.item(i);
            if ("cancelButton".equals(btn.getAttribute("id"))) {
                cancelFound = true;
                break;
            }
        }
        assertTrue(cancelFound, "Кнопка cancelButton обязана присутствовать в тулбаре действий");
    }

    @Test
    @DisplayName("Проверка Java-контроллера TelegramPhotoSelectDialog")
    void testControllerIntegrity() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/telegram/TelegramPhotoSelectDialog.java");
        assertTrue(javaFile.exists(), "Java-контроллер TelegramPhotoSelectDialog.java обязан существовать");

        String content = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("@UiController(\"hunttech_TelegramPhotoSelectDialog\")"),
                "Контроллер должен объявлять screen id hunttech_TelegramPhotoSelectDialog");
        assertTrue(content.contains("@UiDescriptor(\"telegram-photo-select-dialog.xml\")"),
                "Контроллер должен ссылаться на дескриптор telegram-photo-select-dialog.xml");
        assertTrue(content.contains("public void setPhotos(List<FileDescriptor> photos)"),
                "Контроллер обязан предоставлять setter списка фото");
        assertTrue(content.contains("public FileDescriptor getSelectedPhoto()"),
                "Контроллер обязан предоставлять getter выбранного фото");
        assertTrue(content.contains("close(StandardOutcome.SELECT);"),
                "Контроллер обязан закрывать диалог с StandardOutcome.SELECT при щелчке мыши");
        assertTrue(content.contains("FileDescriptorImageHelper.setImageSource"),
                "Контроллер обязан безопасно валидировать и загружать ресурс через FileDescriptorImageHelper");
        assertTrue(content.contains("addLayoutClickListener"),
                "Вся карточка должна быть кликабельной для выбора фотографии");
    }

    @Test
    @DisplayName("Проверка регистрации экрана в web-screens.xml")
    void testScreenRegistration() throws Exception {
        File webScreensFile = resolveFile("modules/web/src/com/company/hunttech/web-screens.xml");
        assertTrue(webScreensFile.exists(), "web-screens.xml обязан существовать");

        String content = new String(Files.readAllBytes(webScreensFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("id=\"hunttech_TelegramPhotoSelectDialog\""),
                "web-screens.xml обязан регистрировать экран hunttech_TelegramPhotoSelectDialog");
        assertTrue(content.contains("template=\"com/company/hunttech/web/screens/telegram/telegram-photo-select-dialog.xml\""),
                "web-screens.xml обязан указывать правильный путь к шаблону");
    }

    @Test
    @DisplayName("Проверка локализации экрана диалога (ru и en)")
    void testLocalizationIntegrity() throws Exception {
        File ruProps = resolveFile("modules/web/src/com/company/hunttech/web/screens/telegram/messages_ru.properties");
        File enProps = resolveFile("modules/web/src/com/company/hunttech/web/screens/telegram/messages.properties");

        assertTrue(ruProps.exists(), "messages_ru.properties обязан существовать");
        assertTrue(enProps.exists(), "messages.properties обязан существовать");

        String ru = new String(Files.readAllBytes(ruProps.toPath()), StandardCharsets.UTF_8);
        String en = new String(Files.readAllBytes(enProps.toPath()), StandardCharsets.UTF_8);

        assertTrue(ru.contains("telegramPhotoSelectDialog.caption="), "ru локализация обязана содержать telegramPhotoSelectDialog.caption");
        assertTrue(ru.contains("telegramPhotoSelectDialog.hint="), "ru локализация обязана содержать telegramPhotoSelectDialog.hint");
        assertTrue(ru.contains("msgNoPhotos="), "ru локализация обязана содержать msgNoPhotos");
        assertTrue(ru.contains("msgCancel="), "ru локализация обязана содержать msgCancel");

        assertTrue(en.contains("telegramPhotoSelectDialog.caption="), "en локализация обязана содержать telegramPhotoSelectDialog.caption");
        assertTrue(en.contains("telegramPhotoSelectDialog.hint="), "en локализация обязана содержать telegramPhotoSelectDialog.hint");
        assertTrue(en.contains("msgNoPhotos="), "en локализация обязана содержать msgNoPhotos");
        assertTrue(en.contains("msgCancel="), "en локализация обязана содержать msgCancel");
    }

    @Test
    @DisplayName("Проверка интеграции диалога выбора в ExtUserEditor")
    void testExtUserEditorIntegration() throws Exception {
        File editorFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java");
        assertTrue(editorFile.exists(), "ExtUserEditor.java обязан существовать");

        String content = new String(Files.readAllBytes(editorFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("saveUserProfilePhotosToFileStorage"),
                "ExtUserEditor обязан использовать метод saveUserProfilePhotosToFileStorage");
        assertTrue(content.contains("TELEGRAM_PHOTOS_LIMIT"),
                "ExtUserEditor обязан использовать именованную константу TELEGRAM_PHOTOS_LIMIT");
        assertTrue(content.contains("TelegramPhotoSelectDialog"),
                "ExtUserEditor обязан ссылаться на класс TelegramPhotoSelectDialog");
        assertTrue(content.contains("screenBuilders.screen(this)"),
                "ExtUserEditor обязан использовать ScreenBuilders для модального открытия диалога");
        assertTrue(content.contains("dialog.setPhotos(photos)"),
                "ExtUserEditor обязан передавать список фото в диалог");
        assertTrue(content.contains("applyLoadedTelegramPhoto"),
                "ExtUserEditor обязан инкапсулировать применение фото в applyLoadedTelegramPhoto");
        assertTrue(content.contains("cleanupUnusedTelegramPhotos"),
                "ExtUserEditor обязан очищать неиспользованные фото через cleanupUnusedTelegramPhotos");
    }
}
