package com.company.hunttech.web.screens.city;

import com.company.hunttech.entity.City;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.util.*;

@UiController("hunttech_CityReestr.browse")
@UiDescriptor("city-reestr-browse.xml")
@LookupComponent("citiesTable")
@LoadDataBeforeShow
public class CityReestrBrowse extends StandardLookup<City> {

    @Inject
    private CollectionContainer<City> citiesDc;
    @Inject
    private CollectionLoader<City> citiesDl;
    @Inject
    private GroupTable<City> citiesTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;

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
    private Label<String> detailRegion;
    @Inject
    private Label<String> detailCountry;
    @Inject
    private Label<String> detailPhoneCode;
    @Inject
    private Label<String> detailPostalCode;
    @Inject
    private Label<String> detailPopulation;
    @Inject
    private Label<String> detailTimeZone;
    @Inject
    private Label<String> detailCoordinates;

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
        citiesTable.addGeneratedColumn("cityEmblemColumn", city -> {
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

            if (city != null && city.getFileCityEmblem() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(city.getFileCityEmblem());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/dictionaries/city.png");
            }

            box.add(image);
            return box;
        });
    }

    private void setupTableSelection() {
        citiesTable.addSelectionListener(e -> {
            Set<City> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                City single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            City selected = citiesTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(citiesTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        citiesDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = citiesTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(citiesTable);
        }
    }

    @Subscribe(id = "citiesDl", target = Target.DATA_LOADER)
    private void onCitiesDlPostLoad(CollectionLoader.PostLoadEvent<City> event) {
        City current = citiesTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            citiesTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void updateSidebarDetails(City city) {
        openEditCardBtn.setEnabled(true);

        // Герб в шапке профиля
        if (city.getFileCityEmblem() != null) {
            logoPic.setSource(FileDescriptorResource.class).setFileDescriptor(city.getFileCityEmblem());
        } else {
            logoPic.applyFallback();
        }

        detailTitle.setValue(city.getCityRuName() != null ? city.getCityRuName() : "Без названия");
        detailSubtitle.setValue(city.getCityEngName() != null ? city.getCityEngName() : "-");
        detailLocation.setValue(city.getCityRegion() != null && city.getCityRegion().getRegionRuName() != null
                ? "Регион: " + city.getCityRegion().getRegionRuName() : "-");

        detailRegion.setValue(city.getCityRegion() != null && city.getCityRegion().getRegionRuName() != null
                ? city.getCityRegion().getRegionRuName() : "-");
        detailCountry.setValue(city.getCityRegion() != null && city.getCityRegion().getRegionCountry() != null
                && city.getCityRegion().getRegionCountry().getCountryRuName() != null
                ? city.getCityRegion().getRegionCountry().getCountryRuName() : "-");
        detailPhoneCode.setValue(nvl(city.getCityPhoneCode()));
        detailPostalCode.setValue(nvl(city.getPostalCode()));
        detailPopulation.setValue(city.getPopulation() != null ? String.valueOf(city.getPopulation()) : "-");
        detailTimeZone.setValue(nvl(city.getTimeZone()));
        detailCoordinates.setValue(city.getLatitude() != null && city.getLongitude() != null
                ? String.format("%.4f, %.4f", city.getLatitude(), city.getLongitude()) : "-");

        // Герб в секции «ГЕРБ ГОРОДА»
        if (city.getFileCityEmblem() != null) {
            detailEmblemImage.setSource(FileDescriptorResource.class).setFileDescriptor(city.getFileCityEmblem());
        } else {
            detailEmblemImage.applyFallback();
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        logoPic.applyFallback();
        detailEmblemImage.applyFallback();
        detailTitle.setValue("Выберите город");
        detailSubtitle.setValue("-");
        detailLocation.setValue("-");
        detailRegion.setValue("-");
        detailCountry.setValue("-");
        detailPhoneCode.setValue("-");
        detailPostalCode.setValue("-");
        detailPopulation.setValue("-");
        detailTimeZone.setValue("-");
        detailCoordinates.setValue("-");
    }

    private String nvl(String value) {
        return value != null ? value : "-";
    }
}
