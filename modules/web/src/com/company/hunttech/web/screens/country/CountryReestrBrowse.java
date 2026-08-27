package com.company.hunttech.web.screens.country;

import com.company.hunttech.entity.Country;
import com.company.hunttech.service.GeoBulkLoaderService;
import com.hunttech.hrm.gui.components.FallbackImage;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.util.*;

@UiController("hunttech_CountryReestr.browse")
@UiDescriptor("country-reestr-browse.xml")
@LookupComponent("countriesTable")
@LoadDataBeforeShow
public class CountryReestrBrowse extends StandardLookup<Country> {

    private static final String QUERY_REGIONS_COUNT_BY_COUNTRIES =
            "select e.regionCountry, count(e) from hunttech_Region e "
                    + "where e.regionCountry in :countries group by e.regionCountry";

    private static final String QUERY_CITIES_COUNT_BY_REGIONS =
            "select r.regionCountry, count(c) from hunttech_City c join c.cityRegion r "
                    + "where r.regionCountry in :countries group by r.regionCountry";

    @Inject
    private CollectionContainer<Country> countriesDc;
    @Inject
    private CollectionLoader<Country> countriesDl;
    @Inject
    private GroupTable<Country> countriesTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private OvaFallbackImage logoPic;
    @Inject
    private FallbackImage detailFlagImage;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailLocation;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Label<String> detailShortName;
    @Inject
    private Label<String> detailAlpha3Code;
    @Inject
    private Label<String> detailNumericCode;
    @Inject
    private Label<String> detailCapital;
    @Inject
    private Label<String> detailCurrencyCode;
    @Inject
    private Label<String> detailPhoneCode;
    @Inject
    private Label<String> detailEngName;
    @Inject
    private Label<String> detailRegionsCount;
    @Inject
    private Label<String> detailCitiesCount;
    @Inject
    private Button loadAllCountriesBtn;

