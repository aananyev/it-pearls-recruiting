package com.company.itpearls.web.screens.laboragreement;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgreement;

@UiController("itpearls_LaborAgreement.edit")
@UiDescriptor("labor-agreement-edit.xml")
@EditedEntityContainer("laborAgreementDc")
@LoadDataBeforeShow
public class LaborAgreementEdit extends StandardEditor<LaborAgreement> {
}