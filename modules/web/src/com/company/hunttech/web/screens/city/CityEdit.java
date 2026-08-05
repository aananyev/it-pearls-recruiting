package com.company.hunttech.web.screens.city;

import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Region;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.LookupPickerField;
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

    private static final String ACTIVE_NAV_STYLE = "label-nav-item-active";

    @Inject
    private TextField<String> cityRuNameField;

    @Inject
    private LookupPickerField<Region> cityRegionField;

    @Inject
    private Button cityIdentityNav;

    @Inject
    private Button cityRegionNav;

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
