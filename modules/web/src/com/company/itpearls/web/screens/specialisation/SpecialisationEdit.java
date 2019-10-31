package com.company.itpearls.web.screens.specialisation;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Specialisation;

@UiController("itpearls_Specialisation.edit")
@UiDescriptor("specialisation-edit.xml")
@EditedEntityContainer("specialisationDc")
@LoadDataBeforeShow
public class SpecialisationEdit extends StandardEditor<Specialisation> {
}