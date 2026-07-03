package com.company.hunttech.web.screens.grade;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Grade;

@UiController("hunttech_Grade.browse")
@UiDescriptor("grade-browse.xml")
@LookupComponent("gradesTable")
@LoadDataBeforeShow
public class GradeBrowse extends StandardLookup<Grade> {
}