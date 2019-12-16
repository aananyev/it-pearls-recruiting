package com.company.itpearls.web.screens.recrutiestasks;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RecrutiesTasks;

@UiController("itpearls_RecrutiesTasks.browse")
@UiDescriptor("recruties-tasks-browse.xml")
@LookupComponent("recrutiesTasksesTable")
@LoadDataBeforeShow
public class RecrutiesTasksBrowse extends StandardLookup<RecrutiesTasks> {
}