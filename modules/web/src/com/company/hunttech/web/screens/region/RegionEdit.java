package com.company.hunttech.web.screens.region;

import com.company.hunttech.entity.Region;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_Region.edit")
@UiDescriptor("region-edit.xml")
@EditedEntityContainer("regionDc")
@LoadDataBeforeShow
public class RegionEdit extends StandardEditor<Region> {

    @Inject
    private TextField<String> regionRuNameField;
    @Inject
    private Table<?> regionRegionOfCityTable;
    @Inject
    private Button regionMainNav;
    @Inject
    private Button regionCitiesNav;

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
