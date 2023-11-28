package com.company.itpearls.web.widgets.candidates;

import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.internalemailer.InternalEmailerEdit;
import com.company.itpearls.web.screens.internalemailertemplate.InternalEmailerTemplateEdit;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateEdit;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
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
    @Inject
    private GroupBoxLayout excludeCandidatesLineGroupBox;
    @Inject
    private CheckBox cardDetailCheckBox;


    @WidgetParam
    @WindowParam
    protected Boolean detailMode;

    @WidgetParam
    @WindowParam
    protected Boolean simpleMode;

    private Set<OpenPosition> caseClosedOpenPosition;
    private Set<OpenPosition> processedOpenPosition;
    private Set<OpenPosition> opportunityOpenPosition;
    private Set<OpenPosition> wasOrNowOpenPosition = new TreeSet<>(Comparator.comparing(OpenPosition::getVacansyName));
    private ScrollBoxLayout scrollBoxLayout;
    private Boolean generatedWidget = false;
    private int candidatesCount = 0;

    private final static String QUERY_MY_CANIDATE_EXCLUDE
            = "select e from itpearls_MyActiveCandidateExclude e where e.jobCandidate = :jobCandidate and e.user = :user";
    private final static String QUERY_EXCLUDE_CANDIDATES
            = "select e from itpearls_OpenPosition e where e.positionType = :positionType and not e.openClose = true and not e in (select f.vacancy from itpearls_IteractionList f where f.candidate = :candidate)";
    private static final String QUERY_CASE_CLOSED_OPEN_POSITION
            = "select e from itpearls_IteractionList e where e.iteractionType.signEndCase = true and e.candidate = :candidate";
    @Inject
    private DataContext dataContext;
    @Inject
    private CollectionLoader<InternalEmailTemplate> internalEmailTemplateDl;
    @Inject
    private CollectionContainer<InternalEmailTemplate> internalEmailTemplateDc;
    @Inject
    private Screens screens;
    @Inject
    private Notifications notifications;

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

