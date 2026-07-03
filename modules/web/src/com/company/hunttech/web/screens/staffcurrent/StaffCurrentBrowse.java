package com.company.hunttech.web.screens.staffcurrent;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.StaffCurrent;

@UiController("hunttech_StaffCurrent.browse")
@UiDescriptor("staff-current-browse.xml")
@LookupComponent("staffCurrentsTable")
@LoadDataBeforeShow
public class StaffCurrentBrowse extends StandardLookup<StaffCurrent> {
    @Install(to = "staffCurrentsTable", subject = "styleProvider")
    private String staffCurrentsTableStyleProvider(StaffCurrent entity, String property) {
        return "table-wordwrap";
    }
}