package com.company.itpearls.web.widgets.candidates;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.*;

@UiController("itpearls_MyActiveCandidatesDashboard")
@UiDescriptor("my-active-candidates-dashboard.xml")
@DashboardWidget(name = "My Active Candidateses")
public class MyActiveCandidatesDashboard extends ScreenFragment {
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionContainer<IteractionList> iteractionListsDc;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScrollBoxLayout candidatesScrollBox;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataManager dataManager;
    @Inject
    private RadioButtonGroup candidateRadioButtonGroup;

    private Set<OpenPosition> caseClosedOpenPosition;
    private Set<OpenPosition> processedOpenPosition;
    private Set<OpenPosition> opportunityOpenPosition;
    private Set<OpenPosition> wasOrNowOpenPosition = new HashSet<>();
    private ScrollBoxLayout scrollBoxLayout;
    private Boolean generatedWidget = false;
    private int candidatesCount = 0;
    @Inject
    private Label<String> notCandidatesLabel;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private CheckBox excludeCheckBox;

    final static String QUERY_EXCLUDE_CANDIDATES = "select e from itpearls_OpenPosition e " +
            "where e.positionType = :positionType and not e.openClose = true " +
            "and not e in (select f.vacancy from itpearls_IteractionList f where f.candidate = :candidate)";

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        excludeCheckBox.setValue(false);
        initInteractionListDataContainer();
        initRadioButton();
        initCandidatesList();
        initNoCandidatesLabel();
    }

    @Subscribe("excludeCheckBox")
    public void onExcludeCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        reinitInteractionListDataContainer(getPeriodRadioButton());
    }

    private void initRadioButton() {
        Map<String, Integer> onlyOpenedPositionMap = new LinkedHashMap<>();

        onlyOpenedPositionMap.put("за 3 дня", 0);
        onlyOpenedPositionMap.put("за неделю", 1);
        onlyOpenedPositionMap.put("за месяц", 2);

        candidateRadioButtonGroup.setOptionsMap(onlyOpenedPositionMap);
        candidateRadioButtonGroup.setValue(1);

        candidateRadioButtonGroup.addValueChangeListener(e -> {
            reinitInteractionListDataContainer(getPeriodRadioButton());
            initNoCandidatesLabel();
        });

    }

    int getPeriodRadioButton() {
        int period = 0;

        switch ((int) candidateRadioButtonGroup.getValue()) {
            case 0:
                period = 3;
                break;
            case 1:
                period = 7;
                break;
            case 2:
                period = 30;
                break;
            default:
                period = 3;
                break;
        }

        return period;
    }

    private void initCandidatesList() {
        Set<JobCandidate> jobCandidateSet = new HashSet<>();
        candidatesCount = jobCandidateSet.size();

        for (IteractionList interactionList : iteractionListsDc.getItems()) {
            if (excludeCheckBox.getValue()) {
                List<OpenPosition> whatToOfferCandidate = dataManager.load(OpenPosition.class)
                        .query(QUERY_EXCLUDE_CANDIDATES)
                        .parameter("positionType", interactionList.getCandidate().getPersonPosition())
                        .parameter("candidate", interactionList.getCandidate())
                        .view("openPosition-view")
                        .list();

                if (whatToOfferCandidate.size() > 0) {
                    jobCandidateSet.add(interactionList.getCandidate());
                }
            } else {
                jobCandidateSet.add(interactionList.getCandidate());
            }
        }

        for (JobCandidate jobCandidate : jobCandidateSet) {
            HBoxLayout hBoxLayout = uiComponents.create(HBoxLayout.class);
            hBoxLayout.setWidthAuto();
            hBoxLayout.setHeightAuto();
            hBoxLayout.setSpacing(true);

            LinkButton candidateLinkButton = uiComponents.create(LinkButton.class);
            candidateLinkButton.setAlignment(Component.Alignment.MIDDLE_LEFT);
            candidateLinkButton.setCaption(jobCandidate.getFullName()
                    + " / "
                    + jobCandidate.getPersonPosition().getPositionRuName());

            candidateLinkButton.addClickListener(e -> {
                screenBuilders.editor(JobCandidate.class, this)
                        .editEntity(jobCandidate)
                        .build()
                        .show();
            });

            PopupButton candidateActioButton = uiComponents.create(PopupButton.class);
            candidateActioButton.setIconFromSet(CubaIcon.ADD_ACTION);
            candidateActioButton.setAlignment(Component.Alignment.MIDDLE_CENTER);

            candidateActioButton.addAction(new BaseAction("closeAllProcesses")
                    .withCaption(messageBundle.getMessage("msgCloseAllProcesses"))
                    .withDescription(messageBundle.getMessage("msgCloseAllProcessesDescription"))
                    .withHandler(actionPerformedEvent -> closeAllProcesses()));

            caseClosedOpenPosition = getCaseClosedOpenPosition(jobCandidate);
            processedOpenPosition = getProcessedOpenPosition(jobCandidate);

            wasOrNowOpenPosition.addAll(caseClosedOpenPosition);
            wasOrNowOpenPosition.addAll(processedOpenPosition);

            opportunityOpenPosition = getOpportunityOpenPosition(jobCandidate);

            hBoxLayout.add(candidateLinkButton);
            // /* TO-DO как то доработать чтоб была кнопка удалить из рассмотрения */
            // hBoxLayout.add(candidateActioButton);

            scrollBoxLayout = uiComponents.create(ScrollBoxLayout.class);
            scrollBoxLayout.setWidthAuto();
            scrollBoxLayout.setSpacing(true);
            scrollBoxLayout.setOrientation(HasOrientation.Orientation.HORIZONTAL);
            scrollBoxLayout.setScrollBarPolicy(ScrollBoxLayout.ScrollBarPolicy.HORIZONTAL);

            createLabels(opportunityOpenPosition, "text-block-gradient-green",
                    messageBundle.getMessage("msgCanSendCandidate"));
            createLabels(processedOpenPosition, "text-block-gradient-yellow",
                    messageBundle.getMessage("msgCandidateIsWork"));
            createLabels(caseClosedOpenPosition, "text-block-gradient-red",
                    messageBundle.getMessage("msgCandidateisEndProcess"));

            hBoxLayout.add(scrollBoxLayout);

            candidatesScrollBox.add(hBoxLayout);
        }

        generatedWidget = true;
    }

    private void closeAllProcesses() {
    }

    private void createLabels(Set<OpenPosition> openPositions, String style, String description) {
        for (OpenPosition openPosition : openPositions) {
            LinkButton retLinkButton = uiComponents.create(LinkButton.class);
            if (openPosition.getProjectName().getProjectName().length() > 20) {
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName().substring(0, 20));
            } else {
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName());
            }

            retLinkButton.setDescription(description + "\n\n" + openPosition.getVacansyName());
            retLinkButton.setWidthAuto();
            retLinkButton.setStyleName(style);
            retLinkButton.setAlignment(Component.Alignment.MIDDLE_LEFT);

            scrollBoxLayout.add(retLinkButton);
        }
    }

    private Set<OpenPosition> getProcessedOpenPosition(JobCandidate jobCandidate) {
        String QUERY_CASE_OPEN_POSITION
                = "select e "
                + "from itpearls_IteractionList e "
                + "where not (e.iteractionType.signEndCase = true) "
                + "and e.candidate = :candidate "
                + "and not (e.vacancy.openClose = true)";
        //+ "and not (e.vacancy.projectName.defaultProject = true)";

        List<IteractionList> caseClosedInteraction = dataManager.load(IteractionList.class)
                .query(QUERY_CASE_OPEN_POSITION)
                .parameter("candidate", jobCandidate)
                .view("iteractionList-view")
                .list();

        Set<OpenPosition> retOpenPosition = new HashSet<>();

        for (IteractionList iteractionList : caseClosedInteraction) {
            Boolean flag = false;
            for (OpenPosition op : caseClosedOpenPosition) {
                if (op.equals(iteractionList.getVacancy())) {
                    flag = true;
                    break;
                }
            }

            if (!flag)
                retOpenPosition.add(iteractionList.getVacancy());
        }

        return retOpenPosition;
    }

    private Set<OpenPosition> getOpportunityOpenPosition(JobCandidate jobCandidate) {
        String QUERY_OPPORTUNITY = "select e from itpearls_OpenPosition e " +
                "where e.positionType = :positionType and not e.openClose = true";

        List<OpenPosition> opportunityOpenPosition = dataManager.load(OpenPosition.class)
                .query(QUERY_OPPORTUNITY)
                .parameter("positionType", jobCandidate.getPersonPosition())
                .view("openPosition-view")
                .list();

        Set<OpenPosition> retOpenPosition = new HashSet<>();

        for (OpenPosition openPosition : opportunityOpenPosition) {
            Boolean flag = false;
            for (OpenPosition ccOP : wasOrNowOpenPosition) {
                if (openPosition.equals(ccOP)) {
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                retOpenPosition.add(openPosition);
            }
        }

        return retOpenPosition;
    }

    private Set<OpenPosition> getCaseClosedOpenPosition(JobCandidate jobCandidate) {
        String QUERY_CASE_CLOSED_OPEN_POSITION
                = "select e " +
                "from itpearls_IteractionList e " +
                "where e.iteractionType.signEndCase = true and e.candidate = :candidate";

        List<IteractionList> caseClosedInteraction = dataManager.load(IteractionList.class)
                .query(QUERY_CASE_CLOSED_OPEN_POSITION)
                .parameter("candidate", jobCandidate)
                .view("iteractionList-view")
                .list();

        Set<OpenPosition> retOpenPosition = new HashSet<>();

        for (IteractionList iteractionList : caseClosedInteraction) {
            retOpenPosition.add(iteractionList.getVacancy());
        }

        return retOpenPosition;
    }

    @Subscribe("candidateRadioButtonGroup")
    public void onCandidateRadioButtonGroupValueChange(HasValue.ValueChangeEvent event) {
        reinitInteractionListDataContainer((int) event.getValue());
    }

    private void reinitInteractionListDataContainer(int daysBetween) {
        if (generatedWidget) {
            iteractionListsDl.setParameter("daysBetween", daysBetween); // 7 дней
            iteractionListsDl.load();

            clearCandidatesList();
            initCandidatesList();
        }

        initNoCandidatesLabel();
    }

    private void initNoCandidatesLabel() {
        notCandidatesLabel.setVisible(iteractionListsDc.getItems().size() == 0);
    }

    private void clearCandidatesList() {
        if (caseClosedOpenPosition != null) {
            if (caseClosedOpenPosition.size() > 0)
                caseClosedOpenPosition.clear();
        }

        if (processedOpenPosition != null) {
            if (processedOpenPosition.size() > 0)
                processedOpenPosition.clear();
        }

        if (opportunityOpenPosition != null) {
            if (opportunityOpenPosition.size() > 0)
                opportunityOpenPosition.clear();
        }

        if (wasOrNowOpenPosition != null) {
            if (wasOrNowOpenPosition.size() > 0)
                wasOrNowOpenPosition.clear();
        }

        candidatesScrollBox.removeAll();

        generatedWidget = false;
    }

    private void initInteractionListDataContainer() {
        iteractionListsDl.setParameter("daysBetween", 7); // 7 дней
        iteractionListsDl.setParameter("recrutier", userSession.getUser().getLogin());
        iteractionListsDl.removeParameter("personPosition");

        iteractionListsDl.load();
    }
}