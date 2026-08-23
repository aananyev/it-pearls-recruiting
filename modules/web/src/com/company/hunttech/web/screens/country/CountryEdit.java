package com.company.hunttech.web.screens.country;

import com.company.hunttech.entity.Country;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_Country.edit")
@UiDescriptor("country-edit.xml")
@EditedEntityContainer("countryDc")
@LoadDataBeforeShow
public class CountryEdit extends StandardEditor<Country> {

    @Inject
    private TextField<String> countryRuNameField;
    @Inject
    private Table<?> countryCountryOfRegionTable;
    @Inject
    private Button countryMainNav;
    @Inject
    private Button countryRegionsNav;
    @Inject
    private com.company.hunttech.service.GeoDataEnrichmentService geoDataEnrichmentService;
    @Inject
    private com.haulmont.cuba.gui.Notifications notifications;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CountryEdit.class);

    @Inject
    private com.haulmont.cuba.gui.model.DataContext dataContext;
    @Inject
    private com.haulmont.cuba.core.app.FileStorageService fileStorageService;
    @Inject
    private com.haulmont.cuba.core.global.DataManager dataManager;

    private final java.util.Set<com.haulmont.cuba.core.entity.FileDescriptor> pendingRemovalDescriptors = new java.util.HashSet<>();
    private final java.util.Set<com.haulmont.cuba.core.entity.FileDescriptor> createdUncommittedDescriptors = new java.util.HashSet<>();

    @com.haulmont.cuba.gui.screen.Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        createdUncommittedDescriptors.clear();
        if (!pendingRemovalDescriptors.isEmpty()) {
            for (com.haulmont.cuba.core.entity.FileDescriptor descriptor : pendingRemovalDescriptors) {
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

    @com.haulmont.cuba.gui.screen.Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        if (!event.closedWith(com.haulmont.cuba.gui.screen.StandardOutcome.COMMIT) && !createdUncommittedDescriptors.isEmpty()) {
            for (com.haulmont.cuba.core.entity.FileDescriptor descriptor : createdUncommittedDescriptors) {
                try {
                    fileStorageService.removeFile(descriptor);
                } catch (Exception ex) {
                    log.warn("Не удалось удалить временный файл флага {}: {}", descriptor.getId(), ex.getMessage());
                }
            }
            createdUncommittedDescriptors.clear();
        }
    }

    /**
     * Умное автозаполнение реквизитов страны по API и скачивание флага государства из интернета.
     * Заполняет все поля формы и скачивает флаг в BLOB (flagImage) и FileDescriptor (fileFlag) для отображения.
     */
    public void onEnrichCountryByApi() {
        Country country = getEditedEntity();
        String query = country.getCountryRuName();
        if (query == null || query.trim().isEmpty()) {
            query = country.getCountryShortName();
        }
        if (query == null || query.trim().isEmpty()) {
            query = country.getCountryEngName();
        }
        if (query == null || query.trim().isEmpty()) {
            notifications.create(com.haulmont.cuba.gui.Notifications.NotificationType.WARNING)
                    .withCaption("Укажите наименование страны")
                    .withDescription("Для автоматического поиска введите русское или английское название или код страны (например, Россия, RU, Казахстан).")
                    .show();
            return;
        }

        try {
            com.company.hunttech.entity.GeoCountryData data = geoDataEnrichmentService.enrichCountry(query);
            if (data != null) {
                boolean anyFieldFilled = false;

                if (data.getCountryRuName() != null && (country.getCountryRuName() == null || country.getCountryRuName().trim().isEmpty())) {
                    country.setCountryRuName(data.getCountryRuName());
                    anyFieldFilled = true;
                }
                if (data.getCountryEngName() != null && (country.getCountryEngName() == null || country.getCountryEngName().trim().isEmpty())) {
                    country.setCountryEngName(data.getCountryEngName());
                    anyFieldFilled = true;
                }
                if (data.getCountryShortName() != null && (country.getCountryShortName() == null || country.getCountryShortName().trim().isEmpty())) {
                    country.setCountryShortName(data.getCountryShortName());
                    anyFieldFilled = true;
                }
                if (data.getAlpha3Code() != null && (country.getAlpha3Code() == null || country.getAlpha3Code().trim().isEmpty())) {
                    country.setAlpha3Code(data.getAlpha3Code());
                    anyFieldFilled = true;
                }
                if (data.getNumericCode() != null && (country.getNumericCode() == null || country.getNumericCode().trim().isEmpty())) {
                    country.setNumericCode(data.getNumericCode());
                    anyFieldFilled = true;
                }
                if (data.getCurrencyCode() != null && (country.getCurrencyCode() == null || country.getCurrencyCode().trim().isEmpty())) {
                    country.setCurrencyCode(data.getCurrencyCode());
                    anyFieldFilled = true;
                }
                if (data.getCapital() != null && (country.getCapital() == null || country.getCapital().trim().isEmpty())) {
                    country.setCapital(data.getCapital());
                    anyFieldFilled = true;
                }
                if (data.getPhoneCode() != null && country.getPhoneCode() == null) {
                    country.setPhoneCode(data.getPhoneCode());
                    anyFieldFilled = true;
                }

                // Скачивание флага: сохраняем в BLOB (flagImage) для миграции и в FileDescriptor (fileFlag) для отображения в UI
                boolean flagLoaded = false;
                if (data.getFlagUrl() != null && !data.getFlagUrl().isEmpty()) {
                    // 1. Скачиваем байты флага (BLOB)
                    byte[] flagBytes = geoDataEnrichmentService.downloadImageAsBytes(data.getFlagUrl());
                    if (flagBytes != null && flagBytes.length > 0) {
                        country.setFlagImage(flagBytes);
                        country.setFlagUrl(data.getFlagUrl());
                        flagLoaded = true;
                    }

                    // 2. Создаём FileDescriptor для отображения в fallbackImage (обратная совместимость)
                    if (country.getFileFlag() == null) {
                        com.haulmont.cuba.core.entity.FileDescriptor flagFd = geoDataEnrichmentService.downloadAndSaveImage(
                                data.getFlagUrl(),
                                "flag_" + (data.getCountryShortName() != null ? data.getCountryShortName().toLowerCase() : "country") + ".png");
                        if (flagFd != null) {
                            createdUncommittedDescriptors.add(flagFd);
                            country.setFileFlag(dataContext.merge(flagFd));
                        }
                    }
                }

                notifications.create(com.haulmont.cuba.gui.Notifications.NotificationType.TRAY)
                        .withCaption(anyFieldFilled ? "Реквизиты страны успешно заполнены" : "Реквизиты страны уже заполнены")
                        .withDescription((flagLoaded
                                ? "Коды ISO, валюта, столица и официальный флаг государства загружены."
                                : "Коды ISO, валюта и столица государства получены.") + (anyFieldFilled ? "" : " (все поля уже были заполнены)"))
                        .show();
            } else {
                notifications.create(com.haulmont.cuba.gui.Notifications.NotificationType.HUMANIZED)
                        .withCaption("Данные не найдены")
                        .withDescription("Не удалось получить реквизиты для запроса: " + query)
                        .show();
            }
        } catch (Exception e) {
            log.error("Ошибка автозаполнения страны: {}", e.getMessage(), e);
            notifications.create(com.haulmont.cuba.gui.Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка обращения к Geo-API")
                    .withDescription("Не удалось получить данные сервиса. Попробуйте позже или проверьте настройки.")
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
