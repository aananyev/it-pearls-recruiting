package com.company.hunttech.web.screens.city;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.City;

@UiController("hunttech_City.edit")
@UiDescriptor("city-edit.xml")
@EditedEntityContainer("cityDc")
@LoadDataBeforeShow
public class CityEdit extends StandardEditor<City> {
}