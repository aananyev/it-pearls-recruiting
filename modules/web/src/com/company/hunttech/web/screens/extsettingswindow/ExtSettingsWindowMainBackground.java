package com.company.hunttech.web.screens.extsettingswindow;

import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.web.screens.mainscreen.MainScreenBackgroundChangedEvent;
import com.company.hunttech.web.screens.mainscreen.MainScreenBackgroundImageProcessor;
import com.company.hunttech.web.screens.mainscreen.MainScreenBackgroundService;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.data.Datasource;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Добавляет во вкладку «Интерфейс» настройку фонового изображения главного экрана.
 * Для хранения используется существующий UserSettings.fileImageFace; entity и БД не меняются.
 */
public class ExtSettingsWindowMainBackground extends ExtSettingsWindowInterfaceLayout {

    private static final Set<String> SUPPORTED_EXTENSIONS = new LinkedHashSet<>(
            Arrays.asList(".png", ".jpg", ".jpeg", ".webp"));
    private static final String STATUS_THEME = "Используется случайный фон активной темы.";
    private static final String STATUS_CUSTOM = "Используется пользовательский фон.";
    private static final String UPLOAD_ERROR =
            "Не удалось загрузить изображение. Проверьте формат, содержимое и размер файла.";
    private static final String REMOVE_ERROR =
            "Не удалось удалить неиспользуемый файл фона. Ссылка на него больше не используется.";
    private static final String NAVIGATION_STYLE = "borderless settings-section-nav-item";
    private static final String ACTIVE_NAVIGATION_STYLE =
            "borderless settings-section-nav-item settings-section-nav-item-active";

    @Inject
    private Datasource<UserSettings> userSettingsDs;
    @Inject
    private FileUploadField mainScreenBackgroundUpload;
    @Inject
    private Label<String> mainScreenBackgroundStatusLabel;
    @Inject
    private MainScreenBackgroundService mainScreenBackgroundService;
    @Inject
    private MainScreenBackgroundImageProcessor imageProcessor;
    @Inject
    private DataManager dataManager;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private Metadata metadata;
    @Inject
    private Events events;
    @Inject
    private Notifications notifications;
    @Inject
    private Dialogs dialogs;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private VBoxLayout interfaceSettingsNavigation;

    /** Старые фоны удаляются только после подтверждённого commit, чтобы Cancel не разрушал настройку. */
    private final Set<FileDescriptor> pendingRemoval = new LinkedHashSet<>();
    /** Новые нормализованные файлы удаляются при discard или если после commit они не стали активными. */
    private final Set<FileDescriptor> pendingCreated = new LinkedHashSet<>();
    private FileDescriptor currentBackground;
    private Button interfaceSettingsBackgroundNav;
    private boolean successfulCommitClosing;
    private boolean backgroundChanged;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        initBackgroundNavigation();
        mainScreenBackgroundUpload.setPermittedExtensions(SUPPORTED_EXTENSIONS);
        mainScreenBackgroundUpload.setFileSizeLimit(
                MainScreenBackgroundImageProcessor.MAX_INPUT_BYTES);

        FileDescriptor storedFile = userSettingsDs.getItem() == null
                ? null : userSettingsDs.getItem().getFileImageFace();
        currentBackground = mainScreenBackgroundService.isCustomBackground(storedFile)
                ? storedFile : null;
        mainScreenBackgroundUpload.setValue(currentBackground);

