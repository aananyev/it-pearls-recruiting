package com.company.itpearls.web.screens.recrutiestasks;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RecrutiesTasks;

@UiController("itpearls_RecrutiesTasks.edit")
@UiDescriptor("recruties-tasks-edit.xml")
@EditedEntityContainer("recrutiesTasksDc")
@LoadDataBeforeShow
public class RecrutiesTasksEdit extends StandardEditor<RecrutiesTasks> {
}