package com.company.hunttech.web.screens.city;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.City;

@UiController("hunttech_City.browse")
@UiDescriptor("city-browse.xml")
@LookupComponent("citiesTable")
@LoadDataBeforeShow
public class CityBrowse extends StandardLookup<City> {
}