package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WebBrowserTools;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import com.ibm.icu.impl.CalendarCache;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Calendar;

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

    List<IteractionList> iteractionList = new ArrayList<>();
    @Inject
    private UiComponents uiComponents;
    @Inject
    private UserSession userSession;
    @Inject
    private HBoxLayout statisticsHLabelBox;
    @Inject
    private Image candidateFaceDefaultImage;

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
        if (jobCandidate == null) {
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
            candidateFaceDefaultImage.setVisible(true);
        } else {
            candidateFaceImage.setVisible(true);
            candidateFaceDefaultImage.setVisible(false);
        }
    }

    public void setStatistics() {
        iteractionList = getAllCandidateIteractions();
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
            String lastIteraction = "";

            if (iteractionList.get(0).getIteractionType() != null) {
                if (iteractionList.get(0).getIteractionType().getIterationName() != null) {
                    lastIteraction = iteractionList.get(0).getIteractionType().getIterationName();
                }
            }

            if (iteractionList.get(0).getDateIteraction() != null) {
                lastIteraction += "(" + simpleDateFormat.format((iteractionList.get(0).getDateIteraction())) + ")";
            }

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
                        .getVacancy()
                        .getProjectName()
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
                        .getVacancy()
                        .getProjectName()
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
                        .getVacancy()
                        .getProjectName()
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
            e.printStackTrace();
        }

        return iteractionList;
    }

    static private String DESC_DATE_ITERACTION = "Даты процессинга кандидата: начало с ним работы и дата последнего взаимодействия";
    static private String DESC_DAYS_LAST_ITERCATION = "Дней с даты последнего взаимодействия";
    static private String DESC_DAYS_ON_LAST_PROJECT = "Количество дней на рассмотрении последнего проекта";

    public void setStatisticsLabel() {
        if(iteractionList.size() != 0) {
            LocalDate d1 = LocalDate.now();
            LocalDate d2 = iteractionList.get(0).getDateIteraction().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long days = ChronoUnit.DAYS.between(d2, d1) - 30;

            if (iteractionList.size() > 0) {
                Label activityCandidate = getActivityCandidatePeriod();

                Label startIteractionDate = getStartIteractionDate();
                // даты взаимодействия

                // дней с последнего взаиможействия
                Label lastItercationDayCount = lastIterDayCount();

                statisticsHLabelBox.add(activityCandidate);
                statisticsHLabelBox.add(startIteractionDate);

                if (days > 0) {
                    statisticsHLabelBox.add(lastItercationDayCount);
                }

                Label countDaysOnLastProject = getCountDaysLastProject();

                if (countDaysOnLastProject != null) {
                    statisticsHLabelBox.add(countDaysOnLastProject);
                }

                // количество дней после собеседования последнего
                Label countAfterITPearlsInterview = getCountAfterITPearlsInterview();

                if (countAfterITPearlsInterview != null) {
                    statisticsHLabelBox.add(countAfterITPearlsInterview);
                }

                // Количество дней с момента передачи резюме заказчику
                Label countAfterSendCVToClient = getCountAfterSendCVToClient();

                if (countAfterSendCVToClient != null) {
                    statisticsHLabelBox.add(countAfterSendCVToClient);
                }

                // число дней с момента интервью на стороне заказчика
                Label countAfrerClientInterview = getCountAfrerClientInterview();

                if (countAfrerClientInterview != null) {
                    statisticsHLabelBox.add(countAfrerClientInterview);
                }
            }
        }
    }

    private Label getCountAfterSendCVToClient() {
        Label countAfterSendCVToClient = uiComponents.create(Label.NAME);

        for (IteractionList iList : iteractionList) {
            if (iList.getIteractionType().getSignSendToClient() != null) {
                if (iList.getIteractionType().getSignSendToClient()) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(iList.getDateIteraction());
                    calendar.add(Calendar.DAY_OF_MONTH, 3);

                    Calendar calendar1 = Calendar.getInstance();

                    Calendar calendar2 = Calendar.getInstance();
                    calendar2.setTime(iteractionList.get(0).getDateIteraction());
                    calendar2.add(Calendar.DAY_OF_MONTH, 7);

                    LocalDate d1 = LocalDate.now();
                    LocalDate d2 = iList.getDateIteraction().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    long days = ChronoUnit.DAYS.between(d2, d1);


                    if(days != 0) {
                        countAfterSendCVToClient.setValue("CV у заказчика: " + days + " дней");
                        countAfterSendCVToClient.setStyleName(getStyleOnlastProcess(calendar, calendar1, calendar2));
                    } else {
                        countAfterSendCVToClient.setValue("Сегодня CV отправлено");
                        countAfterSendCVToClient.setStyleName("button_table_blue");
                    }

                    countAfterSendCVToClient.setDescription("Число дней с момента передачи резюме заказчику.");

                    return countAfterSendCVToClient;
                }
            }
        }

        return null;

    }

    private Label getCountAfrerClientInterview() {
        Label countAfrerClientInterview = uiComponents.create(Label.NAME);

        for (IteractionList iList : iteractionList) {
            if (iList.getIteractionType().getSignClientInterview() != null) {
                if (iList.getIteractionType().getSignClientInterview()) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(iList.getDateIteraction());
                    calendar.add(Calendar.DAY_OF_MONTH, 3);

                    Calendar calendar1 = Calendar.getInstance();

                    Calendar calendar2 = Calendar.getInstance();
                    calendar2.setTime(iteractionList.get(0).getDateIteraction());
                    calendar2.add(Calendar.DAY_OF_MONTH, 7);

                    LocalDate d1 = LocalDate.now();
                    LocalDate d2 = iList.getDateIteraction().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    long days = ChronoUnit.DAYS.between(d2, d1);


                    if(days != 0) {
                        countAfrerClientInterview.setValue("Интервью заказчика: " + days + " дней");
                        countAfrerClientInterview.setStyleName(getStyleOnlastProcess(calendar, calendar1, calendar2));
                    } else {
                        countAfrerClientInterview.setValue("Сегодня интервью у заказчика");
                        countAfrerClientInterview.setStyleName("button_table_blue");

                    }
                    countAfrerClientInterview.setDescription("Число дней с момента последнего интервью на стороне заказчика: технического или  рекрутером.");

                    return countAfrerClientInterview;
                }
            }
        }

        return null;
    }

    private Label getCountAfterITPearlsInterview() {
        Label countAfterITPearlsInterview = uiComponents.create(Label.NAME);

        for (IteractionList iList : iteractionList) {
            if (iList.getIteractionType().getSignOurInterview() != null) {
                if (iList.getIteractionType().getSignOurInterview()) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(iList.getDateIteraction());
                    calendar.add(Calendar.DAY_OF_MONTH, 3);

                    Calendar calendar1 = Calendar.getInstance();

                    Calendar calendar2 = Calendar.getInstance();
                    calendar2.setTime(iteractionList.get(0).getDateIteraction());
                    calendar2.add(Calendar.DAY_OF_MONTH, 7);

                    LocalDate d1 = LocalDate.now();
                    LocalDate d2 = iList.getDateIteraction().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    long days = ChronoUnit.DAYS.between(d2, d1);


                    if (days != 0) {
                        countAfterITPearlsInterview.setValue("Локальное интервью: " + days + " дней");
                        countAfterITPearlsInterview.setStyleName(getStyleOnlastProcess(calendar, calendar1, calendar2));
                    } else {
                        countAfterITPearlsInterview.setValue("Сегодня локальное интервью");
                        countAfterITPearlsInterview.setStyleName("button_table_blue");
                    }

                    countAfterITPearlsInterview.setDescription("Число дней с момента последнего интервью на нашей стороне рекрутером IT Pearls.");

                    return countAfterITPearlsInterview;
                }
            }
        }

        return null;
    }

    private String getStyleOnlastProcess(Calendar cal, Calendar cal1, Calendar cal2) {
        String style;

        if (cal.after(cal1)) {
            if (!iteractionList.get(0).getRecrutier().equals(userSession.getUser())) {
                style = "button_table_gray";
            } else {
                style = "button_table_yellow";
            }
        } else {
            if (cal1.after(cal2)) {
                style = "button_table_gray";
            } else {
                style = "button_table_red";
            }
        }

        return style;
    }

    private Label getCountDaysLastProject() {
        // процессинг последнего проекта
        OpenPosition lastOpenPosition = iteractionList.get(0).getVacancy();
        IteractionList firstIteractionOnProject = null;
        IteractionList lastItercationOnProject = iteractionList.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(iteractionList.get(0).getDateIteraction());
        calendar.add(Calendar.MONTH, 1);

        Calendar calendar1 = Calendar.getInstance();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(iteractionList.get(0).getDateIteraction());
        calendar2.add(Calendar.MONTH, 3);

        LocalDate d1 = LocalDate.now();
        LocalDate d2 = iteractionList.get(0).getDateIteraction().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        for(int i = iteractionList.size() ; i > 0 ; i--) {
            if(iteractionList.get(i - 1).equals(iteractionList.get(0))) {
                lastItercationOnProject = iteractionList.get(i - 1);
                break;
            }
        }

        for (IteractionList iteraction : iteractionList) {
            if(iteraction.getVacancy() != null) {
                if (lastOpenPosition.equals(iteraction.getVacancy())) {
                    firstIteractionOnProject = iteraction;
                    break;
                }
            }
        }

        if (firstIteractionOnProject != null) {
            Label countDaysOnLastProject = uiComponents.create(Label.NAME);
            countDaysOnLastProject.setStyleName(getStyleOnTime(calendar, calendar1, calendar1));
//            LocalDate d3 = firstIteractionOnProject.getDateIteraction().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate d3 = lastItercationOnProject.getDateIteraction().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long daysOnProject = ChronoUnit.DAYS.between(d3, d1);

            countDaysOnLastProject.setValue("На последнем проекте " + daysOnProject + " дней");
            countDaysOnLastProject.setDescription(DESC_DAYS_ON_LAST_PROJECT);

            statisticsHLabelBox.add(countDaysOnLastProject);

            return countDaysOnLastProject;
        } else {
            return null;
        }
    }

    private Label getStartIteractionDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

        Label startIteraction = uiComponents.create(Label.NAME);
        startIteraction.setValue("Даты: c "
                + simpleDateFormat.format(iteractionList.get(iteractionList.size() - 1).getDateIteraction())
                + " по "
                + simpleDateFormat.format(iteractionList.get(0).getDateIteraction()));
        startIteraction.setAlignment(Component.Alignment.MIDDLE_LEFT);
        startIteraction.setDescription(DESC_DATE_ITERACTION);
        startIteraction.setStyleName("button_table_green");

        return startIteraction;
    }

    private Label lastIterDayCount() {
        Label lastItercationDayCount = uiComponents.create(Label.NAME);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(iteractionList.get(0).getDateIteraction());
        calendar.add(Calendar.MONTH, 1);

        Calendar calendar1 = Calendar.getInstance();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(iteractionList.get(0).getDateIteraction());
        calendar2.add(Calendar.MONTH, 3);

        LocalDate d1 = LocalDate.now();
        LocalDate d2 = iteractionList.get(0).getDateIteraction().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(d2, d1) - 30;

        if (days > 0) {
            lastItercationDayCount.setValue("Свободен " + days + " дней");
            lastItercationDayCount.setAlignment(Component.Alignment.MIDDLE_LEFT);
            lastItercationDayCount.setStyleName(getStyleOnTime(calendar, calendar1, calendar2));
            lastItercationDayCount.setDescription(DESC_DAYS_LAST_ITERCATION);
        }

        return lastItercationDayCount;
    }

    private Label getActivityCandidatePeriod() {
        Label activityCanidate = uiComponents.create(Label.NAME);
        String style = "";
        String activity = "";

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(iteractionList.get(0).getDateIteraction());
        calendar.add(Calendar.MONTH, 1);

        Calendar calendar1 = Calendar.getInstance();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(iteractionList.get(0).getDateIteraction());
        calendar2.add(Calendar.MONTH, 3);

        style = getStyleOnTime(calendar, calendar1, calendar2);

        if (calendar.after(calendar1)) {
            if (!iteractionList.get(0).getRecrutier().equals(userSession.getUser())) {
                activity = "В работе";
            } else {
                activity = "В работе";
            }
        } else {
            if (calendar1.after(calendar2)) {
                activity = "СВОБОДЕН";
            } else {
                activity = "СВОБОДЕН";
            }
        }

        activityCanidate.setValue(activity);
        activityCanidate.setStyleName(style);

        return activityCanidate;
    }

    private String getStyleOnTime(Calendar cal, Calendar cal1, Calendar cal2) {
        String style;

        if (cal.after(cal1)) {
            if (!iteractionList.get(0).getRecrutier().equals(userSession.getUser())) {
                style = "button_table_red";
            } else {
                style = "button_table_yellow";
            }
        } else {
            if (cal1.after(cal2)) {
                style = "button_table_gray";
            } else {
                style = "button_table_green";

            }
        }

        return style;
    }
}