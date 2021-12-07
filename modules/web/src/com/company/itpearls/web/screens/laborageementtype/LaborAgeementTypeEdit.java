package com.company.itpearls.web.screens.laborageementtype;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgeementType;

@UiController("itpearls_LaborAgeementType.edit")
@UiDescriptor("labor-ageement-type-edit.xml")
@EditedEntityContainer("laborAgeementTypeDc")
@LoadDataBeforeShow
public class LaborAgeementTypeEdit extends StandardEditor<LaborAgeementType> {
}