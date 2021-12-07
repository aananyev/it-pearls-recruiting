package com.company.itpearls.web.screens.laborageementtype;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgeementType;

@UiController("itpearls_LaborAgeementType.browse")
@UiDescriptor("labor-ageement-type-browse.xml")
@LookupComponent("laborAgeementTypesTable")
@LoadDataBeforeShow
public class LaborAgeementTypeBrowse extends StandardLookup<LaborAgeementType> {
}