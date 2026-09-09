package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает контракт функционала загрузки фотографий профиля из Telegram
 * в экранных формах PersonEdit, JobCandidateEdit и ExtUserEdit.
 */
public class TelegramPhotoLoaderScreensContractTest {

    private static final String SCREENS = "modules/web/src/com/company/hunttech/web/screens/";

    @Test
    public void testPersonEditTelegramPhotoContract() throws IOException {
        String xml = readProjectFile(SCREENS + "person/person-edit.xml");
        String java = readProjectFile(SCREENS + "person/PersonEdit.java");

        assertTrue("В person-edit.xml должна присутствовать кнопка loadTelegramPhotoButton",
                xml.contains("id=\"loadTelegramPhotoButton\""));
        assertTrue("В person-edit.xml должен быть аватар personPic с компонентом ovaFallbackImage",
                xml.contains("<ovaFallbackImage id=\"personPic\""));
        assertTrue("В PersonEdit.java должен быть обработчик onLoadTelegramPhotoButtonClick",
                java.contains("onLoadTelegramPhotoButtonClick"));
        assertTrue("В PersonEdit.java должно обновляться изображение personPic",
                java.contains("personPic.setSource("));
    }

    @Test
    public void testJobCandidateEditTelegramPhotoContract() throws IOException {
        String xml = readProjectFile(SCREENS + "jobcandidate/job-candidate-edit.xml");
        String java = readProjectFile(SCREENS + "jobcandidate/JobCandidateEdit.java");
        String messagesRu = readProjectFile(SCREENS + "jobcandidate/messages_ru.properties");
        String messagesEn = readProjectFile(SCREENS + "jobcandidate/messages.properties");

        assertTrue("В job-candidate-edit.xml должна присутствовать кнопка loadTelegramPhotoButton",
                xml.contains("id=\"loadTelegramPhotoButton\""));
        assertTrue("Кнопка loadTelegramPhotoButton должна ссылаться на msgLoadTelegramPhoto",
                xml.contains("caption=\"msg://msgLoadTelegramPhoto\""));
        assertTrue("В job-candidate-edit.xml должен быть аватар candidatePic с компонентом ovaFallbackImage",
                xml.contains("<ovaFallbackImage id=\"candidatePic\""));
        assertTrue("В JobCandidateEdit.java должен быть обработчик onLoadTelegramPhotoButtonClick",
                java.contains("onLoadTelegramPhotoButtonClick"));
        assertTrue("В JobCandidateEdit.java должно обновляться изображение candidatePic",
                java.contains("candidatePic.setSource("));
        assertTrue("В JobCandidateEdit.java должна обновляться доступность кнопки",
                java.contains("updateLoadTelegramButtonState"));

        assertTrue("В messages_ru.properties должен быть ключ msgLoadTelegramPhoto",
                messagesRu.contains("msgLoadTelegramPhoto="));
        assertTrue("В messages.properties должен быть ключ msgLoadTelegramPhoto",
                messagesEn.contains("msgLoadTelegramPhoto="));
    }

    @Test
    public void testExtUserEditTelegramPhotoContract() throws IOException {
        String xml = readProjectFile(SCREENS + "extuser/ext-user-edit.xml");
        String java = readProjectFile(SCREENS + "extuser/ExtUserEditor.java");
        String messagesRu = readProjectFile(SCREENS + "extuser/messages_ru.properties");
        String messagesEn = readProjectFile(SCREENS + "extuser/messages.properties");

        assertTrue("В ext-user-edit.xml должна присутствовать кнопка loadTelegramPhotoButton",
                xml.contains("id=\"loadTelegramPhotoButton\""));
        assertTrue("В ext-user-edit.xml должен быть аватар userPic с компонентом ovaFallbackImage",
                xml.contains("<ovaFallbackImage id=\"userPic\""));
        assertTrue("В ExtUserEditor.java должен быть заинжектирован userPic",
                java.contains("private OvaFallbackImage userPic;"));
        assertTrue("В ExtUserEditor.java должно обновляться изображение userPic",
                java.contains("userPic.setSource("));
        assertTrue("В ExtUserEditor.java должно присутствовать динамическое обновление состояния кнопки",
                java.contains("updateLoadTelegramButtonState"));

        assertTrue("В messages_ru.properties должен быть ключ msgLoadTelegramPhoto",
                messagesRu.contains("msgLoadTelegramPhoto="));
        assertTrue("В messages.properties должен быть ключ msgLoadTelegramPhoto",
                messagesEn.contains("msgLoadTelegramPhoto="));
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
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
