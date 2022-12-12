package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.StaffInteractionStatus;
import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.LaborAgreement;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.PickerField;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("staff-itpearls_IteractionList.edit")
@UiDescriptor("staff-iteraction-list-edit.xml")
public class StaffIteractionListEdit extends IteractionListEdit {
    @Inject
    private LookupPickerField<LaborAgreement> laborAgreementLookupPickerField;

    @Subscribe("iteractionTypeField")
    public void onIteractionTypeFieldFieldValueChange(PickerField.FieldValueChangeEvent<Iteraction> event) {
        if (event.getSource().getValue().getStaffInteractionStatus() != null) {
            if (event.getSource().getValue().getStaffInteractionStatus() != StaffInteractionStatus.NOTHING) {
                laborAgreementLookupPickerField.setEnabled(true);
            } else {
                laborAgreementLookupPickerField.setEnabled(false);
            }
        }

    }
}