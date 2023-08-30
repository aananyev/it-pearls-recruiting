package com.company.itpearls.web.widgets.candidates;

import com.company.itpearls.entity.*;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

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

    @WidgetParam
    @WindowParam
    protected Boolean detailMode;

    @WidgetParam
    @WindowParam
    protected Boolean simpleMode;

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
    @Inject
    private CheckBox cardDetailCheckBox;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        excludeCheckBox.setValue(false);
        initInteractionListDataContainer();
        initRadioButton();
        initCandidatesList();
        initNoCandidatesLabel();
        initCheckBox();
        initSimpleMode();
        initDetailMode();
    }

    private void initDetailMode() {
        if (detailMode != null) {
            cardDetailCheckBox.setValue(detailMode);
        } else {
            cardDetailCheckBox.setValue(false);
        }
    }

    private void initSimpleMode() {
        if (simpleMode != null) {
            if (simpleMode) {
                cardDetailCheckBox.setVisible(false);
                excludeCheckBox.setVisible(false);
            } else {
                cardDetailCheckBox.setVisible(true);
                excludeCheckBox.setVisible(true);
            }
        } else {
            cardDetailCheckBox.setVisible(true);
            excludeCheckBox.setVisible(true);
        }
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
            HBoxLayout candidateLineHBoxLayout = createCandidateLineHBoxLayout(jobCandidate);
            HBoxLayout jobCandidateNameHBox = jobCandidateNameHBox(jobCandidate);

            caseClosedOpenPosition = getCaseClosedOpenPosition(jobCandidate);
            processedOpenPosition = getProcessedOpenPosition(jobCandidate);

            wasOrNowOpenPosition.addAll(caseClosedOpenPosition);
            wasOrNowOpenPosition.addAll(processedOpenPosition);

            opportunityOpenPosition = getOpportunityOpenPosition(jobCandidate);
            scrollBoxLayout = createScrollBoxLayout(jobCandidate);

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

    private ScrollBoxLayout createScrollBoxLayout(JobCandidate jobCandidate) {
        scrollBoxLayout = uiComponents.create(ScrollBoxLayout.class);
        scrollBoxLayout.setWidthAuto();
        scrollBoxLayout.setSpacing(true);
        scrollBoxLayout.setOrientation(HasOrientation.Orientation.HORIZONTAL);
        scrollBoxLayout.setScrollBarPolicy(ScrollBoxLayout.ScrollBarPolicy.HORIZONTAL);

        return scrollBoxLayout;
    }

    private HBoxLayout createCandidateLineHBoxLayout(JobCandidate jobCandidate) {
        HBoxLayout candidateLineHBoxLayout =
                uiComponents.create(HBoxLayout.class);
        candidateLineHBoxLayout.setWidthAuto();
        candidateLineHBoxLayout.setHeightFull();
        candidateLineHBoxLayout.setAlignment(Component.Alignment.MIDDLE_LEFT);
        candidateLineHBoxLayout.setSpacing(true);

        return candidateLineHBoxLayout;
    }

    private HBoxLayout jobCandidateExcludeNameHBox(JobCandidate jobCandidate) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retHbox.setHeightFull();

        LinkButton candidateLinkButton = uiComponents.create(LinkButton.class);
        candidateLinkButton.setStyleName("h4");
        candidateLinkButton.setHeightFull();
        candidateLinkButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        candidateLinkButton.setCaption(jobCandidate.getFullName()
                + " / "
                + jobCandidate.getPersonPosition().getPositionRuName()
                + " / "
                + jobCandidate.getCityOfResidence().getCityRuName());

        candidateLinkButton.addClickListener(e -> {
            screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(jobCandidate)
                    .build()
                    .show();
        });

        LinkButton undoRemoveCandidateFromConsideration = uiComponents.create(LinkButton.class);
        undoRemoveCandidateFromConsideration.setIconFromSet(CubaIcon.UNDO);
        undoRemoveCandidateFromConsideration.setHeightFull();
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
        retHbox.setHeightFull();
        retHbox.setAlignment(Component.Alignment.MIDDLE_LEFT);

        LinkButton candidateLinkButton = uiComponents.create(LinkButton.class);
        candidateLinkButton.setStyleName("h4");
        candidateLinkButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        candidateLinkButton.setCaption(jobCandidate.getFullName()
                + " / "
                + jobCandidate.getPersonPosition().getPositionRuName()
                + " / "
                + jobCandidate.getCityOfResidence().getCityRuName());

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
            VBoxLayout internalVBox = createInternalVBox(openPosition);
            HBoxLayout internalHBox = createInternalHBox(openPosition);
            VBoxLayout retHBox = createRetHBox(openPosition, style);

            labelCounter++;

            Label newVacanciesLabel = createNewVacanciesLabel(openPosition);
            Image projectLogoImage = createProjectLogoImage(openPosition);
            LinkButton retLinkButton = createRetLinkButton(openPosition, description);
            LinkButton closeInprocessLinkButton = createCloseInprocessLinkButton(openPosition,
                    jobCandidate,
                    retHBox);
// блок детализации
            Label projectDetailLabel = createProjectDetailLabel(openPosition);

            String remoteWorkStr = getRemoteWorkStr(openPosition);
            HBoxLayout remoteWorkHBox = setDetailValueLabel(messageBundle.getMessage("msgRemoteWork"), remoteWorkStr);

            HBoxLayout cityOfVacancy = setDetailValueLabel(messageBundle.getMessage("msgCity"),
                    openPosition.getCityPosition() != null
                            ? openPosition.getCityPosition().getCityRuName()
                            : messageBundle.getMessage("msgUndefinedCity"));
            HBoxLayout salaryMinHBox = setDetailValueLabel(messageBundle.getMessage("msgSalaryMin"),
                    openPosition.getSalaryMin() != null
                            ? openPosition.getSalaryMin().toString() : "");
            HBoxLayout salaryMaxHBox = setDetailValueLabel(messageBundle.getMessage("msgSalaryMax"),
                    openPosition.getSalaryMax() != null
                            ? openPosition.getSalaryMax().toString() : "");

            setProjectVisibleDetails(retHBox,
                    internalVBox,
                    openPosition,
                    retLinkButton,
                    cityOfVacancy,
                    remoteWorkHBox,
                    salaryMinHBox,
                    salaryMaxHBox);

            internalHBox.add(newVacanciesLabel);
            internalHBox.add(projectLogoImage);
            internalHBox.add(retLinkButton);
            internalHBox.expand(retLinkButton);
            internalHBox.add(closeInprocessLinkButton);

            internalVBox.add(projectDetailLabel);
            internalVBox.add(remoteWorkHBox);
            internalVBox.add(cityOfVacancy);
            internalVBox.add(salaryMinHBox);
            internalVBox.add(salaryMaxHBox);

            retHBox.add(internalHBox);
            retHBox.add(internalVBox);

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

    private String getRemoteWorkStr(OpenPosition openPosition) {
        StringBuffer remoteWorkStr = new StringBuffer();

        switch (openPosition.getRemoteWork()) {
            case -1:
                remoteWorkStr.append(messageBundle.getMessage("msgUndefined"));
                break;
            case 0:
                remoteWorkStr.append(messageBundle.getMessage("msgWorkInOffice"));
                break;
            case 1:
                remoteWorkStr.append(messageBundle.getMessage("msgRemoteWork"));
                break;
            case 2:
                remoteWorkStr.append(messageBundle.getMessage("msgHybridWork"));
                break;
        }

        return remoteWorkStr.toString();
    }

    private VBoxLayout createRetHBox(OpenPosition openPosition, String style) {
        VBoxLayout retHBox = uiComponents.create(VBoxLayout.class);
        retHBox.setStyleName(style);
        retHBox.setSpacing(true);

        return retHBox;
    }

    private HBoxLayout createInternalHBox(OpenPosition openPosition) {
        HBoxLayout internalHBox = uiComponents.create(HBoxLayout.class);
        internalHBox.setWidthFull();
        internalHBox.setSpacing(false);

        return internalHBox;
    }

    private VBoxLayout createInternalVBox(OpenPosition openPosition) {
        VBoxLayout internalVBox = uiComponents.create(VBoxLayout.class);
        internalVBox.setHeightAuto();
        internalVBox.setSpacing(true);

        return internalVBox;
    }

    private Label createProjectDetailLabel(OpenPosition openPosition) {
        Label projectDetailLabel = uiComponents.create(Label.class);
        projectDetailLabel.setValue(openPosition.getVacansyName());
        projectDetailLabel.setWidthAuto();
        projectDetailLabel.setHeightAuto();
        projectDetailLabel.setStyleName("detail-candidate-card-wordwrap");
        projectDetailLabel.setDescriptionAsHtml(true);
        projectDetailLabel.setDescription(openPosition.getVacansyName() + "<br><br>" + openPosition.getComment());

        return projectDetailLabel;
    }

    private Label createNewVacanciesLabel(OpenPosition openPosition) {
        Label newVacanciesLabel = uiComponents.create(Label.class);
        newVacanciesLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        gregorianCalendar.add(Calendar.DAY_OF_MONTH, -3);

        if (openPosition.getLastOpenDate() != null) {
            if (openPosition.getLastOpenDate().after(gregorianCalendar.getTime())) {
                newVacanciesLabel.setIconFromSet(CubaIcon.WARNING);
                newVacanciesLabel.setStyleName("label_button_red");
                newVacanciesLabel.setDescription(messageBundle.getMessage("msgOpenedLess3days"));
            } else {
                gregorianCalendar.setTime(new Date());
                gregorianCalendar.add(Calendar.DAY_OF_MONTH, -7);
                if (openPosition.getLastOpenDate().after(gregorianCalendar.getTime())) {
                    newVacanciesLabel.setIconFromSet(CubaIcon.WARNING);
                    newVacanciesLabel.setStyleName("label_button_orange");
                    newVacanciesLabel.setDescription(messageBundle.getMessage("msgOpenedLess7days"));
                } else {
                    gregorianCalendar.setTime(new Date());
                    gregorianCalendar.add(Calendar.MONTH, -1);

                    if (openPosition.getLastOpenDate().after(gregorianCalendar.getTime())) {
                        newVacanciesLabel.setIconFromSet(CubaIcon.WARNING);
                        newVacanciesLabel.setDescription(messageBundle.getMessage("msgOpenedLessMonth"));
                        newVacanciesLabel.setStyleName("label_button_green");
                    } else {
                        newVacanciesLabel.setIconFromSet(CubaIcon.WARNING);
                        newVacanciesLabel.setDescription(messageBundle.getMessage("msgOpenedMoreMonth"));
                        newVacanciesLabel.setStyleName("label_button_gray");
                    }
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

            newVacanciesLabel.setDescription(newVacanciesLabel.getDescription()
                    + "\n"
                    + messageBundle.getMessage("msgLastOpenDate")
                    + ": "
                    + sdf.format(openPosition.getLastOpenDate()));
        }

        return newVacanciesLabel;
    }

    private LinkButton createRetLinkButton(OpenPosition openPosition, String description) {
        LinkButton retLinkButton = uiComponents.create(LinkButton.class);
        if (!cardDetailCheckBox.getValue()) {
            if (openPosition.getProjectName().getProjectName().length() > 20) {
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName().substring(0, 20));
            } else {
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName());
            }

            retLinkButton.removeStyleName("detail-candidate-card-wordwrap");
        } else {
            retLinkButton.setCaption(openPosition.getProjectName().getProjectName());
            retLinkButton.setStyleName("detail-candidate-card-wordwrap");
        }

        retLinkButton.setDescription(description
                + "\n\n"
                + openPosition.getVacansyName()
                + "\n\n"
                + openPosition.getProjectName().getProjectOwner().getSecondName()
                + " "
                + openPosition.getProjectName().getProjectOwner().getFirstName());
        retLinkButton.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retLinkButton.setWidthFull();

        return retLinkButton;
    }

    private LinkButton createCloseInprocessLinkButton(OpenPosition openPosition, JobCandidate jobCandidate, VBoxLayout retHBox) {
        LinkButton closeInprocessLinkButton = uiComponents.create(LinkButton.class);
        closeInprocessLinkButton.setIcon(CubaIcon.EXCLUDE_ACTION.source());
        closeInprocessLinkButton.setStyleName("pic-center-large-black");
        closeInprocessLinkButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        closeInprocessLinkButton.setWidthAuto();
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

        return closeInprocessLinkButton;
    }

    private Image createProjectLogoImage(OpenPosition openPosition) {
        Image projectLogoImage = uiComponents.create(Image.class);
        projectLogoImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        projectLogoImage.setWidth("28px");
        projectLogoImage.setHeight("28px");
        projectLogoImage.setStyleName("icon-no-border-20px");
        projectLogoImage.setAlignment(Component.Alignment.MIDDLE_CENTER);
        projectLogoImage.setDescriptionAsHtml(true);

        if (openPosition.getProjectName() != null) {
            if (openPosition.getProjectName().getProjectDescription() != null) {
                projectLogoImage.setDescription("<h4>"
                        + openPosition.getProjectName().getProjectName()
                        + "</h4><br><br>"
                        + openPosition.getProjectName().getProjectDescription());
            }

            if (openPosition.getProjectName().getProjectLogo() != null) {
                projectLogoImage.setSource(FileDescriptorResource.class)
                        .setFileDescriptor(openPosition
                                .getProjectName()
                                .getProjectLogo());
            } else {
                projectLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");
            }
        }

        return projectLogoImage;
    }

    private HBoxLayout setDetailValueLabel(String leftLabelStr,
                                           String rightLabelStr) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();

        Label leftDetailLabel = uiComponents.create(Label.class);
        leftDetailLabel.setValue(leftLabelStr);
        leftDetailLabel.setWidthFull();
        leftDetailLabel.setStyleName("my-active-candidate-dashboard-detail-normal");
        leftDetailLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);

        Label rightDetailLabel = uiComponents.create(Label.class);
        rightDetailLabel.setValue(rightLabelStr);
        rightDetailLabel.setWidthAuto();
        rightDetailLabel.setStyleName("my-active-candidate-dashboard-detail-bold");
        rightDetailLabel.setAlignment(Component.Alignment.MIDDLE_RIGHT);

        retHBox.add(leftDetailLabel);
        retHBox.add(rightDetailLabel);
        retHBox.expand(leftDetailLabel);

        return retHBox;
    }

    private void setProjectVisibleDetails(VBoxLayout retHBox,
                                          VBoxLayout vBoxDetail,
                                          OpenPosition openPosition,
                                          LinkButton retLinkButton,
                                          HBoxLayout cityOfVacancy,
                                          HBoxLayout remoteWorkHBox,
                                          HBoxLayout salaryMinHBox,
                                          HBoxLayout salaryMaxHBox) {
        if (cardDetailCheckBox.getValue() != null ? cardDetailCheckBox.getValue() : false) {
            vBoxDetail.setVisible(true);
            retHBox.setWidth("300px");
        } else {
            vBoxDetail.setVisible(false);
            retHBox.setWidthAuto();
        }

        if (!cardDetailCheckBox.getValue()) {
            if (openPosition.getProjectName().getProjectName().length() > 20) {
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName().substring(0, 20));
            } else {
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName());
            }

            retLinkButton.removeStyleName("detail-candidate-card-wordwrap");
        } else {
            retLinkButton.setCaption(openPosition.getProjectName().getProjectName());
            retLinkButton.setStyleName("detail-candidate-card-wordwrap");
        }

        if (!cardDetailCheckBox.getValue()) {
            salaryMinHBox.setVisible(false);
            salaryMaxHBox.setVisible(false);
            cityOfVacancy.setVisible(false);
            remoteWorkHBox.setVisible(false);

        } else {
            salaryMinHBox.setVisible(true);
            salaryMaxHBox.setVisible(true);
            cityOfVacancy.setVisible(true);
            remoteWorkHBox.setVisible(true);
        }
    }

    @Subscribe("cardDetailCheckBox")
    public void onCardDetailCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        reinitInteractionListDataContainer();
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