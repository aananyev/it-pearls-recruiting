package com.company.itpearls.web.screens.country;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Country;

@UiController("itpearls_Country.edit")
@UiDescriptor("country-edit.xml")
@EditedEntityContainer("countryDc")
@LoadDataBeforeShow
public class CountryEdit extends StandardEditor<Country> {
}