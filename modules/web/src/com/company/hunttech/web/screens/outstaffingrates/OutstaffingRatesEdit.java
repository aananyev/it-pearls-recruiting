package com.company.hunttech.web.screens.outstaffingrates;

import com.company.hunttech.entity.OutstaffingRates;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_OutstaffingRates.edit")
@UiDescriptor("outstaffing-rates-edit.xml")
@EditedEntityContainer("outstaffingRatesDc")
@LoadDataBeforeShow
public class OutstaffingRatesEdit extends StandardEditor<OutstaffingRates> {

    @Inject
    private TextField<Object> rateField;
    @Inject
    private TextArea<String> commentField;
    @Inject
    private Button ratesNav;
    @Inject
    private Button commentNav;

    /**
     * Презентационная навигация: переводит фокус к первому полю карточки «Ставки»
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusRatesSection() {
        rateField.focus();
        setActiveNavigation(ratesNav, commentNav);
    }

    /**
     * Презентационная навигация: переводит фокус к комментарию ступени тарифной шкалы
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusCommentSection() {
        commentField.focus();
        setActiveNavigation(commentNav, ratesNav);
    }

    private void setActiveNavigation(Button activeButton, Button inactiveButton) {
        activeButton.addStyleName("label-nav-item-active");
        inactiveButton.removeStyleName("label-nav-item-active");
    }
}