//    private final static String QUERY_EXCLUDE_CANDIDATES
//            = "select e from itpearls_OpenPosition e where e.positionType = :positionType and not e.openClose = true and not e in (select f.vacancy from itpearls_IteractionList f where f.candidate = :candidate)";


    private void initCandidatesList() {
        Set<JobCandidate> jobCandidateSet = new TreeSet<>(Comparator.comparing(JobCandidate::getFullName));
        Map<JobCandidate, Map.Entry<Integer, Integer>> candidateProjectMap = new HashMap<JobCandidate, Map.Entry<Integer, Integer>>();

        candidatesCount = jobCandidateSet.size();

        for (IteractionList interactionList : iteractionListsDc.getItems()) {
            if (excludeCheckBox.getValue()) {
                List<OpenPosition> whatToOfferCandidate = dataManager.load(OpenPosition.class)
                        .query(QUERY_EXCLUDE_CANDIDATES)
                        .parameter("positionType", interactionList.getCandidate().getPersonPosition())
                        .parameter("candidate", interactionList.getCandidate())
                        .view("openPosition-view")
                        .cacheable(true)
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
        candidateLinkButton.addStyleName("table-wordwrap");
        candidateLinkButton.setHeightFull();
        candidateLinkButton.setWidthAuto();
        candidateLinkButton.setAlignment(Component.Alignment.MIDDLE_CENTER);

        StringBuilder sb = new StringBuilder();
        sb.append(jobCandidate.getFullName())
                .append(" / ")
                .append(jobCandidate.getPersonPosition().getPositionRuName())
                .append(" / ")
                .append(jobCandidate.getCityOfResidence().getCityRuName());
        candidateLinkButton.setCaption(sb.toString());

        candidateLinkButton.addClickListener(e -> {
            screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(jobCandidate)
                    .withParentDataContext(dataContext)
                    .withScreenClass(JobCandidateEdit.class)
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
        candidateLinkButton.addStyleName("h3");
        candidateLinkButton.addStyleName("table-wordwrap");
        candidateLinkButton.setWidth("300px");
        candidateLinkButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        StringBuilder sb = new StringBuilder();

        sb.append(jobCandidate.getFullName())
                .append(" / ")
                .append(jobCandidate.getPersonPosition().getPositionRuName())
                .append(" / ")
                .append(jobCandidate.getCityOfResidence().getCityRuName());
        candidateLinkButton.setCaption(sb.toString());

        candidateLinkButton.addClickListener(e -> {
            screenBuilders.editor(JobCandidate.class, this)
                    .withParentDataContext(getScreenData().getDataContext())
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

        Button sendEmailMessageButton = uiComponents.create(Button.class);
        sendEmailMessageButton.setIcon(CubaIcon.ENVELOPE.source());

        sendEmailMessageButton.setVisible(true); // TODO: пока не вызывается метод
        if (jobCandidate.getEmail() != null) {
            sendEmailMessageButton.setEnabled(true);
        } else {
            sendEmailMessageButton.setEnabled(false);
            sendEmailMessageButton.setDescription(messageBundle.getMessage("msgNotEmailAddress"));
        }

        sendEmailMessageButton.setDescription(messageBundle.getMessage("msgSendEmailDesc"));
        sendEmailMessageButton.setAlignment(Component.Alignment.MIDDLE_RIGHT);
        sendEmailMessageButton.addClickListener(event -> {
            sendEmailTemplateButtonInvoke(jobCandidate);
        });

        retHbox.add(candidateLinkButton);
        retHbox.add(sendEmailMessageButton);
        retHbox.add(undoRemoveCandidateFromConsideration);
        retHbox.add(removeCandidateFromConsideration);

        return retHbox;
    }

    public void sendEmailTemplateButtonInvoke(JobCandidate jobCandidate) {
        internalEmailTemplateDl.setParameter("author", userSession.getUser());
        internalEmailTemplateDl.setParameter("positionType", jobCandidate.getPersonPosition());
        internalEmailTemplateDl.load();

        if (internalEmailTemplateDc.getItems().size() > 0) {
            dialogs.createInputDialog(this)
                    .withCaption(messageBundle.getMessage("msgSelectTemplate"))
                    .withParameters(
                            InputParameter.parameter("emailTemplate")
                                    .withField(() -> {
                                        LookupPickerField<InternalEmailTemplate> internalEmailTemplatePickerField = uiComponents.create(LookupPickerField.class);
                                        internalEmailTemplatePickerField.setCaption(messageBundle.getMessage("msgTemplate"));
                                        internalEmailTemplatePickerField.setWidthFull();
                                        internalEmailTemplatePickerField.setHeightAuto();
                                        internalEmailTemplatePickerField.setNullOptionVisible(false);
                                        internalEmailTemplatePickerField.setRequired(true);
                                        internalEmailTemplatePickerField.setIcon(CubaIcon.ENVELOPE_SQUARE.source());
                                        internalEmailTemplatePickerField.setOptionsList(internalEmailTemplateDc.getItems());
                                        internalEmailTemplatePickerField.setOptionImageProvider(this::emailTemplateImageProvider);

                                        return internalEmailTemplatePickerField;
                                    }))
                    .withActions(DialogActions.OK_CANCEL)
                    .withCloseListener(closeEvent -> {
                        if (closeEvent.closedWith(DialogOutcome.OK)) {
                            InternalEmailTemplate internalEmailTemplate = closeEvent.getValue("emailTemplate");
                            sendSelectedJobCandidatesWithMessages(jobCandidate, internalEmailTemplate);
                        }
                    })
                    .show();
        } else {
            dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage(messageBundle.getMessage("msgNotTemplate"))
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        sendSelectedJobCandidatesWithoutMessages(jobCandidate);
                    }), new DialogAction(DialogAction.Type.NO))
                    .show();
        }
    }

    private void sendSelectedJobCandidatesWithoutMessages(JobCandidate jobCandidate) {
        screenBuilders.editor(InternalEmailer.class, this)
                .withParentDataContext(dataContext)
                .newEntity()
                .withInitializer(e -> {
                    e.setToEmail(jobCandidate);
                })
                .build()
                .show();
    }

    public void sendEmailTemplateButtonInvoke(JobCandidate jobCandidate, OpenPosition openPosition) {
        internalEmailTemplateDl.setParameter("author", userSession.getUser());
        internalEmailTemplateDl.setParameter("positionType", jobCandidate.getPersonPosition());
        internalEmailTemplateDl.setParameter("openPosition", openPosition);
        internalEmailTemplateDl.load();

        if (internalEmailTemplateDc.getItems().size() > 0) {
            if (internalEmailTemplateDc.getItems().size() > 1) {
                dialogs.createInputDialog(this)
                        .withCaption(messageBundle.getMessage("msgSelectTemplate"))
                        .withParameters(
                                InputParameter.parameter("emailTemplate")
                                        .withField(() -> {

                                            LookupPickerField<InternalEmailTemplate> internalEmailTemplatePickerField = uiComponents.create(LookupPickerField.class);
                                            internalEmailTemplatePickerField.setCaption(messageBundle.getMessage("msgTemplate"));
                                            internalEmailTemplatePickerField.setWidthFull();
                                            internalEmailTemplatePickerField.setHeightAuto();
                                            internalEmailTemplatePickerField.setNullOptionVisible(false);
                                            internalEmailTemplatePickerField.setRequired(true);
                                            internalEmailTemplatePickerField.setIcon(CubaIcon.ENVELOPE_SQUARE.source());
                                            internalEmailTemplatePickerField.setOptionsList(internalEmailTemplateDc.getItems());
                                            internalEmailTemplatePickerField.setOptionImageProvider(this::emailTemplateImageProvider);

                                            return internalEmailTemplatePickerField;

                                        }))
                        .withActions(DialogActions.OK_CANCEL)
                        .withCloseListener(closeEvent -> {
                            if (closeEvent.closedWith(DialogOutcome.OK)) {
                                InternalEmailTemplate internalEmailTemplate = closeEvent.getValue("emailTemplate");
                                sendSelectedJobCandidatesWithMessages(jobCandidate, internalEmailTemplate);
                            }
                        })
                        .show();
            } else {
                InternalEmailTemplate internalEmailTemplate = internalEmailTemplateDc.getItems().get(0);
                sendSelectedJobCandidatesWithMessages(jobCandidate, internalEmailTemplate);
            }
        } else {
            dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage(messageBundle.getMessage("msgNotTemplate"))
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        sendSelectedJobCandidatesWithoutMessages(jobCandidate);
                    }), new DialogAction(DialogAction.Type.NO))
                    .show();
        }
    }

    private void sendSelectedJobCandidatesWithMessages(JobCandidate jobCandidate, InternalEmailTemplate internalEmailTemplate) {
            InternalEmailerTemplate internalEmailerTemplate = metadata.create(InternalEmailerTemplate.class);
            internalEmailerTemplate.setFromEmail((ExtUser) userSession.getUser());

            InternalEmailerTemplateEdit emailerTemplateEdit = screens.create(InternalEmailerTemplateEdit.class);
            emailerTemplateEdit.setEntityToEdit(internalEmailerTemplate);
            emailerTemplateEdit.setEmailTemplate(internalEmailTemplate);
            emailerTemplateEdit.setJobCandidate(jobCandidate);
            emailerTemplateEdit.show();
    }

    protected Resource emailTemplateImageProvider(InternalEmailTemplate internalEmailTemplate) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (internalEmailTemplate.getTemplateOpenPosition() != null) {
            if (internalEmailTemplate.getTemplateOpenPosition().getProjectName() != null) {
                if (internalEmailTemplate.getTemplateOpenPosition().getProjectName().getProjectLogo() != null) {
                    return retImage.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(
                                    internalEmailTemplate
                                            .getTemplateOpenPosition()
                                            .getProjectName()
                                            .getProjectLogo());
                } else {
                    return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
                }
            } else {
                return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
            }
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    private void undoRemoveCandidateFromConsideration(JobCandidate jobCandidate) {
        List<MyActiveCandidateExclude> myActiveCandidateExcludes = dataManager.load(MyActiveCandidateExclude.class)
                .query(QUERY_MY_CANIDATE_EXCLUDE)
                .parameter("jobCandidate", jobCandidate)
                .parameter("user", userSession.getUser())
                .view("myActiveCandidateExclude-view")
                .cacheable(true)
                .list();

        CommitContext commitContext = new CommitContext(null, myActiveCandidateExcludes);
        dataManager.commit(commitContext);

/*        for (MyActiveCandidateExclude myActiveCandidateExclude : myActiveCandidateExcludes) {
            dataManager.remove(myActiveCandidateExclude);
        }*/

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
                .cacheable(true)
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

            Button sendEmailMessageButton = uiComponents.create(Button.class);
            sendEmailMessageButton.setIcon(getIconButtonSendEmail(myActiveCandidateExclude, jobCandidate, openPosition));
            sendEmailMessageButton.setDescription(messageBundle.getMessage("msgSendEmailDesc"));
            sendEmailMessageButton.setAlignment(Component.Alignment.MIDDLE_RIGHT);
            if (jobCandidate.getEmail() != null) {
                sendEmailMessageButton.setEnabled(true);
            } else {
                sendEmailMessageButton.setEnabled(false);
                sendEmailMessageButton.setDescription(messageBundle.getMessage("msgNotEmailAddress"));
            }
            sendEmailMessageButton.addClickListener(event -> {
                sendEmailTemplateButtonInvoke(jobCandidate, openPosition);
            });

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
            internalVBox.add(sendEmailMessageButton);

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

    private String getIconButtonSendEmail(List<MyActiveCandidateExclude> myActiveCandidateExclude, JobCandidate jobCandidate, OpenPosition openPosition) {
        IteractionList lastIteraction = null;

        for (MyActiveCandidateExclude mace : myActiveCandidateExclude) {
            if (mace.getJobCandidate().equals(jobCandidate)) {
                Date lastData = null;
                if (mace.getJobCandidate().getIteractionList().size() > 0) {
                    for (IteractionList iteractionList : mace.getJobCandidate().getIteractionList()) {
                        if (iteractionList.getVacancy().equals(openPosition)) {
                            if (lastData == null) {
                                lastData = iteractionList.getDateIteraction();
                                lastIteraction = iteractionList;
                            } else {
                                if (lastData.before(iteractionList.getDateIteraction())) {
                                    lastData = iteractionList.getDateIteraction();
                                    lastIteraction = iteractionList;
                                }
                            }
                        }
                    }
                } else {
                    return CubaIcon.ENVELOPE.source(); // нет вообще взаимодействий
                }
            }
        }

        if (lastIteraction != null) {
            if (lastIteraction.getIteractionType().getSignEmailSend() != null) {
                if (lastIteraction.getIteractionType().getSignEmailSend()) {
                    return CubaIcon.ENVELOPE_OPEN.source(); // тут письмо было отправлено
                } else {
                   return CubaIcon.ENVELOPE_O.source(); // тут не отправлено
                }
            } else {
               return CubaIcon.ENVELOPE_SQUARE.source(); // не отправка
            }
        } else {
            return CubaIcon.ENVELOPE.source(); // нет вообще взаимодействий
        }
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
        StringBuilder sb = new StringBuilder();
        sb.append(openPosition.getVacansyName())
                .append("<br><br>")
                .append(openPosition.getComment());
        projectDetailLabel.setDescription(sb.toString());
//        projectDetailLabel.setDescription(openPosition.getVacansyName() + "<br><br>" + openPosition.getComment());

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
            StringBuilder sb = new StringBuilder();
            sb.append(newVacanciesLabel.getDescription())
                    .append("\n")
                    .append(messageBundle.getMessage("msgLastOpenDate"))
                    .append(": ")
                    .append(sdf.format(openPosition.getLastOpenDate()));
            newVacanciesLabel.setDescription(sb.toString());
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

        StringBuilder sb = new StringBuilder();
        sb.append(description)
                .append("\n\n")
                .append(openPosition.getVacansyName());

        if (openPosition.getProjectName() != null) {
            if (openPosition.getProjectName().getProjectOwner() != null) {
                if (openPosition.getProjectName().getProjectOwner().getSecondName() != null) {
                    sb.append("\n\n");
                    sb.append(openPosition.getProjectName().getProjectOwner().getSecondName());
                }

                if (openPosition.getProjectName().getProjectOwner().getFirstName() != null) {
                    sb.append("\n\n");
                    sb.append(openPosition.getProjectName().getProjectOwner().getFirstName());
                }
            }
        }

        retLinkButton.setDescription(sb.toString());
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
                StringBuilder sb = new StringBuilder();
                sb.append("<h4>");

                if (openPosition.getProjectName() != null) {
                    if (openPosition.getProjectName().getProjectName() != null) {
                        sb.append(openPosition.getProjectName().getProjectName());
                    }
                }

                sb.append("</h4><br><br>");

                if (openPosition.getProjectName() != null) {
                    if (openPosition.getProjectName().getProjectDescription() != null) {
                        sb.append(openPosition.getProjectName().getProjectDescription());
                    }
                }

                projectLogoImage.setDescription(sb.toString());
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

    private final static String QUERY_CASE_OPEN_POSITION
            = "select e from itpearls_IteractionList e where not (e.iteractionType.signEndCase = true) and e.candidate = :candidate and not (e.vacancy.openClose = true)";

    private Set<OpenPosition> getProcessedOpenPosition(JobCandidate jobCandidate) {
        List<IteractionList> caseClosedInteraction = dataManager.load(IteractionList.class)
                .query(QUERY_CASE_OPEN_POSITION)
                .parameter("candidate", jobCandidate)
                .view("iteractionList-view")
                .cacheable(true)
                .list();

        Set<OpenPosition> retOpenPosition = new TreeSet<>(Comparator.comparing(OpenPosition::getVacansyName));

        for (IteractionList iteractionList : caseClosedInteraction) {
            Boolean flag = false;
            for (OpenPosition op : caseClosedOpenPosition) {
                if (op != null) {
                    if (iteractionList != null) {
                        if (iteractionList.getVacancy() != null) {
                            if (op.equals(iteractionList.getVacancy())) {
                                flag = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!flag)
                retOpenPosition.add(iteractionList.getVacancy());
        }

        return retOpenPosition;
    }

    private static final String QUERY_OPPORTUNITY = "select e from itpearls_OpenPosition e where e.positionType = :positionType and not e.openClose = true";

    private Set<OpenPosition> getOpportunityOpenPosition(JobCandidate jobCandidate) {
        List<OpenPosition> opportunityOpenPosition = dataManager.load(OpenPosition.class)
                .query(QUERY_OPPORTUNITY)
                .parameter("positionType", jobCandidate.getPersonPosition())
                .view("openPosition-view")
                .cacheable(true)
                .list();

        Set<OpenPosition> retOpenPosition = new TreeSet<>(Comparator.comparing(OpenPosition::getVacansyName));

        for (OpenPosition openPosition : opportunityOpenPosition) {
            Boolean flag = false;
            for (OpenPosition ccOP : wasOrNowOpenPosition) {
                if (openPosition != null) {
                    if (ccOP != null) {
                        if (openPosition.equals(ccOP)) {
                            flag = true;
                            break;
                        }
                    }
                }
            }

            if (!flag) {
                retOpenPosition.add(openPosition);
            }
        }

        return retOpenPosition;
    }

    private Set<OpenPosition> getCaseClosedOpenPosition(JobCandidate jobCandidate) {

        List<IteractionList> caseClosedInteraction = dataManager.load(IteractionList.class)
                .query(QUERY_CASE_CLOSED_OPEN_POSITION)
                .parameter("candidate", jobCandidate)
                .view("iteractionList-view")
                .cacheable(true)
                .list();

        Set<OpenPosition> retOpenPosition = new TreeSet<>(Comparator.comparing(OpenPosition::getVacansyName));

        for (IteractionList iteractionList : caseClosedInteraction) {
            if (iteractionList.getVacancy() != null) {
                retOpenPosition.add(iteractionList.getVacancy());
            }
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