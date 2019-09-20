package com.company.itpearls.web.screens.specialisation;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Specialisation;

@UiController("itpearls_Specialisation.browse")
@UiDescriptor("specialisation-browse.xml")
@LookupComponent("specialisationsTable")
@LoadDataBeforeShow
public class SpecialisationBrowse extends StandardLookup<Specialisation> {
}