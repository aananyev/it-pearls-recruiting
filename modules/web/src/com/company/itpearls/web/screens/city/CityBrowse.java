package com.company.itpearls.web.screens.city;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.City;

@UiController("itpearls_City.browse")
@UiDescriptor("city-browse.xml")
@LookupComponent("citiesTable")
@LoadDataBeforeShow
public class CityBrowse extends StandardLookup<City> {
}