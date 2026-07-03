package com.company.hunttech.web.screens.country;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Country;

@UiController("hunttech_Country.edit")
@UiDescriptor("country-edit.xml")
@EditedEntityContainer("countryDc")
@LoadDataBeforeShow
public class CountryEdit extends StandardEditor<Country> {
}