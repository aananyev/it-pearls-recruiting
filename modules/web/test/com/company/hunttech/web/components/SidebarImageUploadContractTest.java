package com.company.hunttech.web.components;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Контракт общего безопасного pipeline пользовательских sidebar/profile изображений. */
public class SidebarImageUploadContractTest {

    private static final String ACCEPT = "accept=\"image/png,image/jpeg,image/gif,image/bmp,image/vnd.wap.wbmp,image/webp,image/tiff\"";
    private static final String EXTENSIONS = "permittedExtensions=\".png,.jpg,.jpeg,.gif,.bmp,.wbmp,.webp,.tif,.tiff\"";
    private static final String LIMIT = "fileSizeLimit=\"20971520\"";

    @Test
    void componentUsesSharedNormalizerAndNeverFallsBackForImageBindings() throws Exception {
        String java = read("modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java");
        String spring = read("modules/web/src/com/company/hunttech/web-spring.xml");

        assertTrue(spring.contains("hunttech_SidebarImageNormalizationService"));
        assertTrue(spring.contains("com.company.hunttech.app.SidebarImageNormalizationService"));
        assertTrue(java.contains("SidebarImageNormalizationService.NAME"));
        assertTrue(java.contains("service.normalize(originalBytes, fileDescriptor.getName())"));
        assertTrue(java.contains("rejectImageUpload(fileDescriptor"));
        assertTrue(java.contains("fileUploading.deleteFile(getFileId())"));
        assertTrue(java.contains("private boolean uploadRejected;"));
        assertTrue(java.contains("if (uploadRejected)"));
        assertTrue(java.contains("return getValue();"),
                "После reject preview/listener должен получить прежнее bound-значение");
        assertTrue(java.contains("protected void fireFileUploadSucceed(String fileName, long contentLength)"));
        assertTrue(java.contains("if (uploadRejected && isSidebarImageBinding())"),
                "Rejected upload не должен запускать downstream success handlers");
        assertTrue(java.contains("fileName = currentValue == null ? null : currentValue.getName();"));
        assertTrue(java.contains("getMetaProperty()"),
                "DatasourceComponent API должен поддерживать и container, и legacy datasource bindings");
        assertFalse(java.contains("instanceof ContainerValueSource"));
        assertTrue(java.contains("POSITION_ICON_PROPERTY"));
        assertTrue(java.contains("SKILL_LOGO_PROPERTY"));
        assertTrue(java.contains("COUNTRY_FLAG_PROPERTY"));
        assertTrue(java.contains("SOCIAL_NETWORK_LOGO_PROPERTY"));
        assertTrue(java.contains("OFFICIAL_PHOTO_PROPERTY"));
        assertTrue(java.contains("USER_AVATAR_PROPERTY"));
        assertTrue(java.contains("APPLICATION_LOGO_PROPERTY"));
        assertTrue(java.contains("APPLICATION_ICON_PROPERTY"));
        assertFalse(java.contains("ProjectLogoImageProcessingService"));
        assertFalse(java.contains("При любой ошибке обработки сохраняем исходный файл"));
        assertTrue(java.contains("SidebarImageNormalizationService.MAX_PIXELS"),
                "Ошибка upload должна объяснять лимит пикселей вместе с лимитом размера файла");
        assertTrue(java.contains("SidebarImageNormalizationService.MAX_INPUT_BYTES"));
    }

