package com.company.itpearls.web.widgets.candidates;

import com.company.itpearls.entity.*;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
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
    @Inject
    private Label<String> notCandidatesLabel;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private CheckBox excludeCheckBox;
    @Inject
    private Metadata metadata;
    @Inject
    private Dialogs dialogs;

    private Set<OpenPosition> caseClosedOpenPosition;
    private Set<OpenPosition> processedOpenPosition;
    private Set<OpenPosition> opportunityOpenPosition;
    private Set<OpenPosition> wasOrNowOpenPosition = new HashSet<>();
    private ScrollBoxLayout scrollBoxLayout;
    private Boolean generatedWidget = false;
    private int candidatesCount = 0;

    final static String QUERY_MY_CANIDATE_EXCLUDE = "select e from itpearls_MyActiveCandidateExclude e " +
            "where e.jobCandidate = :jobCandidate and e.user = :user";

    final static String QUERY_EXCLUDE_CANDIDATES = "select e from itpearls_OpenPosition e " +
            "where e.positionType = :positionType and not e.openClose = true " +
            "and not e in (select f.vacancy from itpearls_IteractionList f where f.candidate = :candidate)";
    @Inject
    private GroupBoxLayout excludeCandidatesLineGroupBox;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        excludeCheckBox.setValue(false);
        initInteractionListDataContainer();
        initRadioButton();
        initCandidatesList();
        initNoCandidatesLabel();
        initCheckBox();
    }

    private void initCheckBox() {
        excludeCheckBox.setValue(true);
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
        Map<JobCandidate, Map.Entry<Integer, Integer>> candidateProjectMap = new HashMap<JobCandidate, Map.Entry<Integer, Integer>>();

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
            HBoxLayout candidateLineHBoxLayout =
                    uiComponents.create(HBoxLayout.class);
            candidateLineHBoxLayout.setWidthAuto();
            candidateLineHBoxLayout.setHeightAuto();
            candidateLineHBoxLayout.setSpacing(true);

            HBoxLayout jobCandidateNameHBox = jobCandidateNameHBox(jobCandidate);

            caseClosedOpenPosition = getCaseClosedOpenPosition(jobCandidate);
            processedOpenPosition = getProcessedOpenPosition(jobCandidate);

            wasOrNowOpenPosition.addAll(caseClosedOpenPosition);
            wasOrNowOpenPosition.addAll(processedOpenPosition);

            opportunityOpenPosition = getOpportunityOpenPosition(jobCandidate);

            scrollBoxLayout = uiComponents.create(ScrollBoxLayout.class);
            scrollBoxLayout.setWidthAuto();
            scrollBoxLayout.setSpacing(true);
            scrollBoxLayout.setOrientation(HasOrientation.Orientation.HORIZONTAL);
            scrollBoxLayout.setScrollBarPolicy(ScrollBoxLayout.ScrollBarPolicy.HORIZONTAL);

            Integer labelCounter = 0;
            Integer labelExcludeCounter = 0;
            Integer labelCloseAllProjectsCounter = 0;

            List<Integer> labelCounterOpportunity = new ArrayList<>();
            List<Integer> labelCounterProcessed = new ArrayList<>();
            List<Integer> labelCounterCaseClosed = new ArrayList<>();

            labelCounterOpportunity = createLabels(jobCandidate, opportunityOpenPosition, "text-block-gradient-green",
                    messageBundle.getMessage("msgCanSendCandidate"));
            labelCounterProcessed = createLabels(jobCandidate, processedOpenPosition, "text-block-gradient-yellow",
                    messageBundle.getMessage("msgCandidateIsWork"));
            labelCounterCaseClosed = createLabels(jobCandidate, caseClosedOpenPosition, "text-block-gradient-red",
                    messageBundle.getMessage("msgCandidateisEndProcess"));

            labelCounter = labelCounterOpportunity.get(0)
                    + labelCounterProcessed.get(0)
                    + labelCounterCaseClosed.get(0);
            labelExcludeCounter = labelCounterOpportunity.get(1)
                    + labelCounterProcessed.get(1)
                    + labelCounterCaseClosed.get(1);
            labelCloseAllProjectsCounter = labelCounterOpportunity.get(2)
                    + labelCounterProcessed.get(2)
                    + labelCounterCaseClosed.get(2);

            candidateProjectMap.put(jobCandidate, new AbstractMap.SimpleEntry<>(labelCounter, labelExcludeCounter));

            HBoxLayout jobCandidateExcludeNameHBox = jobCandidateExcludeNameHBox(jobCandidate);

            if (labelCloseAllProjectsCounter != 0) {
                candidateLineHBoxLayout.setVisible(false);
                jobCandidateExcludeNameHBox.setVisible(true);
            } else {
                if (labelCounter > labelExcludeCounter) {
                    candidateLineHBoxLayout.setVisible(true);
                    jobCandidateExcludeNameHBox.setVisible(false);
                } else {
                    candidateLineHBoxLayout.setVisible(false);
                    jobCandidateExcludeNameHBox.setVisible(true);
                }
            }

            candidateLineHBoxLayout.add(jobCandidateNameHBox);
            candidateLineHBoxLayout.add(scrollBoxLayout);
            candidatesScrollBox.add(candidateLineHBoxLayout);
            excludeCandidatesLineGroupBox.add(jobCandidateExcludeNameHBox);

            candidateProjectMap.put(jobCandidate, new AbstractMap.SimpleEntry<>(labelCounter, labelExcludeCounter));
        }

        generatedWidget = true;
    }

    private HBoxLayout jobCandidateExcludeNameHBox(JobCandidate jobCandidate) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);

        LinkButton candidateLinkButton = uiComponents.create(LinkButton.class);
        candidateLinkButton.setStyleName("h4");
        candidateLinkButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        candidateLinkButton.setCaption(jobCandidate.getFullName()
                + " / "
                + jobCandidate.getPersonPosition().getPositionRuName());

        candidateLinkButton.addClickListener(e -> {
            screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(jobCandidate)
                    .build()
                    .show();
        });

        LinkButton undoRemoveCandidateFromConsideration = uiComponents.create(LinkButton.class);
        undoRemoveCandidateFromConsideration.setIconFromSet(CubaIcon.UNDO);
        undoRemoveCandidateFromConsideration.setAlignment(Component.Alignment.MIDDLE_CENTER);
        undoRemoveCandidateFromConsideration.setDescription(
                messageBundle.getMessage("msgUndoRemoveCandidateFromConsiderationDesc"));

        undoRemoveCandidateFromConsideration.addClickListener(event -> {
            undoRemoveCandidateFromConsideration(jobCandidate);
        });

        retHbox.add(candidateLinkButton);
        retHbox.add(undoRemoveCandidateFromConsideration);

        return retHbox;
    }

    private HBoxLayout jobCandidateNameHBox(JobCandidate jobCandidate) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);

        LinkButton candidateLinkButton = uiComponents.create(LinkButton.class);
        candidateLinkButton.setStyleName("h4");
        candidateLinkButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        candidateLinkButton.setCaption(jobCandidate.getFullName()
                + " / "
                + jobCandidate.getPersonPosition().getPositionRuName());

        candidateLinkButton.addClickListener(e -> {
            screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(jobCandidate)
                    .build()
                    .show();
        });

        LinkButton removeCandidateFromConsideration = uiComponents.create(LinkButton.class);
        removeCandidateFromConsideration.setIconFromSet(CubaIcon.EXCLUDE_ACTION);
        removeCandidateFromConsideration.setAlignment(Component.Alignment.MIDDLE_CENTER);
        removeCandidateFromConsideration.setDescription(messageBundle.getMessage("msgRemoveCandidateFromConsideration"));
        removeCandidateFromConsideration.addClickListener(event -> {
            dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                    .withMessage(messageBundle.getMessage("msgRemoveFromAllConsideration"))
                    .withType(Dialogs.MessageType.WARNING)
                    .withCaption(messageBundle.getMessage("mainmsgWarning"))
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                    .withHandler(e -> {
                                        closeAllProcesses(jobCandidate);
                                        clearCandidatesList();
                                        initCandidatesList();
                                    }),
                            new DialogAction(DialogAction.Type.NO))
                    .show();
        });

        LinkButton undoRemoveCandidateFromConsideration = uiComponents.create(LinkButton.class);
        undoRemoveCandidateFromConsideration.setIconFromSet(CubaIcon.UNDO);
        undoRemoveCandidateFromConsideration.setAlignment(Component.Alignment.MIDDLE_CENTER);
        undoRemoveCandidateFromConsideration.setDescription(
                messageBundle.getMessage("msgUndoRemoveCandidateFromConsiderationDesc"));
        undoRemoveCandidateFromConsideration.addClickListener(event -> {
            undoRemoveCandidateFromConsideration(jobCandidate);
        });

        retHbox.add(candidateLinkButton);
        retHbox.add(undoRemoveCandidateFromConsideration);
        retHbox.add(removeCandidateFromConsideration);

        return retHbox;
    }

    private void undoRemoveCandidateFromConsideration(JobCandidate jobCandidate) {
        List<MyActiveCandidateExclude> myActiveCandidateExcludes = dataManager.load(MyActiveCandidateExclude.class)
                .query(QUERY_MY_CANIDATE_EXCLUDE)
                .parameter("jobCandidate", jobCandidate)
                .parameter("user", userSession.getUser())
                .view("myActiveCandidateExclude-view")
                .list();

        for (MyActiveCandidateExclude myActiveCandidateExclude : myActiveCandidateExcludes) {
            dataManager.remove(myActiveCandidateExclude);
        }

        reinitInteractionListDataContainer();
    }

    private void closeAllProcesses(JobCandidate jobCandidate) {
        MyActiveCandidateExclude myActiveCandidateExclude = metadata.create(MyActiveCandidateExclude.class);
        myActiveCandidateExclude.setUser((ExtUser) userSession.getUser());
        myActiveCandidateExclude.setJobCandidate(jobCandidate);

        dataManager.commit(myActiveCandidateExclude);
    }

    private List<Integer> createLabels(JobCandidate jobCandidate, Set<OpenPosition> openPositions, String style, String description) {
        List<Integer> retList = new ArrayList<>();

        Integer labelCounter = 0;
        Integer excludeCounter = 0;
        Integer flagCandidateCloseAllProject = 0;

        List<MyActiveCandidateExclude> myActiveCandidateExclude = dataManager.load(MyActiveCandidateExclude.class)
                .query(QUERY_MY_CANIDATE_EXCLUDE)
                .parameter("jobCandidate", jobCandidate)
                .parameter("user", userSession.getUser())
                .view("myActiveCandidateExclude-view")
                .list();

        for (MyActiveCandidateExclude mace : myActiveCandidateExclude) {
            if (mace.getOpenPosition() == null) {
                flagCandidateCloseAllProject = 1;
                break;
            }
        }

        for (OpenPosition openPosition : openPositions) {
            HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
            retHBox.setStyleName(style);
            retHBox.setSpacing(true);

            labelCounter++;
            LinkButton retLinkButton = uiComponents.create(LinkButton.class);
            if (openPosition.getProjectName().getProjectName().length() > 20) {
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName().substring(0, 20));
            } else {
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName());
            }

            retLinkButton.setDescription(description + "\n\n" + openPosition.getVacansyName());
            retLinkButton.setWidthAuto();
            retLinkButton.setAlignment(Component.Alignment.MIDDLE_LEFT);

            LinkButton closeInprocessLinkButton = uiComponents.create(LinkButton.class);
            closeInprocessLinkButton.setIcon(CubaIcon.EXCLUDE_ACTION.source());
            closeInprocessLinkButton.setStyleName("pic-center-large-black");
            closeInprocessLinkButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
            closeInprocessLinkButton.addClickListener(event -> {

                dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                        .withMessage(messageBundle.getMessage("msgRemoveFromConsideration"))
                        .withType(Dialogs.MessageType.WARNING)
                        .withCaption(messageBundle.getMessage("mainmsgWarning"))
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                        .withHandler(e -> {
                                            MyActiveCandidateExclude mace = metadata.create(MyActiveCandidateExclude.class);

                                            mace.setJobCandidate(jobCandidate);
                                            mace.setOpenPosition(openPosition);
                                            mace.setUser((ExtUser) userSession.getUser());

                                            dataManager.commit(mace);

                                            retHBox.setVisible(false);
                                        }),
                                new DialogAction(DialogAction.Type.NO))
                        .show();

            });

            retHBox.add(retLinkButton);
            retHBox.add(closeInprocessLinkButton);

            scrollBoxLayout.add(retHBox);

            Boolean excludeProject = false;

            for (MyActiveCandidateExclude mace : myActiveCandidateExclude) {
                if (openPosition.equals(mace.getOpenPosition())) {
                    excludeProject = true;
                    excludeCounter++;
                    break;
                }
            }

            retHBox.setVisible(!excludeProject);
        }
        // блок с "закрытыми" кандидатами
        retList.add(labelCounter);
        retList.add(excludeCounter);
        retList.add(flagCandidateCloseAllProject);

        return retList;
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
        final String QUERY_OPPORTUNITY = "select e from itpearls_OpenPosition e " +
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

    private void reinitInteractionListDataContainer() {
        clearCandidatesList();
        initCandidatesList();

        initNoCandidatesLabel();
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
        excludeCandidatesLineGroupBox.removeAll();

        generatedWidget = false;
    }

    private void initInteractionListDataContainer() {
        iteractionListsDl.setParameter("daysBetween", 7); // 7 дней
        iteractionListsDl.setParameter("recrutier", userSession.getUser().getLogin());
        iteractionListsDl.removeParameter("personPosition");

        iteractionListsDl.load();
    }
}