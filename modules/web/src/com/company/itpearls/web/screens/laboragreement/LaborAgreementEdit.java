package com.company.itpearls.web.screens.laboragreement;

import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgreement;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_LaborAgreement.edit")
@UiDescriptor("labor-agreement-edit.xml")
@EditedEntityContainer("laborAgreementDc")
@LoadDataBeforeShow
public class LaborAgreementEdit extends StandardEditor<LaborAgreement> {
    @Inject
    private CheckBox parpetualAgreementCheckBox;
    @Inject
    private DateField<Date> laborAgreementEndDateField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if(parpetualAgreementCheckBox.getValue() != null) {
                laborAgreementEndDateField.setEnabled(!parpetualAgreementCheckBox.getValue());
        }
    }

    @Subscribe
    public void onInit(InitEvent event) {
        parpetualAgreementCheckBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                laborAgreementEndDateField.setEnabled(!parpetualAgreementCheckBox.getValue());
            }
        });
    }
}