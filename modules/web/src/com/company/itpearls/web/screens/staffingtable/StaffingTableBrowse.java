package com.company.itpearls.web.screens.staffingtable;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.StaffingTable;

@UiController("itpearls_StaffingTable.browse")
@UiDescriptor("staffing-table-browse.xml")
@LookupComponent("staffingTablesTable")
@LoadDataBeforeShow
public class StaffingTableBrowse extends StandardLookup<StaffingTable> {
    @Install(to = "staffingTablesTable", subject = "styleProvider")
    private String staffingTablesTableStyleProvider(StaffingTable entity, String property) {
        return "table-wordwrap";
    }
}