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

@UiController("hunttech_City.edit")
@UiDescriptor("city-edit.xml")
@EditedEntityContainer("cityDc")
@LoadDataBeforeShow
public class CityEdit extends StandardEditor<City> {

    private static final Logger log = LoggerFactory.getLogger(CityEdit.class);
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
    @Inject
    private com.company.hunttech.service.GeoDataEnrichmentService geoDataEnrichmentService;

    /**
     * Умное автозаполнение реквизитов города по API и поиск герба.
     */
    public void onEnrichCityByApi() {
        City city = getEditedEntity();
        String query = city.getCityRuName();
        if (query == null || query.trim().isEmpty()) {
            query = city.getCityEngName();
        }
        if (query == null || query.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Укажите наименование города")
                    .withDescription("Для автоматического поиска введите название города (например, Москва, Казань, Екатеринбург).")
                    .show();
            return;
        }

        try {
            com.company.hunttech.entity.Region region = city.getCityRegion();
            com.company.hunttech.entity.Country country = region != null ? region.getRegionCountry() : null;
            com.company.hunttech.entity.GeoCityData data = geoDataEnrichmentService.enrichCity(query, region, country);
            if (data != null) {
                if (data.getCityRuName() != null && (city.getCityRuName() == null || city.getCityRuName().trim().isEmpty())) {
                    city.setCityRuName(data.getCityRuName());
                }
                if (data.getCityEngName() != null && (city.getCityEngName() == null || city.getCityEngName().trim().isEmpty())) {
                    city.setCityEngName(data.getCityEngName());
                }
                if (data.getCityPhoneCode() != null && (city.getCityPhoneCode() == null || city.getCityPhoneCode().trim().isEmpty())) {
                    city.setCityPhoneCode(data.getCityPhoneCode());
                }
                if (data.getPostalCode() != null && (city.getPostalCode() == null || city.getPostalCode().trim().isEmpty())) {
                    city.setPostalCode(data.getPostalCode());
                }
                if (data.getPopulation() != null && city.getPopulation() == null) {
                    city.setPopulation(data.getPopulation());
                }
                if (data.getLatitude() != null && city.getLatitude() == null) {
                    city.setLatitude(data.getLatitude());
                }
                if (data.getLongitude() != null && city.getLongitude() == null) {
                    city.setLongitude(data.getLongitude());
                }
                if (data.getTimeZone() != null && (city.getTimeZone() == null || city.getTimeZone().trim().isEmpty())) {
                    city.setTimeZone(data.getTimeZone());
                }
                if (data.getFiasId() != null && (city.getFiasId() == null || city.getFiasId().trim().isEmpty())) {
                    city.setFiasId(data.getFiasId());
                }

                // Скачивание и привязка герба города
                boolean emblemLoaded = false;
                if (data.getEmblemUrl() != null && !data.getEmblemUrl().isEmpty() && city.getFileCityEmblem() == null) {
                    FileDescriptor emblemFd = geoDataEnrichmentService.downloadAndSaveImage(
                            data.getEmblemUrl(),
                            "emblem_city_" + System.currentTimeMillis() + ".png");
                    if (emblemFd != null) {
                        createdUncommittedDescriptors.add(emblemFd);
                        city.setFileCityEmblem(dataContext.merge(emblemFd));
                        emblemLoaded = true;
                    }
                }

                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Реквизиты города успешно заполнены")
                        .withDescription(emblemLoaded
                                ? "Телефонный код, индекс, население, координаты и герб получены."
                                : "Телефонный код, индекс, население и координаты получены.")
                        .show();
            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Данные не найдены")
                        .withDescription("Не удалось получить реквизиты для города: " + query)
                        .show();
            }
        } catch (Exception e) {
            log.error("Ошибка автозаполнения города: {}", e.getMessage(), e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка обращения к Geo-API")
                    .withDescription("Не удалось получить данные сервиса. Попробуйте позже или проверьте настройки.")
                    .show();
        }
    }

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
                    log.warn("Не удалось удалить устаревший дескриптор герба города {}: {}", descriptor.getId(), ex.getMessage());
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
                    log.warn("Не удалось удалить временный файл герба города {}: {}", descriptor.getId(), ex.getMessage());
                }
            }
            createdUncommittedDescriptors.clear();
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
                createdUncommittedDescriptors.add(newDescriptor);
                FileDescriptor mergedDescriptor = dataContext.merge(newDescriptor);

                city.setFileCityEmblem(mergedDescriptor);
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
            log.error("Ошибка при умной обработке герба города", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка обработки изображения")
                    .withDescription("Не удалось обработать изображение, попробуйте позже")
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