    @Inject
    private GeoBulkLoaderService geoBulkLoaderService;
    @Inject
    private Notifications notifications;
    @Inject
    private BackgroundWorker backgroundWorker;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CountryReestrBrowse.class);

    private Map<UUID, Integer> regionsCountCache = Collections.emptyMap();
    private Map<UUID, Integer> citiesCountCache = Collections.emptyMap();

    @Subscribe
    public void onInit(InitEvent event) {
        detailTitle.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailSubtitle.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailLocation.setAlignment(Component.Alignment.MIDDLE_CENTER);

        setupTableColumns();
        setupTableSelection();
        setupSidebarButtons();
    }

    private void setupTableColumns() {
        countriesTable.addGeneratedColumn("countryFlagColumn", country -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Image image = uiComponents.create(Image.class);
            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setWidth("28px");
            image.setHeight("20px");
            image.setStyleName("icon-no-border-20px");
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (country != null && country.getFileFlag() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(country.getFileFlag());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/dictionaries/country.png");
            }

            box.add(image);
            return box;
        });
    }

    private void setupTableSelection() {
        countriesTable.addSelectionListener(e -> {
            Set<Country> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                Country single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            Country selected = countriesTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(countriesTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });

        loadAllCountriesBtn.addClickListener(e -> onLoadAllCountriesBtnClick());
    }

    private void onLoadAllCountriesBtnClick() {
        loadAllCountriesBtn.setEnabled(false);
        notifications.create(Notifications.NotificationType.HUMANIZED)
                .withCaption("Загрузка стран")
                .withDescription("Начинаем загрузку всех государств мира. Это может занять 1-3 минуты (скачивание флагов).")
                .show();

        BackgroundTask<Long, String> task = new BackgroundTask<Long, String>(30 * 60, this) {
            @Override
            public String run(TaskLifeCycle<Long> taskLifeCycle) {
                return geoBulkLoaderService.loadAllCountries();
            }

            @Override
            public void done(String summary) {
                loadAllCountriesBtn.setEnabled(true);
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Загрузка стран завершена")
                        .withDescription(summary)
                        .show();
                try {
                    getScreenData().loadAll();
                } catch (Exception e) {
                    log.warn("Не удалось обновить таблицу стран: {}", e.getMessage());
                }
            }

            @Override
            public void canceled() {
                loadAllCountriesBtn.setEnabled(true);
            }
        };

        try {
            BackgroundTaskHandler<String> handler = backgroundWorker.handle(task);
            handler.execute();
        } catch (Exception e) {
            loadAllCountriesBtn.setEnabled(true);
            log.error("Ошибка запуска фоновой загрузки всех стран", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка загрузки стран")
                    .withDescription(e.getMessage())
                    .show();
        }
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        countriesDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = countriesTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(countriesTable);
        }
    }

    @Subscribe(id = "countriesDl", target = Target.DATA_LOADER)
    private void onCountriesDlPostLoad(CollectionLoader.PostLoadEvent<Country> event) {
        refreshStatsCaches(event.getLoadedEntities());

        Country current = countriesTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            countriesTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void refreshStatsCaches(List<Country> countries) {
        if (countries.isEmpty()) {
            regionsCountCache = Collections.emptyMap();
            citiesCountCache = Collections.emptyMap();
            return;
        }
        List<KeyValueEntity> regionRows = dataManager.loadValues(QUERY_REGIONS_COUNT_BY_COUNTRIES)
                .properties("country", "count")
                .parameter("countries", countries)
                .list();
        Map<UUID, Integer> regionsCache = new HashMap<>();
        for (KeyValueEntity row : regionRows) {
            Country country = row.getValue("country");
            Long count = row.getValue("count");
            if (country != null && country.getId() != null && count != null) {
                regionsCache.put(country.getId(), count.intValue());
            }
        }
        regionsCountCache = regionsCache;

        List<KeyValueEntity> cityRows = dataManager.loadValues(QUERY_CITIES_COUNT_BY_REGIONS)
                .properties("country", "count")
                .parameter("countries", countries)
                .list();
        Map<UUID, Integer> citiesCache = new HashMap<>();
        for (KeyValueEntity row : cityRows) {
            Country country = row.getValue("country");
            Long count = row.getValue("count");
            if (country != null && country.getId() != null && count != null) {
                citiesCache.put(country.getId(), count.intValue());
            }
        }
        citiesCountCache = citiesCache;
    }

    private void updateSidebarDetails(Country country) {
        openEditCardBtn.setEnabled(true);

        // Флаг в шапке профиля
        if (country.getFileFlag() != null) {
            logoPic.setSource(FileDescriptorResource.class).setFileDescriptor(country.getFileFlag());
        } else {
            logoPic.applyFallback();
        }

        detailTitle.setValue(html(country.getCountryRuName() != null ? country.getCountryRuName() : "Без названия"));
        detailSubtitle.setValue(html(country.getCountryEngName() != null ? country.getCountryEngName() : "-"));
        detailLocation.setValue(html(country.getCapital() != null ? "Столица: " + country.getCapital() : "-"));

        detailShortName.setValue(html(nvl(country.getCountryShortName())));
        detailAlpha3Code.setValue(html(nvl(country.getAlpha3Code())));
        detailNumericCode.setValue(html(nvl(country.getNumericCode())));
        detailCapital.setValue(html(nvl(country.getCapital())));
        detailCurrencyCode.setValue(html(nvl(country.getCurrencyCode())));
        detailPhoneCode.setValue(html(country.getPhoneCode() != null ? String.valueOf(country.getPhoneCode()) : "-"));
        detailEngName.setValue(html(nvl(country.getCountryEngName())));

        int regionsCount = country.getId() != null ? regionsCountCache.getOrDefault(country.getId(), 0) : 0;
        detailRegionsCount.setValue(html(String.valueOf(regionsCount)));
        int citiesCount = country.getId() != null ? citiesCountCache.getOrDefault(country.getId(), 0) : 0;
        detailCitiesCount.setValue(html(String.valueOf(citiesCount)));

        // Флаг в секции «ФЛАГ СТРАНЫ»
        if (country.getFileFlag() != null) {
            detailFlagImage.setSource(FileDescriptorResource.class).setFileDescriptor(country.getFileFlag());
        } else {
            detailFlagImage.applyFallback();
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        logoPic.applyFallback();
        detailFlagImage.applyFallback();
        detailTitle.setValue(html("Выберите страну"));
        detailSubtitle.setValue(html("-"));
        detailLocation.setValue(html("-"));
        detailShortName.setValue(html("-"));
        detailAlpha3Code.setValue(html("-"));
        detailNumericCode.setValue(html("-"));
        detailCapital.setValue(html("-"));
        detailCurrencyCode.setValue(html("-"));
        detailPhoneCode.setValue(html("-"));
        detailEngName.setValue(html("-"));
        detailRegionsCount.setValue(html("0"));
        detailCitiesCount.setValue(html("0"));
    }

    private String html(String value) {
        return "<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + value + "</div>";
    }

    private String nvl(String value) {
        return value != null ? value : "-";
    }
}
