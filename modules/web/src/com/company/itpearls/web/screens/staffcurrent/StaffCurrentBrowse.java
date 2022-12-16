package com.company.itpearls.web.screens.staffcurrent;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.StaffCurrent;

@UiController("itpearls_StaffCurrent.browse")
@UiDescriptor("staff-current-browse.xml")
@LookupComponent("staffCurrentsTable")
@LoadDataBeforeShow
public class StaffCurrentBrowse extends StandardLookup<StaffCurrent> {
    @Install(to = "staffCurrentsTable", subject = "styleProvider")
    private String staffCurrentsTableStyleProvider(StaffCurrent entity, String property) {
        return "table-wordwrap";
    }
}