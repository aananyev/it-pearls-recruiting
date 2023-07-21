package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.Project;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;

import javax.inject.Inject;

@UiController("itpearls_JobCandidateInProject.browse")
@UiDescriptor("job-candidate-in-project-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class JobCandidateInProjectBrowse extends StandardLookup<JobCandidate> {
    @Inject
    private UiComponents uiComponents;

    private HBoxLayout columnGenerator(String text) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setHeightFull();
        retHBox.setWidthFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retLabel.setValue(text);

        retHBox.add(retLabel);
        return retHBox;
    }

    public Component vacancyColumnGenerator(JobCandidate entity) {

        IteractionList startProject = null;
        HBoxLayout retHBox = null;

        for (IteractionList iteractionList : entity.getIteractionList()) {
            if (iteractionList.getIteractionType().getSignStartProject() != null) {
                if (iteractionList.getIteractionType().getSignStartProject()) {
                    startProject = iteractionList;
                }
            }
        }


        if (startProject != null) {
            if (startProject.getVacancy() != null) {
                if (startProject.getVacancy().getVacansyName() != null) {
                    retHBox = columnGenerator(startProject.getVacancy().getVacansyName());
                }
            }
        }

        return retHBox;
    }

    public Component projectColumnGenerator(JobCandidate entity) {
        HBoxLayout retHBox = null;

        Project project = null;

        for (IteractionList iteractionList : entity.getIteractionList()) {
            if (iteractionList.getVacancy() != null) {
                if (iteractionList.getVacancy().getProjectName() != null) {
                        project = iteractionList.getVacancy().getProjectName();
                }
            }
        }

        if (project.getProjectName() != null) {
            retHBox = columnGenerator(project.getProjectName());
        }

        return retHBox;
    }
}