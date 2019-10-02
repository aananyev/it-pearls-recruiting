package com.company.itpearls.web.screens.project;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;

@UiController("itpearls_Project.browse")
@UiDescriptor("project-browse.xml")
@LookupComponent("projectsTable")
@LoadDataBeforeShow
public class ProjectBrowse extends StandardLookup<Project> {
}