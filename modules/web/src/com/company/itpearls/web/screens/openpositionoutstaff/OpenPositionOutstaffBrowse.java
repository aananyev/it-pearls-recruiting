package com.company.itpearls.web.screens.openpositionoutstaff;

import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.StaffingTable;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TreeDataGrid;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.openposition.OpenPositionBrowse;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionOutstaff.browse")
@UiDescriptor("open-position-outstaff-browse.xml")
public class OpenPositionOutstaffBrowse extends OpenPositionBrowse {
    @Inject
    private Metadata metadata;
    @Inject
    private TreeDataGrid<OpenPosition> openPositionsTable;
    @Inject
    private DataManager dataManager;
    @Inject
    private Button addToStaffTableButton;

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        addAddToStaffTableButtonEnable();
    }

    private void addAddToStaffTableButtonEnable() {
        openPositionsTable.addSelectionListener(e -> {
                addToStaffTableButton.setEnabled(openPositionsTable.getSingleSelected() != null);
        });
    }

    public void attToStaffTableButtonInvoke() {
        StaffingTable staffingTable = metadata.create(StaffingTable.class);

        staffingTable.setActive(true);
        staffingTable.setNumberOfStaff(openPositionsTable.getSingleSelected().getNumberPosition());
        staffingTable.setSalaryMin(openPositionsTable.getSingleSelected().getSalaryMin());
        staffingTable.setSalaryMax(openPositionsTable.getSingleSelected().getSalaryMax());
        staffingTable.setOpenPosition(openPositionsTable.getSingleSelected());

//        staffingTableDc.getItems().add(staffingTable);

        CommitContext commitContext = new CommitContext();
        commitContext.addInstanceToCommit(staffingTable);
        dataManager.commit(commitContext);
    }
}