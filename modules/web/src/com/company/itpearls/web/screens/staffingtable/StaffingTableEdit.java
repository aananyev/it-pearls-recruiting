package com.company.itpearls.web.screens.staffingtable;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.StaffingTable;

@UiController("itpearls_StaffingTable.edit")
@UiDescriptor("staffing-table-edit.xml")
@EditedEntityContainer("staffingTableDc")
@LoadDataBeforeShow
public class StaffingTableEdit extends StandardEditor<StaffingTable> {
}