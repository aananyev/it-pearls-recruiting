package com.company.itpearls.web.screens.staffingtable;

import com.haulmont.bpm.entity.ProcAttachment;
import com.haulmont.bpm.gui.procactionsfragment.ProcActionsFragment;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.StaffingTable;

import javax.inject.Inject;
import java.util.UUID;

@UiController("itpearls_StaffingTable.edit")
@UiDescriptor("staffing-table-edit.xml")
@EditedEntityContainer("staffingTableDc")
@LoadDataBeforeShow
public class StaffingTableEdit extends StandardEditor<StaffingTable> {
    @Inject
    private CollectionLoader<ProcAttachment> procAttachmentsDl;
    @Inject
    private ProcActionsFragment procActionsFragment;

    private static final String PROCESS_CODE = "staffingTableApproval";

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setInitApprovalProcess();
    }


    private void setInitApprovalProcess() {
        UUID entityId = getEditedEntity().getId();
        procAttachmentsDl.setParameter("entityId",entityId);
        procAttachmentsDl.load();
        procActionsFragment.initializer()
                .standard()
                .init(PROCESS_CODE, getEditedEntity());
    }
}