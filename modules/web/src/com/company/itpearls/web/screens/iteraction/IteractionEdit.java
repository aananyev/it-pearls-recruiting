package com.company.itpearls.web.screens.iteraction;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

import javax.inject.Inject;

@UiController("itpearls_Iteraction.edit")
@UiDescriptor("iteraction-edit.xml")
@EditedEntityContainer("iteractionDc")
@LoadDataBeforeShow
public class IteractionEdit extends StandardEditor<Iteraction> {
    @Inject
    private LookupPickerField<Iteraction> iteractionTreeField;
    @Inject
    private TextField<String> iterationNameField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
       if(PersistenceHelper.isNew(getEditedEntity())) {
           getEditedEntity().setMandatoryIteraction(false);
       }
    }

    @Subscribe("iteractionCheckBoxMandatory")
    public void onIteractionCheckBoxMandatoryValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (getEditedEntity().getMandatoryIteraction()) {
            iteractionTreeField.setEditable(false);
            iterationNameField.setEditable(false);
        } else {
            iteractionTreeField.setEditable(true);
            iterationNameField.setEditable(true);
        }
    }
}