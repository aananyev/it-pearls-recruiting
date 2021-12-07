package com.company.itpearls.web.screens.laboragreement;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgreement;

@UiController("itpearls_LaborAgreement.browse")
@UiDescriptor("labor-agreement-browse.xml")
@LookupComponent("laborAgreementsTable")
@LoadDataBeforeShow
public class LaborAgreementBrowse extends StandardLookup<LaborAgreement> {
}