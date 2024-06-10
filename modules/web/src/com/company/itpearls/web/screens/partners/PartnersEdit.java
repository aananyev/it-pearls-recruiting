package com.company.itpearls.web.screens.partners;

import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.company.CompanyEdit;

import javax.inject.Inject;

@UiController("itpearls_Partners.edit")
@UiDescriptor("partners-edit.xml")
public class PartnersEdit extends CompanyEdit {
    @Inject
    private CheckBox partnersCheckBox;

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        partnersCheckBox.setValue(true);
    }
}