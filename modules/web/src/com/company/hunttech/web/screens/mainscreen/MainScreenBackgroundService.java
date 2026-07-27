package com.company.hunttech.web.screens.mainscreen;

import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.ThemeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Выбирает фоновое изображение главного экрана для текущего пользователя.
 * Пользовательский файл имеет абсолютный приоритет; при его отсутствии сервис
 * возвращает один из десяти растровых ресурсов активной темы.
 */
@Component(MainScreenBackgroundService.NAME)
public class MainScreenBackgroundService {

    public static final String NAME = "hunttech_MainScreenBackgroundService";
    public static final String CUSTOM_BACKGROUND_PREFIX = "hrm-main-background-";
    public static final int VARIANT_COUNT = 10;

    private static final Logger log = LoggerFactory.getLogger(MainScreenBackgroundService.class);
    private static final String QUERY_USER_SETTINGS =
            "select e from hunttech_UserSettings e where e.user = :currentUser";
    private static final String DEFAULT_THEME = "hover";
    private static final String LAST_VARIANT_ATTRIBUTE = "hrm.main.background.lastVariant.";

    private static final Set<String> SUPPORTED_THEMES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "halo",
                    "havana",
                    "helium",
                    "hover",
                    "hunttech-modern",
                    "hunttech-modern-light",
                    "hunttech-modern-dark"
            )));

    @Inject
    private DataManager dataManager;
    @Inject
    private FileLoader fileLoader;

    /**
     * Разрешает фон при каждом создании или обновлении главного экрана. Для системного
     * каталога сессия исключает немедленное повторение предыдущего варианта темы.
     */
    public Resource resolveForUser(User currentUser, String themeName, UserSession session) {
        FileDescriptor customBackground = loadUserBackground(currentUser);
        Optional<Resource> customResource = createCustomResource(customBackground);
        if (customResource.isPresent()) {
            return customResource.get();
        }

        String normalizedTheme = normalizeTheme(themeName);
        int variant = nextVariant(session, normalizedTheme);
        return createGeneratedResource(variant);
    }

    /**
     * Маркер имени отделяет пользовательский фон от legacy-фотографии,
     * которая исторически могла находиться в UserSettings.fileImageFace.
     */
    public boolean isCustomBackground(FileDescriptor descriptor) {
        return descriptor != null
                && descriptor.getName() != null
                && descriptor.getName().startsWith(CUSTOM_BACKGROUND_PREFIX);
    }

    private int nextVariant(UserSession session, String normalizedTheme) {
        String attributeName = LAST_VARIANT_ATTRIBUTE + normalizedTheme;
        Integer previous = session.getAttribute(attributeName);
        int variant;

        if (previous == null || previous < 0 || previous >= VARIANT_COUNT) {
            variant = ThreadLocalRandom.current().nextInt(VARIANT_COUNT);
        } else {
            int candidate = ThreadLocalRandom.current().nextInt(VARIANT_COUNT - 1);
            variant = candidate >= previous ? candidate + 1 : candidate;
        }

        session.setAttribute(attributeName, variant);
        return variant;
    }

    private FileDescriptor loadUserBackground(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        return dataManager.load(UserSettings.class)
                .query(QUERY_USER_SETTINGS)
                .parameter("currentUser", currentUser)
                .view("userSettings-view")
                .optional()
                .map(UserSettings::getFileImageFace)
                .orElse(null);
    }

    /**
     * Читает байты пользовательского файла из FileStorage и упаковывает в StreamResource.
     * StreamResource регистрируется через ResourceReference в HrmMainScreen — даёт
     * стабильный Vaadin connector URL, не зависящий от dispatch-сервлета.
     */
    private Optional<Resource> createCustomResource(FileDescriptor descriptor) {
        if (!isCustomBackground(descriptor)) {
            return Optional.empty();
        }

        try (InputStream stream = fileLoader.openStream(descriptor)) {
            byte[] bytes = stream.readAllBytes();
            StreamResource resource = new StreamResource(
                    (StreamResource.StreamSource) () -> new ByteArrayInputStream(bytes),
                    descriptor.getName());
            resource.setMIMEType(mimeType(descriptor));
            resource.setCacheTime(-1);
            return Optional.of(resource);
        } catch (FileStorageException | IOException e) {
            log.warn("Cannot load custom main screen background id={}: {}",
                    descriptor.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    private String mimeType(FileDescriptor descriptor) {
        String extension = descriptor.getExtension();
        if (extension == null) {
            return "application/octet-stream";
        }
        switch (extension.toLowerCase(Locale.ROOT)) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * ThemeResource разрешает путь относительно фактически активной Vaadin-темы:
     * VAADIN/themes/{activeTheme}/backgrounds/{1..10}.jpg.
     */
    private Resource createGeneratedResource(int variant) {
        return new ThemeResource("backgrounds/" + (variant + 1) + ".jpg");
    }

    private String normalizeTheme(String themeName) {
        if (themeName == null) {
            return DEFAULT_THEME;
        }
        String normalized = themeName.toLowerCase(Locale.ROOT);
        return SUPPORTED_THEMES.contains(normalized) ? normalized : DEFAULT_THEME;
    }
}
