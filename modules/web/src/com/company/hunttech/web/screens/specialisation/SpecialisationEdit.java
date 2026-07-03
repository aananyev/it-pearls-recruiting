package com.company.hunttech.web.screens.specialisation;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Specialisation;

@UiController("hunttech_Specialisation.edit")
@UiDescriptor("specialisation-edit.xml")
@EditedEntityContainer("specialisationDc")
@LoadDataBeforeShow
public class SpecialisationEdit extends StandardEditor<Specialisation> {
}