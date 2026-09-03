package com.company.hunttech.web.screens.region;

import com.company.hunttech.entity.Region;
import com.company.hunttech.service.GeoBulkLoaderService;
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

@UiController("hunttech_RegionReestr.browse")
@UiDescriptor("region-reestr-browse.xml")
@LookupComponent("regionsTable")
@LoadDataBeforeShow
public class RegionReestrBrowse extends StandardLookup<Region> {

    private static final String QUERY_CITIES_COUNT_BY_REGIONS =
            "select c.cityRegion, count(c) from hunttech_City c "
                    + "where c.cityRegion in :regions group by c.cityRegion";

    @Inject
    private CollectionContainer<Region> regionsDc;
    @Inject
    private CollectionLoader<Region> regionsDl;
    @Inject
    private GroupTable<Region> regionsTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataManager dataManager;

    @Inject
    private OvaFallbackImage logoPic;
    @Inject
    private OvaFallbackImage detailEmblemImage;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailLocation;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Label<String> detailCountry;
    @Inject
    private Label<String> detailRegionCode;
    @Inject
    private Label<String> detailIsoCode;
    @Inject
    private Label<String> detailRegionType;
    @Inject
    private Label<String> detailCapital;
    @Inject
    private Label<String> detailFiasId;
    @Inject
    private Label<String> detailTimeZone;
    @Inject
    private Label<String> detailCitiesCount;
    @Inject
    private Button loadRegionsRussiaBtn;

    @Inject
    private GeoBulkLoaderService geoBulkLoaderService;
    @Inject
    private Notifications notifications;
    @Inject
    private BackgroundWorker backgroundWorker;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegionReestrBrowse.class);

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
        regionsTable.addGeneratedColumn("regionEmblemColumn", region -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Image image = uiComponents.create(Image.class);
            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("24px");
            image.setHeight("24px");
            image.setStyleName("icon-no-border-20px");
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (region != null && region.getFileRegionEmblem() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(region.getFileRegionEmblem());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/dictionaries/region.png");
            }

            box.add(image);
            return box;
        });
    }

    private void setupTableSelection() {
        regionsTable.addSelectionListener(e -> {
            Set<Region> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                Region single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            Region selected = regionsTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(regionsTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });

        loadRegionsRussiaBtn.addClickListener(e -> onLoadRegionsRussiaBtnClick());
    }

    private void onLoadRegionsRussiaBtnClick() {
        loadRegionsRussiaBtn.setEnabled(false);
        notifications.create(Notifications.NotificationType.HUMANIZED)
                .withCaption("Загрузка регионов России")
                .withDescription("Начинаем загрузку всех субъектов РФ. Это может занять 1-3 минуты (скачивание гербов).")
                .show();

        BackgroundTask<Long, String> task = new BackgroundTask<Long, String>(30 * 60, this) {
            @Override
            public String run(TaskLifeCycle<Long> taskLifeCycle) {
                return geoBulkLoaderService.loadAllRegionsForRussia();
            }

            @Override
            public void done(String summary) {
                loadRegionsRussiaBtn.setEnabled(true);
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Загрузка регионов завершена")
                        .withDescription(summary)
                        .show();
                try {
                    getScreenData().loadAll();
                } catch (Exception ex) {
                    log.warn("Не удалось обновить таблицу регионов: {}", ex.getMessage());
                }
            }

            @Override
            public void canceled() {
                loadRegionsRussiaBtn.setEnabled(true);
            }
        };

        try {
            BackgroundTaskHandler<String> handler = backgroundWorker.handle(task);
            handler.execute();
        } catch (Exception ex) {
            loadRegionsRussiaBtn.setEnabled(true);
            log.error("Ошибка запуска фоновой загрузки регионов", ex);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка загрузки регионов")
                    .withDescription(ex.getMessage())
                    .show();
        }
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        regionsDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = regionsTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(regionsTable);
        }
    }

    @Subscribe(id = "regionsDl", target = Target.DATA_LOADER)
    private void onRegionsDlPostLoad(CollectionLoader.PostLoadEvent<Region> event) {
        refreshCitiesCountCache(event.getLoadedEntities());

        Region current = regionsTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            regionsTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void refreshCitiesCountCache(List<Region> regions) {
        if (regions.isEmpty()) {
            citiesCountCache = Collections.emptyMap();
            return;
        }
        List<KeyValueEntity> cityRows = dataManager.loadValues(QUERY_CITIES_COUNT_BY_REGIONS)
                .properties("region", "count")
                .parameter("regions", regions)
                .list();
        Map<UUID, Integer> citiesCache = new HashMap<>();
        for (KeyValueEntity row : cityRows) {
            Region region = row.getValue("region");
            Long count = row.getValue("count");
            if (region != null && region.getId() != null && count != null) {
                citiesCache.put(region.getId(), count.intValue());
            }
        }
        citiesCountCache = citiesCache;
    }

    private void updateSidebarDetails(Region region) {
        openEditCardBtn.setEnabled(true);

        // Герб в шапке профиля
        if (region.getFileRegionEmblem() != null) {
            logoPic.setSource(FileDescriptorResource.class).setFileDescriptor(region.getFileRegionEmblem());
        } else {
            logoPic.applyFallback();
        }

        detailTitle.setValue(region.getRegionRuName() != null ? region.getRegionRuName() : "Без названия");
        detailSubtitle.setValue(region.getRegionEngName() != null ? region.getRegionEngName() : "-");
        detailLocation.setValue(region.getRegionCountry() != null && region.getRegionCountry().getCountryRuName() != null
                ? "Страна: " + region.getRegionCountry().getCountryRuName() : "-");

        detailCountry.setValue(region.getRegionCountry() != null && region.getRegionCountry().getCountryRuName() != null
                ? region.getRegionCountry().getCountryRuName() : "-");
        detailRegionCode.setValue(nvl(region.getRegionCode() != null ? String.valueOf(region.getRegionCode()) : null));
        detailIsoCode.setValue(nvl(region.getIsoCode()));
        detailRegionType.setValue(nvl(region.getRegionType()));
        detailCapital.setValue(nvl(region.getCapital()));
        detailFiasId.setValue(nvl(region.getFiasId()));
        detailTimeZone.setValue(nvl(region.getTimeZone()));

        int citiesCount = region.getId() != null ? citiesCountCache.getOrDefault(region.getId(), 0) : 0;
        detailCitiesCount.setValue(String.valueOf(citiesCount));

        // Герб в секции «ГЕРБ РЕГИОНА»
        if (region.getFileRegionEmblem() != null) {
            detailEmblemImage.setSource(FileDescriptorResource.class).setFileDescriptor(region.getFileRegionEmblem());
        } else {
            detailEmblemImage.applyFallback();
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        logoPic.applyFallback();
        detailEmblemImage.applyFallback();
        detailTitle.setValue("Выберите регион");
        detailSubtitle.setValue("-");
        detailLocation.setValue("-");
        detailCountry.setValue("-");
        detailRegionCode.setValue("-");
        detailIsoCode.setValue("-");
        detailRegionType.setValue("-");
        detailCapital.setValue("-");
        detailFiasId.setValue("-");
        detailTimeZone.setValue("-");
        detailCitiesCount.setValue("0");
    }

    private String nvl(String value) {
        return value != null ? value : "-";
    }
}