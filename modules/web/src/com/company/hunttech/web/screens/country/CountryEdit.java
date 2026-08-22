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
