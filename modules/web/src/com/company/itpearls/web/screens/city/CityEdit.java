package com.company.itpearls.web.screens.city;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.City;

@UiController("itpearls_City.edit")
@UiDescriptor("city-edit.xml")
@EditedEntityContainer("cityDc")
@LoadDataBeforeShow
public class CityEdit extends StandardEditor<City> {
}