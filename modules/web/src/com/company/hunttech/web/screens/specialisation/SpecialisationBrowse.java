package com.company.hunttech.web.screens.specialisation;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Specialisation;

@UiController("hunttech_Specialisation.browse")
@UiDescriptor("specialisation-browse.xml")
@LookupComponent("specialisationsTable")
@LoadDataBeforeShow
public class SpecialisationBrowse extends StandardLookup<Specialisation> {
}