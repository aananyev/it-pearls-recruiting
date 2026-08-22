package com.company.hunttech.web.screens.city;

import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.ProjectLogoImageProcessingService;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Region;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@UiController("hunttech_City.edit")
@UiDescriptor("city-edit.xml")
@EditedEntityContainer("cityDc")
@LoadDataBeforeShow
public class CityEdit extends StandardEditor<City> {

    private static final String ACTIVE_NAV_STYLE = "label-nav-item-active";

    @Inject
    private TextField<String> cityRuNameField;

    @Inject
    private LookupPickerField<Region> cityRegionField;

    @Inject
    private Button cityIdentityNav;

    @Inject
    private Button cityRegionNav;

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

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if (!pendingRemovalDescriptors.isEmpty()) {
            for (FileDescriptor descriptor : pendingRemovalDescriptors) {
                try {
                    fileStorageService.removeFile(descriptor);
                    dataManager.remove(descriptor);
                } catch (Exception ex) {
                    // non-fatal
                }
            }
            pendingRemovalDescriptors.clear();
        }
    }

    @Subscribe("enhanceCityEmblemBtn")
    public void onEnhanceCityEmblemBtnClick(Button.ClickEvent event) {
        City city = getEditedEntity();
        FileDescriptor emblemDescriptor = city.getFileCityEmblem();
        if (emblemDescriptor == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Герб отсутствует")
                    .withDescription("Сначала выберите или загрузите изображение герба города")
                    .show();
            return;
        }

        try {
            byte[] originalBytes;
            try (InputStream is = fileLoader.openStream(emblemDescriptor)) {
                originalBytes = IOUtils.toByteArray(is);
            }

            ProcessedImage processed = projectLogoImageProcessingService.process(
                    originalBytes, emblemDescriptor.getName(), false);

            if (processed != null && processed.isProcessed() && processed.getData() != null) {
                FileDescriptor newDescriptor = metadata.create(FileDescriptor.class);
                String uniqueName = processed.getName() + "-" + newDescriptor.getId() + "." + processed.getExtension();
                newDescriptor.setName(uniqueName);
                newDescriptor.setExtension(processed.getExtension());
                newDescriptor.setSize((long) processed.getData().length);
                newDescriptor.setCreateDate(new java.util.Date());

                fileStorageService.saveFile(newDescriptor, processed.getData());
                FileDescriptor committedDescriptor = dataManager.commit(newDescriptor);

                city.setFileCityEmblem(dataContext.merge(committedDescriptor));
                pendingRemovalDescriptors.add(emblemDescriptor);

                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Герб успешно обработан")
                        .withDescription("Улучшено качество, удален фон и выполнено вписывание в круг")
                        .show();
            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Обработка не выполнена")
                        .withDescription("Файл не является поддерживаемым растровым изображением или уже оптимизирован")
                        .show();
            }
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка обработки изображения")
                    .withDescription(e.getMessage())
                    .show();
        }
    }

    /**
     * Сохраняет presentation-контракт прежней версии экрана и переводит
     * пользователя к первому логическому разделу формы.
     */
    public void focusMainSection() {
        focusIdentitySection();
    }

    /**
     * Переводит фокус к наименованию города и отражает выбранный раздел только
     * в presentation-состоянии label-навигации, не изменяя entity и lifecycle editor-а.
     */
    public void focusIdentitySection() {
        activateNavigation(cityIdentityNav, cityRegionNav);
        cityRuNameField.focus();
    }

    /**
     * Переводит фокус к региональной принадлежности города без запуска loader-ов
     * и без изменения значения связанного справочника.
     */
    public void focusRegionSection() {
        activateNavigation(cityRegionNav, cityIdentityNav);
        cityRegionField.focus();
    }

    /**
     * Поддерживает единственное активное состояние sidebar-навигации.
     * Метод меняет только локальные CSS-классы компонентов экрана.
     */
    private void activateNavigation(Button activeButton, Button inactiveButton) {
        activeButton.addStyleName(ACTIVE_NAV_STYLE);
        inactiveButton.removeStyleName(ACTIVE_NAV_STYLE);
    }
}
