package com.company.hunttech.web.screens.region;

import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.ProjectLogoImageProcessingService;
import com.company.hunttech.entity.Region;
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

@UiController("hunttech_Region.edit")
@UiDescriptor("region-edit.xml")
@EditedEntityContainer("regionDc")
@LoadDataBeforeShow
public class RegionEdit extends StandardEditor<Region> {

    private static final Logger log = LoggerFactory.getLogger(RegionEdit.class);

    @Inject
    private TextField<String> regionRuNameField;
    @Inject
    private Table<?> regionRegionOfCityTable;
    @Inject
    private Button regionMainNav;
    @Inject
    private Button regionCitiesNav;
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
     * Умное автозаполнение реквизитов региона по API и поиск герба.
     * Заполняет все поля формы и скачивает герб в BLOB (emblemImage) и FileDescriptor (fileRegionEmblem) для отображения.
     */
    public void onEnrichRegionByApi() {
        Region region = getEditedEntity();
        String query = region.getRegionRuName();
        if (query == null || query.trim().isEmpty()) {
            query = region.getRegionEngName();
        }
        if (query == null || query.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Укажите наименование региона")
                    .withDescription("Для автоматического поиска введите название региона (например, Московская область, Татарстан, Санкт-Петербург).")
                    .show();
            return;
        }

        try {
            com.company.hunttech.entity.GeoRegionData data = geoDataEnrichmentService.enrichRegion(query, region.getRegionCountry());
            if (data != null) {
                boolean anyFieldFilled = false;

                if (data.getRegionRuName() != null && (region.getRegionRuName() == null || region.getRegionRuName().trim().isEmpty())) {
                    region.setRegionRuName(data.getRegionRuName());
                    anyFieldFilled = true;
                }
                if (data.getRegionEngName() != null && (region.getRegionEngName() == null || region.getRegionEngName().trim().isEmpty())) {
                    region.setRegionEngName(data.getRegionEngName());
                    anyFieldFilled = true;
                }
                if (data.getRegionCode() != null && region.getRegionCode() == null) {
                    region.setRegionCode(data.getRegionCode());
                    anyFieldFilled = true;
                }
                if (data.getIsoCode() != null && (region.getIsoCode() == null || region.getIsoCode().trim().isEmpty())) {
                    region.setIsoCode(data.getIsoCode());
                    anyFieldFilled = true;
                }
                if (data.getRegionType() != null && (region.getRegionType() == null || region.getRegionType().trim().isEmpty())) {
                    region.setRegionType(data.getRegionType());
                    anyFieldFilled = true;
                }
                if (data.getCapital() != null && (region.getCapital() == null || region.getCapital().trim().isEmpty())) {
                    region.setCapital(data.getCapital());
                    anyFieldFilled = true;
                }
                if (data.getTimeZone() != null && (region.getTimeZone() == null || region.getTimeZone().trim().isEmpty())) {
                    region.setTimeZone(data.getTimeZone());
                    anyFieldFilled = true;
                }
                if (data.getFiasId() != null && (region.getFiasId() == null || region.getFiasId().trim().isEmpty())) {
                    region.setFiasId(data.getFiasId());
                    anyFieldFilled = true;
                }

                // Скачивание герба: сохраняем в BLOB (emblemImage) для миграции и в FileDescriptor (fileRegionEmblem) для отображения в UI
                boolean emblemLoaded = false;
                if (data.getEmblemUrl() != null && !data.getEmblemUrl().isEmpty()) {
                    // 1. Скачиваем байты герба (BLOB)
                    byte[] emblemBytes = geoDataEnrichmentService.downloadImageAsBytes(data.getEmblemUrl());
                    if (emblemBytes != null && emblemBytes.length > 0) {
                        region.setEmblemImage(emblemBytes);
                        region.setEmblemUrl(data.getEmblemUrl());
                        emblemLoaded = true;
                    }

                    // 2. Создаём FileDescriptor для отображения в fallbackImage (обратная совместимость)
                    if (region.getFileRegionEmblem() == null) {
                        FileDescriptor emblemFd = geoDataEnrichmentService.downloadAndSaveImage(
                                data.getEmblemUrl(),
                                "emblem_region_" + (data.getIsoCode() != null ? data.getIsoCode().toLowerCase() : "region") + ".png");
                        if (emblemFd != null) {
                            createdUncommittedDescriptors.add(emblemFd);
                            region.setFileRegionEmblem(dataContext.merge(emblemFd));
                        }
                    }
                }

                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(anyFieldFilled ? "Реквизиты региона успешно заполнены" : "Реквизиты региона уже заполнены")
                        .withDescription((emblemLoaded
                                ? "Коды, тип региона, столица и герб получены."
                                : "Коды, тип региона и столица получены.") + (anyFieldFilled ? "" : " (все поля уже были заполнены)"))
                        .show();
            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Данные не найдены")
                        .withDescription("Не удалось получить реквизиты для региона: " + query)
                        .show();
            }
        } catch (Exception e) {
            log.error("Ошибка автозаполнения региона: {}", e.getMessage(), e);
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
                    log.warn("Не удалось удалить устаревший дескриптор герба региона {}: {}", descriptor.getId(), ex.getMessage());
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
                    log.warn("Не удалось удалить временный файл герба региона {}: {}", descriptor.getId(), ex.getMessage());
                }
            }
            createdUncommittedDescriptors.clear();
        }
    }

    @Subscribe("enhanceRegionEmblemBtn")
    public void onEnhanceRegionEmblemBtnClick(Button.ClickEvent event) {
        Region region = getEditedEntity();
        FileDescriptor emblemDescriptor = region.getFileRegionEmblem();
        if (emblemDescriptor == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Герб отсутствует")
                    .withDescription("Сначала выберите или загрузите изображение герба региона")
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

                region.setFileRegionEmblem(mergedDescriptor);
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
            log.error("Ошибка при умной обработке герба региона", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка обработки изображения")
                    .withDescription("Не удалось обработать изображение, попробуйте позже")
                    .show();
        }
    }

    /**
     * Переводит фокус к основным реквизитам региона без изменения данных и загрузчиков.
     */
    public void focusMainSection() {
        regionRuNameField.focus();
        setActiveNavigation(regionMainNav, regionCitiesNav);
    }

    /**
     * Переводит фокус к composition-таблице городов, сохраняя исходные actions и lifecycle.
     */
    public void focusCitiesSection() {
        regionRegionOfCityTable.focus();
        setActiveNavigation(regionCitiesNav, regionMainNav);
    }

    private void setActiveNavigation(Button activeButton, Button inactiveButton) {
        activeButton.addStyleName("label-nav-item-active");
        inactiveButton.removeStyleName("label-nav-item-active");
    }
}
