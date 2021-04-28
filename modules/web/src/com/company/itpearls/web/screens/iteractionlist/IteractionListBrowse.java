package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateEdit;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

@UiController("itpearls_IteractionList.browse")
@UiDescriptor("iteraction-list-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class IteractionListBrowse extends StandardLookup<IteractionList> {
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private CheckBox checkBoxShowOnlyMy;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Button buttonCopy;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Button buttonExcel;
    @Inject
    private GetRoleService getRoleService;

    static String RESEARCHER = "Researcher";
    static String RECRUITER = "Recruiter";
    static String MANAGER = "Manager";
    static String ADMINISTRATOR = "Administrators";
    static String STAGER = "Стажер";

    @Inject
    private DataManager dataManager;
    @Inject
    private DataGrid<IteractionList> iteractionListsTable;
    private DataContext dataContext;
    @Inject
    private Screens screens;
    @Inject
    private StarsAndOtherService starsAndOtherService;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        buttonExcel.setVisible(getRoleService.isUserRoles(userSession.getUser(), MANAGER));

        filterStagerField();
//        filterInternalProject();
    }

    private void filterStagerField() {
        if (userSession.getUser().getGroup().getName().equals(STAGER)) {
            iteractionListsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");

            checkBoxShowOnlyMy.setValue(true);
            checkBoxShowOnlyMy.setEditable(false);

            iteractionListsDl.load();
        }
    }

    @Install(to = "iteractionListsTable.iteractionType", subject = "descriptionProvider")
    private String iteractionListsTableIteractionTypeDescriptionProvider(IteractionList iteractionList) {
        String add = "";

        if (iteractionList.getAddDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy H:m");
            add = dateFormat.format(iteractionList.getAddDate());
        }

        if (iteractionList.getAddString() != null)
            add = iteractionList.getAddString();

        if (iteractionList.getAddInteger() != null)
            add = iteractionList.getAddInteger().toString();

        return (iteractionList.getComment() != null ? iteractionList.getComment() : "") + add;
    }

    private void filterInternalProject() {
        if (getRoleService.isUserRoles(userSession.getUser(), MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), ADMINISTRATOR)) {
            iteractionListsDl.removeParameter("internalProject");
        } else {
            iteractionListsDl.setParameter("internalProject", false);
        }

        try {
            iteractionListsDl.load();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Subscribe("checkBoxShowOnlyMy")
    public void onCheckBoxShowOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxShowOnlyMy.getValue()) {
            iteractionListsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        } else {
            iteractionListsDl.removeParameter("userName");
        }

        iteractionListsDl.load();
    }

    public void onButtonCopyClick() {
        Screen screen = screenBuilders.editor(iteractionListsTable)
                .newEntity()
                .withInitializer(data -> {
                    if (iteractionListsTable.getSingleSelected() != null) {

                        data.setCandidate(iteractionListsTable.getSingleSelected().getCandidate());
                        data.setVacancy(iteractionListsTable.getSingleSelected().getVacancy());
                        // data.setCompanyDepartment( iteractionListsTable.getSingleSelected().getCompanyDepartment() );
                        data.setProject(iteractionListsTable.getSingleSelected().getProject());
                    }
                })
                .withScreenClass(IteractionListEdit.class)
                .withLaunchMode(OpenMode.THIS_TAB)
                .build();

        screen.show();
    }

    private void addIconColumn() {
        DataGrid.Column iconColumn = iteractionListsTable.addGeneratedColumn("icon",
                new DataGrid.ColumnGenerator<IteractionList, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
                        return getIcon(event.getItem());
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        iconColumn.setRenderer(iteractionListsTable.createRenderer(DataGrid.ImageRenderer.class));
    }


    private String getIcon(IteractionList item) {
        return item.getIteractionType().getPic();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        addIconColumn();
    }

    public void onJobCandidateButtonClick() {
        JobCandidateEdit jobCandidateEdit = screens.create(JobCandidateEdit.class);

        jobCandidateEdit.setEntityToEdit(iteractionListsTable.getSingleSelected().getCandidate());
        screens.show(jobCandidateEdit);
    }

    @Install(to = "iteractionListsTable.rating", subject = "columnGenerator")
    private String iteractionListsTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {

        return event.getItem().getRating() != null ? starsAndOtherService.setStars(event.getItem().getRating() + 1) : "";
    }

    @Install(to = "iteractionListsTable.rating", subject = "styleProvider")
    private String iteractionListsTableRatingStyleProvider(IteractionList iteractionList) {
        return "rating_box";
    }
}