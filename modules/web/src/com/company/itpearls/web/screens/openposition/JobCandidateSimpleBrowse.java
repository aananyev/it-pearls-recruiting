package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.collections.BagUtils;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

@UiController("itpearls_JobCandidateSimple.browse")
@UiDescriptor("job-candidate-simple-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class JobCandidateSimpleBrowse extends StandardLookup<IteractionList> {

    @Inject
    private CollectionLoader<JobCandidate> jobCandidateDl;
    JobCandidate jobCandidate = null;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Label<String> vacancyLabel;
    @Inject
    private ScreenBuilders screenBuilders;

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    private OpenPosition openPosition;

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
        jobCandidateDl.setParameter("vacancy", this.openPosition);
        jobCandidateDl.load();
    }

    @Install(to = "iteractionListsTable.active", subject = "columnGenerator")
    private Component iteractionListsTableActiveColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        Boolean flag = false;

        for (IteractionList iteractionList : event.getItem().getIteractionList()) {
            if (iteractionList.getIteractionType() != null) {
                if (iteractionList.getIteractionType().getSignEndCase() != null) {
                    if (iteractionList.getIteractionType().getSignEndCase()) {
                        retLabel.setIconFromSet(CubaIcon.MINUS_CIRCLE);
                        retLabel.setStyleName("open-position-pic-center-large-red");
                        retLabel.setDescription("С кандидатом кейс завершен по этой вакансии");

                        flag = true;
                        break;
                    }
                }
            }
        }

        if (!flag) {
            retLabel.setIconFromSet(CubaIcon.PLUS_CIRCLE);
            retLabel.setStyleName("open-position-pic-center-large-green");
            retLabel.setDescription("С кандидатом кейс НЕ завершен по этой вакансии");
        }

        return retLabel;
    }

    @Install(to = "iteractionListsTable.lastInterationName", subject = "columnGenerator")
    private Component iteractionListsTableLastInterationNameColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {

        Date startDate = event.getItem().getIteractionList().get(0).getDateIteraction();
        String startInteractionName = null;
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);

        if (event.getItem().getIteractionList() != null) {
            if (event.getItem().getIteractionList().get(0) != null) {
                if (event.getItem().getIteractionList().get(0).getDateIteraction() != null) {
                    startInteractionName = event
                            .getItem()
                            .getIteractionList()
                            .get(0)
                            .getIteractionType()
                            .getIterationName();
                }
            }
        }

        if (startDate != null) {
            Label retLabel = uiComponents.create(Label.class);

            for (IteractionList iteractionList : event.getItem().getIteractionList()) {
                if (iteractionList.getDateIteraction() != null) {
                    if (startDate.before(iteractionList.getDateIteraction())) {
                        startDate = iteractionList.getDateIteraction();
                        startInteractionName = iteractionList.getIteractionType().getIterationName();
                    }
                }
            }


            retLabel.setValue(startInteractionName);
            retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);

            retBox.setWidthFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retBox.add(retLabel);

        }

        return retBox;
    }

    @Install(to = "iteractionListsTable.lastInteractionDate", subject = "columnGenerator")
    private Component iteractionListsTableLastInteractionDateColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {

        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        Date startDate = event.getItem().getIteractionList().get(0).getDateIteraction();

        if (startDate != null) {
            Label retLabel = uiComponents.create(Label.class);

            for (IteractionList iteractionList : event.getItem().getIteractionList()) {
                if (iteractionList.getDateIteraction() != null) {
                    if (startDate.before(iteractionList.getDateIteraction())) {
                        startDate = iteractionList.getDateIteraction();
                    }
                }
            }


            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            retLabel.setValue(sdf.format(startDate));
            retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

            retBox.setWidthFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retBox.add(retLabel);

        }
        return retBox;
    }

    @Install(to = "iteractionListsTable.recruter", subject = "columnGenerator")
    private Component iteractionListsTableRecruterColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        User recruter = null;

        for (IteractionList iteractionList : event.getItem().getIteractionList()) {
            if (iteractionList.getIteractionType() != null) {
                if (iteractionList.getIteractionType().getSignOurInterview() != null) {
                    if (iteractionList.getIteractionType().getSignOurInterview()) {
                        recruter = iteractionList.getRecrutier();
                    }
                }
            }
        }

        if (recruter != null) {
            retLabel.setValue(recruter.getName());
            retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
            retBox.setWidthFull();
            retBox.setHeightFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retBox.add(retLabel);
        }
        return retBox;
    }

    @Install(to = "iteractionListsTable.sourcer", subject = "columnGenerator")
    private Component iteractionListsTableSourcerColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        User sourcer = null;

        for (IteractionList iteractionList : event.getItem().getIteractionList()) {
            if (iteractionList.getIteractionType() != null) {
                if (iteractionList.getIteractionType().getSignOurInterviewAssigned() != null) {
                    if (iteractionList.getIteractionType().getSignOurInterviewAssigned()) {
                        sourcer = iteractionList.getRecrutier();
                    }
                }
            }
        }

        if (sourcer != null) {
            retLabel.setValue(sourcer.getName());
            retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
            retBox.setWidthFull();
            retBox.setHeightFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retBox.add(retLabel);
        }
        return retBox;
    }

    public void setHeader(OpenPosition openPosition) {
        vacancyLabel.setValue(openPosition.getVacansyName());
    }

    @Install(to = "iteractionListsTable.jobCandidateCard", subject = "columnGenerator")
    private Component iteractionListsTableJobCandidateCardColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        Button retButton = uiComponents.create(Button.class);

        retButton.setCaption("Просмотр");
        retButton.setDescription("Просмотр и редактирование карточки кандидата");
        retButton.setWidthAuto();
        retButton.setHeightAuto();
        retButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retButton.addClickListener(e -> {
            screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(event.getItem())
                    .build()
                    .show();
        });

        retBox.setHeightFull();
        retBox.setWidthFull();
        retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        retBox.add(retButton);
        return retBox;
    }
}