package com.company.hunttech.web.screens.staffingtable;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.StaffingTable;

@UiController("hunttech_StaffingTable.browse")
@UiDescriptor("staffing-table-browse.xml")
@LookupComponent("staffingTablesTable")
@LoadDataBeforeShow
public class StaffingTableBrowse extends StandardLookup<StaffingTable> {
    @Install(to = "staffingTablesTable", subject = "styleProvider")
    private String staffingTablesTableStyleProvider(StaffingTable entity, String property) {
        return "table-wordwrap";
    }
}