package com.company.hunttech.web.screens.currency;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Currency;

@UiController("hunttech_Currency.browse")
@UiDescriptor("currency-browse.xml")
@LookupComponent("currenciesTable")
@LoadDataBeforeShow
public class CurrencyBrowse extends StandardLookup<Currency> {
}