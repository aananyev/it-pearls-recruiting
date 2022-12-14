package com.company.itpearls.web.screens.staffcurrent;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.StaffCurrent;

@UiController("itpearls_StaffCurrent.edit")
@UiDescriptor("staff-current-edit.xml")
@EditedEntityContainer("staffCurrentDc")
@LoadDataBeforeShow
public class StaffCurrentEdit extends StandardEditor<StaffCurrent> {
}