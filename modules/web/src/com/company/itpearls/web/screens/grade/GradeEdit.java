package com.company.itpearls.web.screens.grade;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Grade;

@UiController("itpearls_Grade.edit")
@UiDescriptor("grade-edit.xml")
@EditedEntityContainer("gradeDc")
@LoadDataBeforeShow
public class GradeEdit extends StandardEditor<Grade> {
}