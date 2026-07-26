package com.company.hunttech.web.screens.extsettingswindow;

import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.web.screens.mainscreen.MainScreenBackgroundService;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.data.Datasource;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Добавляет во вкладку «Интерфейс» настройку фонового изображения главного экрана.
 * Для хранения используется существующий UserSettings.fileImageFace; entity и БД не меняются.
 */
public class ExtSettingsWindowMainBackground extends ExtSettingsWindowInterfaceLayout {

    private static final long MAX_BACKGROUND_FILE_SIZE = 15L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_EXTENSIONS = new LinkedHashSet<>(
            Arrays.asList(".png", ".jpg", ".jpeg", ".webp"));
    private static final String STATUS_THEME = "Используется случайный фон активной темы.";
    private static final String STATUS_CUSTOM = "Используется персональное изображение пользователя.";
    private static final String UNSUPPORTED_FILE = "Поддерживаются PNG, JPG, JPEG и WEBP размером до 15 МБ.";
    private static final String REMOVE_ERROR = "Не удалось удалить прежний файл фона. Ссылка на него больше не используется.";

    @Inject
    private Datasource<UserSettings> userSettingsDs;
    @Inject
    private FileUploadField mainScreenBackgroundUpload;
    @Inject
    private Label<String> mainScreenBackgroundStatusLabel;
    @Inject
    private MainScreenBackgroundService mainScreenBackgroundService;
    @Inject
    private DataManager dataManager;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private Notifications notifications;

    /** Файлы удаляются только после подтверждённого commit, чтобы Cancel не разрушал прежнюю настройку. */
    private final Set<FileDescriptor> pendingRemoval = new LinkedHashSet<>();
    private FileDescriptor currentBackground;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        mainScreenBackgroundUpload.setPermittedExtensions(SUPPORTED_EXTENSIONS);
        mainScreenBackgroundUpload.setFileSizeLimit(MAX_BACKGROUND_FILE_SIZE);

        FileDescriptor storedFile = userSettingsDs.getItem() == null
                ? null : userSettingsDs.getItem().getFileImageFace();
        currentBackground = mainScreenBackgroundService.isCustomBackground(storedFile) ? storedFile : null;
        mainScreenBackgroundUpload.setValue(currentBackground);
        mainScreenBackgroundUpload.addFileUploadSucceedListener(event -> onMainScreenBackgroundUploaded());
        refreshBackgroundStatus();
    }

    /**
     * Помечает новый FileDescriptor специальным префиксом. Это исключает трактовку
     * legacy-фотографии из fileImageFace как пользовательского фонового изображения.
     */
    private void onMainScreenBackgroundUploaded() {
        FileDescriptor uploaded = mainScreenBackgroundUpload.getFileDescriptor();
        if (uploaded == null || userSettingsDs.getItem() == null) {
            return;
        }
        if (!isSupportedImage(uploaded)) {
            removeStoredFile(uploaded);
            mainScreenBackgroundUpload.setValue(currentBackground);
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(UNSUPPORTED_FILE)
                    .show();
            return;
        }

        if (currentBackground != null && !Objects.equals(currentBackground.getId(), uploaded.getId())) {
            pendingRemoval.add(currentBackground);
        }

        String extension = uploaded.getExtension();
        uploaded.setName(MainScreenBackgroundService.CUSTOM_BACKGROUND_PREFIX
                + uploaded.getId()
                + (extension == null || extension.isEmpty() ? "" : "." + extension.toLowerCase(Locale.ROOT)));
        FileDescriptor committedDescriptor = dataManager.commit(uploaded);

        currentBackground = committedDescriptor;
        userSettingsDs.getItem().setFileImageFace(committedDescriptor);
        mainScreenBackgroundUpload.setValue(committedDescriptor);
        refreshBackgroundStatus();
    }

    /**
     * Сбрасывает только маркированный персональный фон. Не относящийся к фону legacy-файл
     * в том же историческом поле не изменяется новой логикой.
     */
    public void clearMainScreenBackground() {
        if (userSettingsDs.getItem() == null || currentBackground == null) {
            refreshBackgroundStatus();
            return;
        }
        pendingRemoval.add(currentBackground);
        currentBackground = null;
        userSettingsDs.getItem().setFileImageFace(null);
        mainScreenBackgroundUpload.setValue(null);
        refreshBackgroundStatus();
    }

    @Override
    protected void commit() {
        UUID settingsId = userSettingsDs.getItem() == null ? null : userSettingsDs.getItem().getId();
        super.commit();
        cleanupUnreferencedBackgrounds(settingsId);
    }

    private void refreshBackgroundStatus() {
        mainScreenBackgroundStatusLabel.setValue(currentBackground == null ? STATUS_THEME : STATUS_CUSTOM);
    }

    private boolean isSupportedImage(FileDescriptor descriptor) {
        String extension = descriptor.getExtension();
        return extension != null && SUPPORTED_EXTENSIONS.contains("." + extension.toLowerCase(Locale.ROOT));
    }

    /**
     * После commit перечитывает активную ссылку и удаляет только собственные background-файлы,
     * которые больше не используются. Legacy-файлы без префикса никогда не удаляются.
     */
    private void cleanupUnreferencedBackgrounds(UUID settingsId) {
        UUID activeFileId = null;
        if (settingsId != null) {
            activeFileId = dataManager.load(UserSettings.class)
                    .id(settingsId)
                    .view("userSettings-view")
                    .optional()
                    .map(UserSettings::getFileImageFace)
                    .map(FileDescriptor::getId)
                    .orElse(null);
        }

        for (FileDescriptor descriptor : pendingRemoval) {
            if (mainScreenBackgroundService.isCustomBackground(descriptor)
                    && !Objects.equals(descriptor.getId(), activeFileId)) {
                removeStoredFile(descriptor);
            }
        }
        pendingRemoval.clear();
    }

    private void removeStoredFile(FileDescriptor descriptor) {
        if (descriptor == null) {
            return;
        }
        try {
            fileStorageService.removeFile(descriptor);
            dataManager.remove(descriptor);
        } catch (FileStorageException e) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(REMOVE_ERROR)
                    .show();
        }
    }
}
