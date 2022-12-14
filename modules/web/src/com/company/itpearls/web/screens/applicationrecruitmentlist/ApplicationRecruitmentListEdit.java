package com.company.itpearls.web.screens.applicationrecruitmentlist;

import com.company.itpearls.entity.*;
import com.haulmont.bpm.entity.ProcAttachment;
import com.haulmont.bpm.gui.procactionsfragment.ProcActionsFragment;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.app.core.file.FileDownloadHelper;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.PickerField;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@UiController("itpearls_ApplicationRecruitmentList.edit")
@UiDescriptor("application-recruitment-list-edit.xml")
@EditedEntityContainer("applicationRecruitmentListDc")
@LoadDataBeforeShow
public class ApplicationRecruitmentListEdit extends StandardEditor<ApplicationRecruitmentList> {
    @Inject
    private DateField<Date> openDateField;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private InstanceContainer<ApplicationRecruitmentList> applicationRecruitmentListDc;
    @Inject
    private Table<ApplicationRecruitment> applicationRecruitmentTable;
    @Inject
    private DataContext dataContext;
    @Inject
    private CollectionPropertyContainer<ApplicationRecruitment> applicationRecruitmentDc;
    @Inject
    private PickerField<ExtUser> recruiterField;
    @Inject
    private UserSession userSession;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private InstanceLoader<ApplicationRecruitmentList> applicationRecruitmentListDl;
    @Inject
    private CollectionLoader<ProcAttachment> procAttachmentsDl;
    @Inject
    private ProcActionsFragment procActionsFragment;
    @Inject
    private Table<ProcAttachment> attachmentsTable;

    private static final String PROCESS_CODE = "approvalApplicationRecruiting";

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            openDateField.setValue(new Date());
            recruiterField.setValue((ExtUser) userSession.getUser());
        }

        approvalApplicationRecrutitng();
    }

    private void approvalApplicationRecrutitng() {
        applicationRecruitmentListDl.load();
        procAttachmentsDl.setParameter("entityId",applicationRecruitmentListDc.getItem().getId());
        procAttachmentsDl.load();
        procActionsFragment.initializer()
                .standard()
                .init(PROCESS_CODE, getEditedEntity());

        FileDownloadHelper.initGeneratedColumn(attachmentsTable, "file");
    }

    public void automatingFillButton() {
        final String QUERY_STAFFING_TABLE
                = "select e from itpearls_StaffingTable e where e.active = true";
        final String QUERY_STAFF_CURRENT
                = "select e from itpearls_StaffCurrent e where e.staffingTable = :staffingTable";

        if (PersistenceHelper.isNew(getEditedEntity())) {

            LoadContext staffTable = LoadContext.create(StaffingTable.class)
                    .setQuery(LoadContext.createQuery(QUERY_STAFFING_TABLE));
            List<StaffingTable> staffingTable = dataManager.loadList(staffTable);

            dataContext.create(ApplicationRecruitment.class);
            List<ApplicationRecruitment> applicationRecruitments = new ArrayList<>();

            for (StaffingTable st : staffingTable) {
                LoadContext staffCurrent = LoadContext.create(StaffCurrent.class)
                        .setQuery(LoadContext.createQuery(QUERY_STAFF_CURRENT)
                                .setParameter("staffingTable", st))
                        .setView("staffCurrent-view");

                List<StaffCurrent> staffCurrents = dataManager.loadList(staffCurrent);

                if (st.getActive()) {
                    if (st.getNumberOfStaff() > staffCurrents.size()) {
                        ApplicationRecruitment applicationRecruitment = metadata.create(ApplicationRecruitment.class);
                        applicationRecruitment.setActive(true);
                        applicationRecruitment.setAmount(st.getNumberOfStaff() - staffCurrents.size());
                        applicationRecruitment.setApplicationDate(new Date());
                        applicationRecruitment.setStaffingTable(st);

                        applicationRecruitmentDc.getMutableItems().add(applicationRecruitment);
                        applicationRecruitments.add(applicationRecruitment);
                    }
                }
            }

            dataContext.merge(applicationRecruitments);
        }
    }
}