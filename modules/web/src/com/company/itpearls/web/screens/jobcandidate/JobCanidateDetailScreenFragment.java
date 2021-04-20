package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.WebBrowserTools;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LinkButton;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.*;

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
    protected JobCandidate jobCandidate = null;

    @Inject
    private Label<String> lastResearcherLabel;
    @Inject
    private LinkButton telegrammLinkButton;


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
    @Inject
    private Label<String> resumeCountLabel;
    @Inject
    private WebBrowserTools webBrowserTools;
    @Inject
    private LinkButton skypeLinkButton;
    @Inject
    private LinkButton emailLinkButton;

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public void setLinkButtonTelegrem() {
        String retStr = "";

        if (jobCandidatesDc.getItem().getTelegramName() != null) {
            if (jobCandidate == null) {
                retStr = jobCandidatesDc.getItem().getTelegramName().trim();
            } else {
                retStr = jobCandidate.getTelegramName().trim();
            }

            if (retStr != null) {
                if (retStr.charAt(0) == '@') {
                    retStr = retStr.substring(1);
                }

                telegrammLinkButton.setCaption(retStr);
            }
        }
    }

    public void setLinkButtonTelegremGroup() {
        String retStr = "";

        if (jobCandidatesDc.getItem().getTelegramGroup() != null) {
            if (jobCandidate == null) {
                retStr = jobCandidatesDc.getItem().getTelegramGroup().trim();
            } else {
                retStr = jobCandidate.getTelegramGroup().trim();
            }

            if (retStr != null) {
                if (retStr.charAt(0) == '@') {
                    retStr = retStr.substring(1);
                }

                telegrammLinkButton.setCaption(retStr);
            }
        }
    }

    public void setLinkButtonEmail() {
        if (jobCandidate == null) {
            if (jobCandidatesDc.getItem().getEmail() != null) {

                emailLinkButton.setCaption(jobCandidatesDc.getItem().getEmail());
            }
        } else {
            emailLinkButton.setCaption(jobCandidate.getEmail());
        }
    }


    public void setLinkButtonSkype() {
        if(jobCandidate == null) {
            if (jobCandidatesDc.getItem().getSkypeName() != null) {
                skypeLinkButton.setCaption(jobCandidatesDc.getItem().getSkypeName());
            }
        } else
            skypeLinkButton.setCaption(jobCandidate.getSkypeName());
    }

    @Subscribe("emailLinkButton")
    public void onEmailLinkButtonClick(Button.ClickEvent event) {
        webBrowserTools.showWebPage("mailto:" + event.getButton().getCaption(), null);
    }

    @Subscribe("telegrammLinkButton")
    public void onTelegrammLinkButtonClick(Button.ClickEvent event) {
        String retStr = event.getButton().getCaption();

        webBrowserTools.showWebPage("http://t.me/" + retStr, null);
    }


    @Subscribe("skypeLinkButton")
    public void onSkypeLinkButtonClick(Button.ClickEvent event) {
        webBrowserTools.showWebPage("skype:" + event.getButton().getCaption() + "?chat", null);

    }

    public void setLastSalaryLabel(String iteractionName) {
        IteractionList iteractionList = getStatistics(iteractionName);
        if (iteractionList != null) {
            if (iteractionList.getAddString() != null) {
                String salary = getStatistics(iteractionName).getAddString();
                salaryExpectationLabel.setValue(salary);
                salaryExpectationLabel.setDescription(getStatistics(iteractionName).getAddString());
            }
        }
    }

    public void setVisibleLogo() {

        if (candidateFaceImage.getValueSource().getValue() == null) {
            candidateFaceImage.setVisible(false);
        } else {
            candidateFaceImage.setVisible(true);
        }
    }

    public void setStatistics() {
        List<IteractionList> iteractionList = getAllCandidateIteractions();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

        // найти последнего рекрутера
        for (IteractionList iteraction : iteractionList) {
            if (iteraction.getRecrutier() != null)
                if (iteraction.getRecrutier().getGroup() != null)
                    if (iteraction.getRecrutier().getGroup().getName() != null)
                        if (iteraction.getRecrutier().getGroup().getName().equals(RECRUTIER_GROUP)) {
                            String lastRecrutier = iteraction.getRecrutier().getName() + " ("
                                    + simpleDateFormat.format(iteraction.getDateIteraction()) + ")";

                            lastRecruterLabel.setValue(lastRecrutier);
                            lastRecruterLabel.setDescription(lastRecrutier);
                            break;
                        }
        }

        //Найти последнего реcthxthf
        for (IteractionList iteraction : iteractionList) {
            if (iteraction.getRecrutier() != null)
                if (iteraction.getRecrutier().getGroup() != null)
                    if (iteraction.getRecrutier().getGroup().getName() != null)
                        if (iteraction.getRecrutier().getGroup().getName().equals(RESEARCHER_GROUP)) {
                            String lastResearcher = iteraction.getRecrutier().getName() + " ("
                                    + simpleDateFormat.format(iteraction.getDateIteraction()) + ")";
                            lastResearcherLabel.setValue(lastResearcher);
                            lastRecruterLabel.setDescription(lastResearcher);
                            break;
                        }
        }
        // последнее взаимодействие
        if (iteractionList.size() != 0) {
            String lastIteraction = iteractionList.get(0).getIteractionType().getIterationName() + " ("
                    + simpleDateFormat.format((iteractionList.get(0).getDateIteraction())) + ")";

            lastIteractionLabel.setValue(lastIteraction);
            lastIteractionLabel.setDescription(lastIteraction);

            if (candidateFaceImage == null) {
                String width = String.valueOf(lastIteractionLabel.getWidth() + 20);
                lastIteractionLabel.setWidth(width);
            }
        }

        if (iteractionList.size() != 0) {
            String project = "";

            try {
                project = iteractionList.get(0)
                        .getProject()
                        .getProjectDepartment()
                        .getCompanyName()
                        .getComanyName();
            } catch (Exception e) {
                project = "";
            }

            if (project != null) {
                companyLabel.setValue(project);
                companyLabel.setDescription(project);
            }
        }

        if (iteractionList.size() != 0) {
            String departament = "";

            try {
                departament = iteractionList.get(0)
                        .getProject()
                        .getProjectDepartment()
                        .getDepartamentRuName();
            } catch (Exception e) {
                departament = "";
            }

            if (departament != null) {
                departamentLabel.setValue(departament);
                departamentLabel.setDescription(departament);
            }
        }

        if (iteractionList.size() != 0) {
            String vacansyName = "";

            try {
                vacansyName = iteractionList.get(0)
                        .getVacancy()
                        .getVacansyName();
            } catch (Exception e) {
                vacansyName = "";
            }
            vacancyNameLabel.setValue(vacansyName);
            vacancyNameLabel.setDescription(vacansyName);
        }

        if (iteractionList.size() != 0) {
            String projectName = "";

            try {
                projectName = iteractionList.get(0)
                        .getProject()
                        .getProjectName();
            } catch (Exception e) {
                projectName = "";
            }

            projectNameLabel.setValue(projectName);
            projectNameLabel.setDescription(projectName);
        }

        iteractionCountLabel.setValue(String.valueOf(iteractionList.size()));
        resumeCountLabel.setValue(getResumeCount());
    }

    private String getResumeCount() {
        String QUERY_ALL_CV = "select e from itpearls_CandidateCV e " +
                "where e.candidate = :candidate ";
        String retStr = "";

        try {
            List<CandidateCV> candidateCV = dataManager.load(CandidateCV.class)
                    .query(QUERY_ALL_CV)
                    .view("candidateCV-view")
                    .parameter("candidate", jobCandidatesDc.getItem())
                    .list();

            retStr = String.valueOf(candidateCV.size());
        } catch (Exception e) {
            retStr = "0";
        }

        return retStr;
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
        } catch (Exception e) {
        }

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
        } catch (Exception e) {
        }

        return iteractionList;
    }
}