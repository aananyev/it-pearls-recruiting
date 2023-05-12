package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateEdit;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.ui.JavaScript;

import javax.inject.Inject;
import java.text.SimpleDateFormat;

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
    private Button buttonExcel;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private DataGrid<IteractionList> iteractionListsTable;
    @Inject
    private Screens screens;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private Button jobCandidateCardButton;
    @Inject
    private Notifications notifications;
    @Inject
    private Button clipBtn;
    @Inject
    private Button itercationListButton;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        buttonExcel.setVisible(getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER));

        filterStagerField();
        filterUoustaffing();
        addTableListeners();
//        filterInternalProject();
    }

    private void filterUoustaffing() {
        if (userSession.getUser().getGroup().getName().equals(StandartRoles.MANAGER)) {
            iteractionListsDl.setParameter("outStaffing", true);
        } else {
            iteractionListsDl.removeParameter("outStaffing");
        }

        iteractionListsDl.load();
    }

    private void filterStagerField() {
        if (userSession.getUser().getGroup().getName().equals(StandartRoles.STAGER)) {
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
        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.ADMINISTRATOR)) {
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
        if (item.getIteractionType() != null) {
            if (item.getIteractionType().getPic() != null) {
                return item.getIteractionType().getPic();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Subscribe
    public void onInit(InitEvent event) {
        addIconColumn();
    }

    private void addTableListeners() {
        jobCandidateCardButton.setEnabled(false);

        iteractionListsTable.addSelectionListener(event -> {
            if (event.getSelected() == null) {
                jobCandidateCardButton.setEnabled(false);
                clipBtn.setEnabled(false);
                itercationListButton.setEnabled(false);
                buttonCopy.setEnabled(false);

            } else {
                jobCandidateCardButton.setEnabled(true);
                clipBtn.setEnabled(true);
                itercationListButton.setEnabled(true);
                buttonCopy.setEnabled(true);
            }
        });
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

    public void onButtonCopyToClibboard() {
        String clipboardText = "";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

        if (iteractionListsTable.getSelected() != null) {
            for (IteractionList i : iteractionListsTable.getSelected()) {
                clipboardText = clipboardText + i.getCandidate().getFullName() + "," +
                        simpleDateFormat.format(i.getDateIteraction()) + "," +
                        i.getVacancy().getVacansyName();
            }
        }

        JavaScript.getCurrent().execute("navigator.clipboard.writeText('" +
                clipboardText
                + "');");
        notifications.create().withCaption("Copied to clipboard").show();
    }

    public void onIteractionListButton() {
        IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
        iteractionListSimpleBrowse.setSelectedCandidate(iteractionListsTable.getSingleSelected().getCandidate());
        screens.show(iteractionListSimpleBrowse);
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "columnGenerator")
    private Icons.Icon iteractionListsTableCurrentOpenCloseColumnColumnGenerator(
            DataGrid.ColumnGeneratorEvent<IteractionList> columnGeneratorEvent) {

        if (columnGeneratorEvent.getItem().getCurrentOpenClose() != null) {
            return columnGeneratorEvent.getItem().getCurrentOpenClose()
                    ? CubaIcon.MINUS_CIRCLE : CubaIcon.PLUS_CIRCLE;
        } else {
            if (columnGeneratorEvent.getItem().getVacancy() != null) {
                return columnGeneratorEvent.getItem().getVacancy().getOpenClose() != null ?
                        (columnGeneratorEvent.getItem().getVacancy().getOpenClose() ?
                                CubaIcon.MINUS_CIRCLE : CubaIcon.PLUS_CIRCLE) : CubaIcon.PLUS_CIRCLE;
            } else {
                return CubaIcon.CANCEL;
            }
        }
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "styleProvider")
    private String iteractionListsTableCurrentOpenCloseColumnStyleProvider(
            IteractionList iteractionList) {

        if (iteractionList.getCurrentOpenClose() != null) {
            return iteractionList.getCurrentOpenClose()
                    ? "pic-center-large-red" : "pic-center-large-green";
        } else {
            if (iteractionList.getVacancy() != null) {
                return iteractionList.getVacancy().getOpenClose() ?
                        "pic-center-large-red" : "pic-center-large-green";
            } else {
                return "pic-center-large-gray";
            }
        }
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "descriptionProvider")
    private String iteractionListsTableCurrentOpenCloseColumnDescriptionProvider(IteractionList iteractionList) {

        if (iteractionList.getCurrentOpenClose() != null) {
            return iteractionList.getCurrentOpenClose()
                    ? "Закрыта на момент создания взаимодействия" : "Открыта на момент создания взаимодействия";
        } else {
            if (iteractionList.getVacancy() != null) {
                return iteractionList.getVacancy().getOpenClose() ?
                        "Закрыта на текущий момент" : "Открыта на текущий момент";
            } else {
                return null;
            }
        }
    }
}