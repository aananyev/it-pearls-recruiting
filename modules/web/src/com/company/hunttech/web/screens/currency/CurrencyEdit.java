package com.company.hunttech.web.screens.currency;

import com.company.hunttech.entity.Currency;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_Currency.edit")
@UiDescriptor("currency-edit.xml")
@EditedEntityContainer("currencyDc")
@LoadDataBeforeShow
public class CurrencyEdit extends StandardEditor<Currency> {

    @Inject
    private TextField<String> currencyLongNameField;
    @Inject
    private Button mainNav;

    /**
     * Презентационная навигация: переводит фокус к первому полю «Основных данных»
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        currencyLongNameField.focus();
        setActiveNavigation(mainNav);
    }

    private void setActiveNavigation(Button activeButton) {
        activeButton.addStyleName("label-nav-item-active");
    }
}
