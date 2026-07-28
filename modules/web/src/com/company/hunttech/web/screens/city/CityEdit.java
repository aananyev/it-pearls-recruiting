package com.company.hunttech.web.screens.city;

import com.company.hunttech.entity.City;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_City.edit")
@UiDescriptor("city-edit.xml")
@EditedEntityContainer("cityDc")
@LoadDataBeforeShow
public class CityEdit extends StandardEditor<City> {

    @Inject
    private TextField<String> cityRuNameField;

    /**
     * Переводит фокус к основным реквизитам города без изменения данных и lifecycle editor-а.
     */
    public void focusMainSection() {
        cityRuNameField.focus();
    }
}
