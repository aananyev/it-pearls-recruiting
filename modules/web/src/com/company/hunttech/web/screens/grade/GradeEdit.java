package com.company.hunttech.web.screens.grade;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Grade;

@UiController("hunttech_Grade.edit")
@UiDescriptor("grade-edit.xml")
@EditedEntityContainer("gradeDc")
@LoadDataBeforeShow
public class GradeEdit extends StandardEditor<Grade> {
}