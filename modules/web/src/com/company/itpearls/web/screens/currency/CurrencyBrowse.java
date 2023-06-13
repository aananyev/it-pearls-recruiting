package com.company.itpearls.web.screens.currency;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Currency;

@UiController("itpearls_Currency.browse")
@UiDescriptor("currency-browse.xml")
@LookupComponent("currenciesTable")
@LoadDataBeforeShow
public class CurrencyBrowse extends StandardLookup<Currency> {
}