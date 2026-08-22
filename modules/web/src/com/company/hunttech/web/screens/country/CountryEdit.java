package com.company.hunttech.web.screens.country;

import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.ProjectLogoImageProcessingService;
import com.company.hunttech.entity.Country;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.StandardOutcome;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@UiController("hunttech_Country.edit")
@UiDescriptor("country-edit.xml")
@EditedEntityContainer("countryDc")
@LoadDataBeforeShow
public class CountryEdit extends StandardEditor<Country> {

    private static final Logger log = LoggerFactory.getLogger(CountryEdit.class);

    @Inject
    private TextField<String> countryRuNameField;
    @Inject
    private Table<?> countryCountryOfRegionTable;
    @Inject
    private Button countryMainNav;
    @Inject
    private Button countryRegionsNav;
    @Inject
    private ProjectLogoImageProcessingService projectLogoImageProcessingService;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private Metadata metadata;
    @Inject
    private DataManager dataManager;
    @Inject
    private DataContext dataContext;
    @Inject
    private Notifications notifications;

    private final Set<FileDescriptor> pendingRemovalDescriptors = new HashSet<>();
    private final Set<FileDescriptor> createdUncommittedDescriptors = new HashSet<>();

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        createdUncommittedDescriptors.clear();
        if (!pendingRemovalDescriptors.isEmpty()) {
            for (FileDescriptor descriptor : pendingRemovalDescriptors) {
                try {
                    fileStorageService.removeFile(descriptor);
                    dataManager.remove(descriptor);
                } catch (Exception ex) {
                    log.warn("Не удалось удалить устаревший дескриптор флага {}: {}", descriptor.getId(), ex.getMessage());
                }
            }
            pendingRemovalDescriptors.clear();
        }
    }

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        if (!event.closedWith(StandardOutcome.COMMIT) && !createdUncommittedDescriptors.isEmpty()) {
            for (FileDescriptor descriptor : createdUncommittedDescriptors) {
                try {
                    fileStorageService.removeFile(descriptor);
                } catch (Exception ex) {
                    log.warn("Не удалось удалить временный файл флага {}: {}", descriptor.getId(), ex.getMessage());
                }
            }
            createdUncommittedDescriptors.clear();
        }
    }

    @Subscribe("enhanceFlagBtn")
    public void onEnhanceFlagBtnClick(Button.ClickEvent event) {
        Country country = getEditedEntity();
        FileDescriptor flagDescriptor = country.getFileFlag();
        if (flagDescriptor == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Флаг отсутствует")
                    .withDescription("Сначала выберите или загрузите изображение флага")
                    .show();
            return;
        }

        try {
            byte[] originalBytes;
            try (InputStream is = fileLoader.openStream(flagDescriptor)) {
                originalBytes = IOUtils.toByteArray(is);
            }

            ProcessedImage processed = projectLogoImageProcessingService.process(
                    originalBytes, flagDescriptor.getName(), false);

            if (processed != null && processed.isProcessed() && processed.getData() != null) {
                FileDescriptor newDescriptor = metadata.create(FileDescriptor.class);
                String uniqueName = processed.getName() + "-" + newDescriptor.getId() + "." + processed.getExtension();
                newDescriptor.setName(uniqueName);
                newDescriptor.setExtension(processed.getExtension());
                newDescriptor.setSize((long) processed.getData().length);
                newDescriptor.setCreateDate(new java.util.Date());

                fileStorageService.saveFile(newDescriptor, processed.getData());
                FileDescriptor mergedDescriptor = dataContext.merge(newDescriptor);

                country.setFileFlag(mergedDescriptor);
                createdUncommittedDescriptors.add(newDescriptor);
                pendingRemovalDescriptors.add(flagDescriptor);

                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Флаг успешно обработан")
                        .withDescription("Улучшено качество, удален фон и выполнено вписывание в круг")
                        .show();
            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Обработка не выполнена")
                        .withDescription("Файл не является поддерживаемым растровым изображением или уже оптимизирован")
                        .show();
            }
        } catch (Exception e) {
            log.error("Ошибка при умной обработке флага страны", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка обработки изображения")
                    .withDescription("Не удалось обработать изображение, попробуйте позже")
                    .show();
        }
    }

    /**
     * Переводит фокус к основным реквизитам страны, не затрагивая entity и lifecycle сохранения.
     */
    public void focusMainSection() {
        countryRuNameField.focus();
        setActiveNavigation(countryMainNav, countryRegionsNav);
    }

    /**
     * Переводит фокус к таблице регионов, сохраняя исходные actions composition-коллекции.
     */
    public void focusRegionsSection() {
        countryCountryOfRegionTable.focus();
        setActiveNavigation(countryRegionsNav, countryMainNav);
    }

    private void setActiveNavigation(Button activeButton, Button inactiveButton) {
        activeButton.addStyleName("label-nav-item-active");
        inactiveButton.removeStyleName("label-nav-item-active");
    }
}
