package com.company.itpearls.web.screens.project;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;

@UiController("itpearls_Project.edit")
@UiDescriptor("project-edit.xml")
@EditedEntityContainer("projectDc")
@LoadDataBeforeShow
public class ProjectEdit extends StandardEditor<Project> {
}