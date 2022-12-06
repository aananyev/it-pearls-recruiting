package com.company.itpearls.web.screens.grade;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Grade;

@UiController("itpearls_Grade.browse")
@UiDescriptor("grade-browse.xml")
@LookupComponent("gradesTable")
@LoadDataBeforeShow
public class GradeBrowse extends StandardLookup<Grade> {
}