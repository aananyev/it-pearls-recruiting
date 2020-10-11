package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@UiController("itpearls_JobCanidateDetailScreenFragment")
@UiDescriptor("job-canidate-detail-screen-fragment.xml")
public class JobCanidateDetailScreenFragment extends ScreenFragment {
    @Inject
    private Label<String> lastRecruterLabel;
    @Inject
    private DataManager dataManager;
    @Inject
    private CollectionContainer<JobCandidate> jobCandidatesDc;
    @Inject
    private Label<String> salaryExpectationLabel;

    private static final String MANAGER_GROUP = "Менеджмент";
    private static final String RECRUTIER_GROUP = "Хантинг";
    private static final String RESEARCHER_GROUP = "Ресерчинг";
    @Inject
    private Label<String> lastResearcherLabel;
    @Inject
    private Label<String> lastIteractionLabel;
    @Inject
    private Image candidateFaceImage;
    @Inject
    private Label<String> iteractionCountLabel;
    @Inject
    private Label<String> companyLabel;
    @Inject
    private Label<String> departamentLabel;
    @Inject
    private Label<String> vacancyNameLabel;
    @Inject
    private Label<String> projectNameLabel;

    public void setLastSalaryLabel(String iteractionName) {
        IteractionList iteractionList = getStatistics(iteractionName);
        if (iteractionList != null) {
            if (iteractionList.getAddString() != null) {
                salaryExpectationLabel.setValue(getStatistics(iteractionName).getAddString());
            }
        }
    }

    public void setVisibleLogo() {

        if(candidateFaceImage.getValueSource().getValue() == null) {
            candidateFaceImage.setVisible(false);
        } else {
            candidateFaceImage.setVisible(true);
        }
    }

    public void setStatistics() {
        List<IteractionList> iteractionList = getAllCandidateIteractions();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

        // найти последнего рекрутера
        for(IteractionList iteraction : iteractionList) {
            if(iteraction.getRecrutier().getGroup().getName().equals(RECRUTIER_GROUP)) {
                lastRecruterLabel.setValue(iteraction.getRecrutier().getName() + "("
                        + simpleDateFormat.format(iteraction.getDateIteraction()) + ")");
                break;
            }
        }

        //Найти последнего реcthxthf
        for(IteractionList iteraction : iteractionList) {
            if(iteraction.getRecrutier().getGroup().getName().equals(RESEARCHER_GROUP)) {
                lastResearcherLabel.setValue(iteraction.getRecrutier().getName() + "("
                        + simpleDateFormat.format(iteraction.getDateIteraction()) + ")");
                break;
            }
        }
        // последнее взаимодействие
        if (iteractionList.size() != 0) {
            lastIteractionLabel.setValue(iteractionList.get(0).getIteractionType().getIterationName() + "("
                    + simpleDateFormat.format((iteractionList.get(0).getDateIteraction())) + ")");
        }

        if (iteractionList.size() != 0) {
            companyLabel.setValue(iteractionList.get(0)
                    .getProject()
                    .getProjectDepartment()
                    .getCompanyName()
                    .getComanyName());
        }

        if (iteractionList.size() != 0) {
            departamentLabel.setValue(iteractionList.get(0)
                    .getProject()
                    .getProjectDepartment()
                    .getDepartamentRuName());
        }

        if (iteractionList.size() != 0) {
            vacancyNameLabel.setValue(iteractionList.get(0)
                    .getVacancy()
                    .getVacansyName());
        }

        if (iteractionList.size() != 0) {
            projectNameLabel.setValue(iteractionList.get(0)
                    .getProject()
                    .getProjectName());
        }

        iteractionCountLabel.setValue(String.valueOf(iteractionList.size()));
    }

    private List<IteractionList> getAllCandidateIteractions() {
        String QUERY_ALL_ITERACIONS = "select e from itpearls_IteractionList e " +
                "where e.candidate = :candidate " +
                "order by e.dateIteraction desc";

        List<IteractionList> iteractionLists = new ArrayList<>();

        try {
            iteractionLists = dataManager.load(IteractionList.class)
                    .query(QUERY_ALL_ITERACIONS)
                    .view("iteractionList-view")
                    .parameter("candidate", jobCandidatesDc.getItem())
                    .list();
        } catch (Exception e) {}

        return iteractionLists;
    }

    private IteractionList getStatistics(String iteractionName) {

        String QUERY_LAST_SALARY = "select e from itpearls_IteractionList e where e.iteractionType = " +
                "(select f from itpearls_Iteraction f where f.iterationName like :iteractionName) and " +
                "e.candidate = :candidate";

        IteractionList iteractionList = null;

        try {
            iteractionList = dataManager.load(IteractionList.class)
                    .query(QUERY_LAST_SALARY)
                    .view("iteractionList-view")
                    .parameter("iteractionName", iteractionName)
                    .parameter("candidate", jobCandidatesDc.getItem())
                    .one();
        } catch (Exception e) {}

        return iteractionList;
    }
}