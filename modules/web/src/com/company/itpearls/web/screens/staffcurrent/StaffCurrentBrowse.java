package com.company.itpearls.web.screens.staffcurrent;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.StaffCurrent;

@UiController("itpearls_StaffCurrent.browse")
@UiDescriptor("staff-current-browse.xml")
@LookupComponent("staffCurrentsTable")
@LoadDataBeforeShow
public class StaffCurrentBrowse extends StandardLookup<StaffCurrent> {
}