    @Test
    void everySidebarOrProfileImageUploadHasRuntimeSupportedFilters() throws Exception {
        Map<String, Integer> descriptors = new LinkedHashMap<>();
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/project/project-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/city/city-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/region/region-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/candidatecv/candidate-cv-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/person/person-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/position/position-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/skilltree/skill-tree-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/country/country-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/socialnetworktype/social-network-type-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml", 1);
        descriptors.put("modules/web/src/com/company/hunttech/web/screens/applicationsetup/application-setup-edit.xml", 2);

        assertEquals(15, descriptors.values().stream().mapToInt(Integer::intValue).sum());
        for (Map.Entry<String, Integer> descriptor : descriptors.entrySet()) {
            String xml = read(descriptor.getKey());
            assertEquals(descriptor.getValue().intValue(), occurrences(xml, ACCEPT),
                    descriptor.getKey() + " должен ограничивать каждый graphics upload MIME-фильтром");
            assertEquals(descriptor.getValue().intValue(), occurrences(xml, EXTENSIONS),
                    descriptor.getKey() + " должен ограничивать каждый graphics upload расширениями");
            assertEquals(descriptor.getValue().intValue(), occurrences(xml, LIMIT),
                    descriptor.getKey() + " должен использовать общий лимит 20 МБ");
            assertTrue(xml.contains("image/webp"), descriptor + " должен принимать WebP через pinned runtime codec");
            assertTrue(xml.contains("image/tiff"), descriptor + " должен принимать TIFF через pinned runtime codec");
            assertFalse(xml.contains("image/svg"), descriptor + " не должен принимать документы/SVG");
        }
    }

    @Test
    void specializedAvatarAndAdminBlobAdaptersReuseNormalizedPng() throws Exception {
        String settings = read("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java");
        assertFalse(settings.contains("AvatarImageUploadHelper"));
        assertFalse(settings.contains("processUploadedAvatar("));
        assertTrue(settings.contains("userAvatarManagementService.applyUserPersonalAvatar(user, uploaded)"));

        String adminXml = read("modules/web/src/com/company/hunttech/web/screens/adminaiconfiguration/admin-ai-configuration-edit.xml");
        assertEquals(1, occurrences(adminXml, ACCEPT));
        assertEquals(1, occurrences(adminXml, EXTENSIONS));
        assertEquals(1, occurrences(adminXml, LIMIT));
        assertFalse(adminXml.contains("image/svg"));

        String adminJava = read("modules/web/src/com/company/hunttech/web/screens/adminaiconfiguration/AdminAiConfigurationEdit.java");
        assertTrue(adminJava.contains("SidebarImageNormalizationService sidebarImageNormalizationService"));
        assertTrue(adminJava.contains("sidebarImageNormalizationService.normalize(bytes, event.getFileName())"));
        assertTrue(adminJava.contains("getEditedEntity().setLogoImage(processed.getData())"));
        assertFalse(adminJava.contains("getEditedEntity().setLogoImage(bytes)"));
        assertTrue(adminJava.contains("SidebarImageNormalizationService.MAX_PIXELS"));
    }

    @Test
    void rejectedImageUploadsKeepExistingFallbackPreviews() throws Exception {
        String applicationSetup = read("modules/web/src/com/company/hunttech/web/screens/applicationsetup/ApplicationSetupEdit.java");
        assertTrue(applicationSetup.contains("FileDescriptor descriptor = applicationLogoField.getFileDescriptor();\n        // Rejected image uploads"));
        assertTrue(applicationSetup.contains("FileDescriptor descriptor = applicationIconField.getFileDescriptor();\n        // Rejected image uploads"));

        String socialNetwork = read("modules/web/src/com/company/hunttech/web/screens/socialnetworktype/SocialNetworkTypeEdit.java");
        assertTrue(socialNetwork.contains("FileDescriptor descriptor = snLogoFileUpload.getFileDescriptor();\n        if (descriptor == null)"));

        String person = read("modules/web/src/com/company/hunttech/web/screens/person/PersonEdit.java");
        assertTrue(person.contains("FileDescriptor descriptor = fileImageFaceUpload.getFileDescriptor();\n        if (descriptor == null)"));
    }

    private String read(String relativePath) throws Exception {
        File file = new File(relativePath);
        if (!file.exists()) file = new File("../" + relativePath);
        if (!file.exists()) file = new File("../../" + relativePath);
        assertTrue(file.exists(), relativePath + " должен существовать");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private int occurrences(String text, String needle) {
        int count = 0;
        for (int from = 0; (from = text.indexOf(needle, from)) >= 0; from += needle.length()) count++;
        return count;
    }
}
