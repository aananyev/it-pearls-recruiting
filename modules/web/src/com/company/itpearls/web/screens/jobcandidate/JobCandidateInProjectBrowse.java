package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.*;
import com.company.itpearls.web.StandartRegistrationForWork;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_JobCandidateInProject.browse")
@UiDescriptor("job-candidate-in-project-browse.xml")
@LookupComponent("employeeTable")
@LoadDataBeforeShow
public class JobCandidateInProjectBrowse extends StandardLookup<Employee> {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private RadioButtonGroup selectTypeOfWorksRadioButton;
    @Inject
    private RadioButtonGroup recrutingOrAutstaffingRadioButtonGroup;
    @Inject
    private MessageBundle messageBundle;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setCandidateCollectionToWork();
    }

    private void setCandidateCollectionToWork() {
    }

    @Subscribe
    public void onInit(InitEvent event) {
        setSelectTypeOfWorksRadioButtonGroup();
    }

    private void setSelectTypeOfWorksRadioButtonGroup() {
        Map<String, Integer> workStatusMap = new LinkedHashMap<>();

        workStatusMap.put("В проекте", 0);
        workStatusMap.put("На бенче", 1);
        workStatusMap.put("Ранее работал на проектах фуллтайм", 2);
        workStatusMap.put("Ранее работал на проектах парттайм", 3);

        selectTypeOfWorksRadioButton.setOptionsMap(workStatusMap);

        Map<String, Integer> recruitingOrOutstaffingMap = new LinkedHashMap<>();
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.ALL_MSG),
                StandartRegistrationForWork.ALL);
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.RECRUITING_MSG),
                StandartRegistrationForWork.RECRUITING);
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.OUTSTAFING_MSG),
                StandartRegistrationForWork.OUTSTAFING);

        recrutingOrAutstaffingRadioButtonGroup.setOptionsMap(recruitingOrOutstaffingMap);
    }

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

    public Component projectColumnGenerator(Employee entity) {
        HBoxLayout retHBox = null;

        Project project = null;

        for (IteractionList iteractionList : entity.getJobCandidate().getIteractionList()) {
            if (iteractionList.getVacancy() != null) {
                if (iteractionList.getVacancy().getProjectName() != null) {
                    project = iteractionList.getVacancy().getProjectName();
                }
            }
        }

        if (project != null) {
            if (project.getProjectName() != null) {
                retHBox = columnGenerator(project.getProjectName());
            }
        }

        return retHBox;
    }
}
