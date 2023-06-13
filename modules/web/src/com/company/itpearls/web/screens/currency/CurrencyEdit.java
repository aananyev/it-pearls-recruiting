package com.company.itpearls.web.screens.currency;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Currency;

@UiController("itpearls_Currency.edit")
@UiDescriptor("currency-edit.xml")
@EditedEntityContainer("currencyDc")
@LoadDataBeforeShow
public class CurrencyEdit extends StandardEditor<Currency> {
}