        mainScreenBackgroundUpload.addFileUploadSucceedListener(
                event -> onMainScreenBackgroundUploaded());
        mainScreenBackgroundUpload.addFileUploadErrorListener(
                event -> onMainScreenBackgroundUploadError());
        refreshBackgroundStatus();
    }

    private void initBackgroundNavigation() {
        interfaceSettingsBackgroundNav = uiComponents.create(Button.class);
        interfaceSettingsBackgroundNav.setId("interfaceSettingsBackgroundNav");
        interfaceSettingsBackgroundNav.setCaption("Фон главного экрана");
        interfaceSettingsBackgroundNav.setWidth("100%");
        interfaceSettingsBackgroundNav.setStyleName(NAVIGATION_STYLE);
        interfaceSettingsBackgroundNav.addClickListener(
                event -> selectInterfaceBackgroundSettings());
        interfaceSettingsNavigation.add(interfaceSettingsBackgroundNav);
    }

    public void selectInterfaceBackgroundSettings() {
        for (Component component : interfaceSettingsNavigation.getComponents()) {
            if (component instanceof Button) {
                ((Button) component).setStyleName(NAVIGATION_STYLE);
            }
        }
        interfaceSettingsBackgroundNav.setStyleName(ACTIVE_NAVIGATION_STYLE);
        mainScreenBackgroundUpload.focus();
    }

    @Override
    public void selectInterfaceWindowSettings() {
        setBackgroundNavigationActive(false);
        super.selectInterfaceWindowSettings();
    }

    @Override
    public void selectInterfaceAppearanceSettings() {
        setBackgroundNavigationActive(false);
        super.selectInterfaceAppearanceSettings();
    }

    @Override
    public void selectInterfaceRegionalSettings() {
        setBackgroundNavigationActive(false);
        super.selectInterfaceRegionalSettings();
    }

    @Override
    public void selectInterfaceStartupSettings() {
        setBackgroundNavigationActive(false);
        super.selectInterfaceStartupSettings();
    }

    private void setBackgroundNavigationActive(boolean active) {
        if (interfaceSettingsBackgroundNav != null) {
            interfaceSettingsBackgroundNav.setStyleName(
                    active ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        }
    }

    /**
     * FileUploadField.getFileDescriptor() может вернуть null при IMMEDIATE.
     * getValue() всегда возвращает загруженный FileDescriptor после успешного upload.
     */
    private FileDescriptor getUploadedDescriptor() {
        FileDescriptor descriptor = mainScreenBackgroundUpload.getFileDescriptor();
        if (descriptor == null) {
            Object value = mainScreenBackgroundUpload.getValue();
            if (value instanceof FileDescriptor) {
                descriptor = (FileDescriptor) value;
            }
        }
        return descriptor;
    }

    private void onMainScreenBackgroundUploaded() {
        FileDescriptor uploaded = getUploadedDescriptor();
        if (uploaded == null || userSettingsDs.getItem() == null) {
            return;
        }

        String originalName = uploaded.getName();
        FileDescriptor normalizedDescriptor = null;
        try {
            MainScreenBackgroundImageProcessor.ProcessedImage processed;
            try (InputStream stream = fileLoader.openStream(uploaded)) {
                processed = imageProcessor.process(stream.readAllBytes(), originalName);
            }

            normalizedDescriptor = createNormalizedDescriptor(processed, originalName);
            FileDescriptor committedDescriptor = dataManager.commit(normalizedDescriptor);
            pendingCreated.add(committedDescriptor);

            if (currentBackground != null
                    && !Objects.equals(currentBackground.getId(), committedDescriptor.getId())) {
                pendingRemoval.add(currentBackground);
            }

            currentBackground = committedDescriptor;
            userSettingsDs.getItem().setFileImageFace(committedDescriptor);
            mainScreenBackgroundUpload.setValue(committedDescriptor);
            backgroundChanged = true;
            refreshBackgroundStatus();
        } catch (MainScreenBackgroundImageProcessor.ImageValidationException e) {
            if (normalizedDescriptor != null) {
                removeStoredFile(normalizedDescriptor, true);
            }
            mainScreenBackgroundUpload.setValue(currentBackground);
            refreshBackgroundStatus();
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(e.getMessage())
                    .show();
        } catch (Exception e) {
            if (normalizedDescriptor != null) {
                removeStoredFile(normalizedDescriptor, true);
            }
            mainScreenBackgroundUpload.setValue(currentBackground);
            refreshBackgroundStatus();
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(UPLOAD_ERROR)
                    .show();
        } finally {
            // IMMEDIATE upload является временным входом; хранится только новый нормализованный descriptor.
            removeStoredFile(uploaded, false);
        }
    }

    private FileDescriptor createNormalizedDescriptor(
            MainScreenBackgroundImageProcessor.ProcessedImage processed,
            String originalName) throws FileStorageException {
        FileDescriptor descriptor = metadata.create(FileDescriptor.class);
        descriptor.setName(MainScreenBackgroundService.CUSTOM_BACKGROUND_PREFIX
                + descriptor.getId() + "-" + safeOriginalName(originalName) + ".jpg");
        descriptor.setExtension(processed.getExtension());
        descriptor.setSize((long) processed.getBytes().length);
        descriptor.setCreateDate(new Date());
        fileLoader.saveStream(descriptor,
                () -> new ByteArrayInputStream(processed.getBytes()));
        return descriptor;
    }

    private String safeOriginalName(String originalName) {
        String value = originalName == null ? "background" : originalName.trim();
        int dot = value.lastIndexOf('.');
        if (dot > 0) {
            value = value.substring(0, dot);
        }
        value = value.replaceAll("[^\\p{L}\\p{N}._-]+", "_");
        if (value.isEmpty()) {
            value = "background";
        }
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private void onMainScreenBackgroundUploadError() {
        mainScreenBackgroundUpload.setValue(currentBackground);
        refreshBackgroundStatus();
        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption(UPLOAD_ERROR)
                .show();
    }

    public void clearMainScreenBackground() {
        if (userSettingsDs.getItem() == null || currentBackground == null) {
            refreshBackgroundStatus();
            return;
        }
        pendingRemoval.add(currentBackground);
        currentBackground = null;
        userSettingsDs.getItem().setFileImageFace(null);
        mainScreenBackgroundUpload.setValue(null);
        backgroundChanged = true;
        refreshBackgroundStatus();
    }

    @Override
    protected void commit() {
        UUID settingsId = userSettingsDs.getItem() == null
                ? null : userSettingsDs.getItem().getId();
        boolean publishBackgroundChange = backgroundChanged;
        successfulCommitClosing = true;
        try {
            super.commit();
        } finally {
            successfulCommitClosing = false;
        }

        cleanupUnreferencedBackgrounds(settingsId);
        backgroundChanged = false;
        if (publishBackgroundChange) {
            events.publish(new MainScreenBackgroundChangedEvent(this));
        }
    }

    @Override
    public boolean hasUnsavedChanges() {
        return !successfulCommitClosing && super.hasUnsavedChanges();
    }

    @Override
    protected void cancel() {
        dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                .withCaption("Выход из настроек")
                .withMessage("Остаться в экране или выйти без сохранения?")
                .withActions(
                        new BaseAction("stayInSettings")
                                .withCaption("Остаться")
                                .withPrimary(true),
                        new BaseAction("discardSettings")
                                .withCaption("Выйти без сохранения")
                                .withHandler(event -> discardAndClose())
                )
                .show();
    }

    private void discardAndClose() {
        for (FileDescriptor descriptor : new LinkedHashSet<>(pendingCreated)) {
            removeStoredFile(descriptor, true);
        }
        pendingCreated.clear();
        pendingRemoval.clear();
        backgroundChanged = false;
        closeWithDiscard();
    }

    private void refreshBackgroundStatus() {
        mainScreenBackgroundStatusLabel.setValue(
                currentBackground == null ? STATUS_THEME : STATUS_CUSTOM);
    }

    private void cleanupUnreferencedBackgrounds(UUID settingsId) {
        UUID activeFileId = loadActiveFileId(settingsId);
        Set<FileDescriptor> cleanupCandidates = new LinkedHashSet<>(pendingRemoval);
        cleanupCandidates.addAll(pendingCreated);

        for (FileDescriptor descriptor : cleanupCandidates) {
            if (mainScreenBackgroundService.isCustomBackground(descriptor)
                    && !Objects.equals(descriptor.getId(), activeFileId)) {
                removeStoredFile(descriptor, true);
            }
        }
        pendingRemoval.clear();
        pendingCreated.clear();
    }

    private UUID loadActiveFileId(UUID settingsId) {
        if (settingsId == null) {
            return null;
        }
        return dataManager.load(UserSettings.class)
                .id(settingsId)
                .view("userSettings-view")
                .optional()
                .map(UserSettings::getFileImageFace)
                .map(FileDescriptor::getId)
                .orElse(null);
    }

    /**
     * deleteDescriptor=false применяется к временному IMMEDIATE upload: его запись
     * может ещё не быть закоммичена CUBA, поэтому удаление metadata выполняется best effort.
     */
    private void removeStoredFile(FileDescriptor descriptor, boolean deleteDescriptor) {
        if (descriptor == null) {
            return;
        }
        try {
            fileStorageService.removeFile(descriptor);
            if (deleteDescriptor) {
                dataManager.remove(descriptor);
            } else {
                try {
                    dataManager.remove(descriptor);
                } catch (RuntimeException ignored) {
                    // Временный FileDescriptor мог не попасть в middleware store.
                }
            }
        } catch (FileStorageException | RuntimeException e) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(REMOVE_ERROR)
                    .show();
        }
    }
}
