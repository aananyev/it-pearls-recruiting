package com.company.itpearls.web.screens.country;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Country;

@UiController("itpearls_Country.browse")
@UiDescriptor("country-browse.xml")
@LookupComponent("countriesTable")
@LoadDataBeforeShow
public class CountryBrowse extends StandardLookup<Country> {
}