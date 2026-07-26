package com.company.hunttech.web.screens.extsettingswindow;

import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.web.screens.mainscreen.MainScreenBackgroundService;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileStorageException;
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
    private DataManager dataManager;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private Notifications notifications;
    @Inject
    private Dialogs dialogs;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private VBoxLayout interfaceSettingsNavigation;

    /** Файлы удаляются только после подтверждённого commit, чтобы Cancel не разрушал прежнюю настройку. */
    private final Set<FileDescriptor> pendingRemoval = new LinkedHashSet<>();
    private FileDescriptor currentBackground;
    private Button interfaceSettingsBackgroundNav;
    private boolean successfulCommitClosing;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        initBackgroundNavigation();
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
     * Добавляет пятый пункт в существующий индекс вкладки «Интерфейс». Навигация
     * переводит фокус к upload-компоненту и не изменяет значения формы.
     */
    private void initBackgroundNavigation() {
        interfaceSettingsBackgroundNav = uiComponents.create(Button.class);
        interfaceSettingsBackgroundNav.setId("interfaceSettingsBackgroundNav");
        interfaceSettingsBackgroundNav.setCaption("Фон главного экрана");
        interfaceSettingsBackgroundNav.setWidth("100%");
        interfaceSettingsBackgroundNav.setStyleName(NAVIGATION_STYLE);
        interfaceSettingsBackgroundNav.addClickListener(event -> selectInterfaceBackgroundSettings());
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
            interfaceSettingsBackgroundNav.setStyleName(active ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        }
    }

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

    /**
     * После успешного сохранения закрывает SettingsWindow без повторного диалога
     * несохранённых изменений: данные уже записаны действующей commit-цепочкой.
     */
    @Override
    protected void commit() {
        UUID settingsId = userSettingsDs.getItem() == null ? null : userSettingsDs.getItem().getId();
        successfulCommitClosing = true;
        try {
            super.commit();
        } finally {
            successfulCommitClosing = false;
        }
        cleanupUnreferencedBackgrounds(settingsId);
    }

    @Override
    public boolean hasUnsavedChanges() {
        return !successfulCommitClosing && super.hasUnsavedChanges();
    }

    /**
     * Отмена всегда требует явного выбора: остаться в форме либо закрыть её с
     * отбрасыванием datasource-изменений. Сохранение из этого сценария недоступно.
     */
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
                                .withHandler(event -> closeWithDiscard())
                )
                .show();
    }

    private void refreshBackgroundStatus() {
        mainScreenBackgroundStatusLabel.setValue(currentBackground == null ? STATUS_THEME : STATUS_CUSTOM);
    }

    private boolean isSupportedImage(FileDescriptor descriptor) {
        String extension = descriptor.getExtension();
        return extension != null && SUPPORTED_EXTENSIONS.contains("." + extension.toLowerCase(Locale.ROOT));
    }

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
