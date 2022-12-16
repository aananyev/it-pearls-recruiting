package com.company.itpearls.web.screens.applicationrecruitmentlist;

import com.company.itpearls.entity.*;
import com.haulmont.bpm.entity.ProcAttachment;
import com.haulmont.bpm.gui.procactionsfragment.ProcActionsFragment;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.app.core.file.FileDownloadHelper;
import com.haulmont.cuba.gui.components.*;
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

    private static final String PROCESS_CODE = "applicationRecruitmentApproval";
    @Inject
    private TextField<String> codeTextField;
    @Inject
    private Notifications notifications;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Button openPositionsButton;
    @Inject
    private Button approveAllButton;
    @Inject
    private LookupPickerField<CompanyDepartament> projectDepartmentLookupPicketField;
    @Inject
    private LookupPickerField<Company> companyLookupPickerField;
    @Inject
    private CollectionLoader<Company> companyDl;
    @Inject
    private CollectionContainer<Project> projectDc;
    @Inject
    private CollectionLoader<Project> projectDl;
    @Inject
    private CollectionLoader<CompanyDepartament> projectDepartmentDl;
    @Inject
    private LookupPickerField<Project> projectLookupPickerField;

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
        if (codeTextField.getValue() != null) {
            final String QUERY_STAFFING_TABLE
                    = "select e from itpearls_StaffingTable e where e.active = true";
            final String QUERY_STAFF_CURRENT
                    = "select e from itpearls_StaffCurrent e where e.staffingTable = :staffingTable";

            StringBuffer QUERY_STAFFING_TABLE_RESULT = new StringBuffer(QUERY_STAFFING_TABLE);

            // Project
            QUERY_STAFFING_TABLE_RESULT.append(projectLookupPickerField.getValue() != null
                    ? " and e.openPosition.projectName.projectName like '" : "");

            QUERY_STAFFING_TABLE_RESULT.append(projectLookupPickerField.getValue() != null
                    ? projectLookupPickerField.getValue().getProjectName() + "'" : "");

            if (PersistenceHelper.isNew(getEditedEntity())) {

                LoadContext staffTable = LoadContext.create(StaffingTable.class)
                        .setQuery(LoadContext.createQuery(QUERY_STAFFING_TABLE_RESULT.toString()));
                List<StaffingTable> staffingTable = dataManager.loadList(staffTable);

                dataContext.create(ApplicationRecruitment.class);
                List<ApplicationRecruitment> applicationRecruitments = new ArrayList<>();
                Integer counter = 1;
                Boolean flag = false;

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
                            applicationRecruitment.setCode(codeTextField.getValue() + "/" + counter++);
                            applicationRecruitment.setApplicationRecruitmentList(getEditedEntity());

                            applicationRecruitmentDc.getMutableItems().add(applicationRecruitment);
//                            applicationRecruitments.add(applicationRecruitment);
                            dataContext.merge(applicationRecruitment);
                            flag = true;
                        }
                    }
                }

                if (flag) {
                    dataContext.commit();
                }
            }
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Заполните код вакансии")
                    .withDescription("ВНИМАНИЕ")
                    .show();

            getWindow().setFocusComponent("codeTextField");
        }
    }

    public void openPositionButtonInvoke() {
        dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                .withType(Dialogs.MessageType.CONFIRMATION)
                .withCaption("ЗАПРОС")
                .withMessage("Открыть все согласованные вакансии?")
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                    setOpenPositionAllApprovedApplication();

                    notifications.create(Notifications.NotificationType.TRAY)
                            .withDescription("Открыты все согласованные вакансии из списка")
                            .withCaption("ИНФОРМАЦИЯ")
                            .show();
                }), new DialogAction((DialogAction.Type.NO)))
                .show();;
    }

    @Subscribe(id = "applicationRecruitmentDc", target = Target.DATA_CONTAINER)
    public void onApplicationRecruitmentDcCollectionChange(CollectionContainer.CollectionChangeEvent<ApplicationRecruitment> event) {

        if (event.getSource().getItems().size() == 0) {
            approveAllButton.setEnabled(false);
        } else {
            approveAllButton.setEnabled(true);
        }

        enableOpenPositionButton();
    }

    private void enableOpenPositionButton() {
        Boolean flag = false;

        for (ApplicationRecruitment applicationRecruitment : applicationRecruitmentDc.getItems()) {
            if (applicationRecruitment.getApproval() != null) {
                if (applicationRecruitment.getApproval()) {
                    flag = true;
                    break;
                }
            }
        }

        openPositionsButton.setEnabled(flag);
    }


    private void setOpenPositionAllApprovedApplication() {
        for (ApplicationRecruitment applicationRecruitment : applicationRecruitmentDc.getItems()) {
            if (applicationRecruitment.getActive()) {
                applicationRecruitment.getStaffingTable().getOpenPosition().setOpenClose(false);
            }
        }
    }

    public void approveAllButtonInvoke() {
        dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                .withType(Dialogs.MessageType.CONFIRMATION)
                .withCaption("ЗАПРОС")
                .withMessage("Согласовать все не заполненные вакансии?")
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                    setApprovalAllApplication();

                    notifications.create(Notifications.NotificationType.TRAY)
                            .withDescription("Согласованы все вакансии из списка")
                            .withCaption("ИНФОРМАЦИЯ")
                            .show();
                }), new DialogAction(DialogAction.Type.NO))
                .show();
    }

    private void setApprovalAllApplication() {
        for (ApplicationRecruitment applicationRecruitment : applicationRecruitmentDc.getItems()) {
            applicationRecruitment.setApproval(true);
        }
    }

    @Subscribe("projectLookupPickerField")
    public void onProjectLookupPickerFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        if (event.getValue() != null) {
            if (event.getValue().getProjectDepartment() != null) {
                projectDepartmentLookupPicketField.setValue(event.getValue().getProjectDepartment());
            }
        } else {
            projectDepartmentLookupPicketField.setValue(null);
            companyLookupPickerField.setValue(null);

            projectDl.removeParameter("projectDepartment");
        }

        projectDepartmentDl.removeParameter("companyName");
    }

    @Subscribe("projectDepartmentLookupPicketField")
    public void onProjectDepartmentLookupPicketFieldValueChange(HasValue.ValueChangeEvent<CompanyDepartament> event) {
        if (event.getValue() != null) {
            if (event.getValue().getCompanyName() != null) {
                companyLookupPickerField.setValue(event.getValue().getCompanyName());
            }
        }
    }
}