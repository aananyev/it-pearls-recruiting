package com.company.hunttech.web.screens.country;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Country;

@UiController("hunttech_Country.browse")
@UiDescriptor("country-browse.xml")
@LookupComponent("countriesTable")
@LoadDataBeforeShow
public class CountryBrowse extends StandardLookup<Country> {
}