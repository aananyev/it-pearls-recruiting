package com.company.hunttech.web.screens.currency;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Currency;

@UiController("hunttech_Currency.edit")
@UiDescriptor("currency-edit.xml")
@EditedEntityContainer("currencyDc")
@LoadDataBeforeShow
public class CurrencyEdit extends StandardEditor<Currency> {
}