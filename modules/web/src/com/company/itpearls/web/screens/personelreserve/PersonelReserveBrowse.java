package com.company.itpearls.web.screens.personelreserve;

import com.company.itpearls.core.InteractionService;
import com.company.itpearls.core.StrSimpleService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.candidatecv.CandidateCVEdit;
import com.company.itpearls.web.screens.candidatecv.CandidateCVSimpleBrowse;
import com.company.itpearls.web.screens.fragments.Skillsbar;
import com.company.itpearls.web.screens.internalemailertemplate.InternalEmailerTemplateEdit;
import com.company.itpearls.web.screens.iteractionlist.IteractionListEdit;
import com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.company.itpearls.web.screens.jobcandidate.FindSuitable;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateEdit;
import com.company.itpearls.web.screens.jobcandidate.JobCanidateDetailScreenFragment;
import com.company.itpearls.web.screens.signicons.SignIconsBrowse;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Page;
import org.eclipse.persistence.jpa.jpql.parser.DateTime;

import javax.inject.Inject;
import javax.xml.crypto.Data;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@UiController("itpearls_PersonelReserve.browse")
@UiDescriptor("personel-reserve-browse.xml")
@LookupComponent("personelReservesTable")
@LoadDataBeforeShow
public class PersonelReserveBrowse extends StandardLookup<PersonelReserve> {
    @Inject
    private Button viewJobCandidateCardButton;
    @Inject
    private CheckBox allCandidatesCheckBox;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox activesCheckBox;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private CheckBox inNotWorkCheckBox;
    @Inject
    private DataGrid<PersonelReserve> personelReservesTable;
    @Inject
    private Fragments fragments;
    @Inject
    private DataManager dataManager;
    @Inject
    private Screens screens;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Notifications notifications;
    @Inject
    private DataComponents dataComponents;
    @Inject
    private InteractionService interactionService;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Button sendEmailButton;
    @Inject
    private Metadata metadata;

    static final String QUERY_ITERACTION_DELETE_PERSONAL_RESERVE = "select e from itpearls_Iteraction e where e.signPersonalReserveDelete = true";
    static final String QUERY_DEFAULT_OPEN_POSITION = "select e from itpearls_OpenPosition e where e.vacansyName like 'Default'";
    static final String QUERY_MAX_NUMBER_INTERACTION = "select max(e.numberIteraction) from itpearls_IteractionList e";
    private static final String QUERY_GET_JOB_CANDIDATE_SIGN_ICONS =
            "select e from itpearls_JobCandidateSignIcon e where e.jobCandidate = :jobCandidate";


    private CollectionContainer<IteractionList> iteractionListDc;
    private CollectionLoader<IteractionList> iteractionListDl;
    private CollectionContainer<CandidateCV> candidateCVDc;
    private CollectionLoader<CandidateCV> candidateCVDl;

    private final static String statusColumn = "statusColumn";
    private final static String inWorkColumn = "inWorkColumn";
    private final static String faceImage = "faceImage";
    private final static String tableWordWrapStyle = "table-wordwrap";
    @Inject
    private CollectionLoader<PersonelReserve> personelReservesDl;
    private String personelReserveCloseComment;
    @Inject
    private CheckBox removedFromReserveCheckBox;
    @Inject
    private PopupButton actionsButton;
    @Inject
    private Button closePersonalReserveButton;
    @Inject
    private CheckBox showBetweenAndOther;
    @Inject
    private CollectionContainer<SignIcons> signIconsDc;
    @Inject
    private CollectionLoader<SignIcons> signIconsDl;
    @Inject
    private StrSimpleService strSimpleService;

    @Subscribe("personelReservesTable")
    public void onPersonelReservesTableSelection(DataGrid.SelectionEvent<PersonelReserve> event) {
        viewJobCandidateCardButton.setEnabled(personelReservesTable.getSingleSelected() != null);

        if (personelReservesTable.getSingleSelected() != null) {
            actionsButton.setEnabled(true);
            closePersonalReserveButton.setEnabled(true);

            if (personelReservesTable.getSingleSelected().getJobCandidate().getEmail() != null) {
                if (!personelReservesTable.getSingleSelected().getJobCandidate().getEmail().equals("")) {
                    sendEmailButton.setEnabled(true);
                } else {
                    sendEmailButton.setEnabled(false);
                }
            } else {
                sendEmailButton.setEnabled(false);
            }
        } else {
            sendEmailButton.setEnabled(false);
            actionsButton.setEnabled(false);
            closePersonalReserveButton.setEnabled(false);
        }
    }

    @Subscribe
    public void onInit(InitEvent event) {
//        candidateImageColumnRenderer();
        initInWorkColumnRenderer();
        initStatusColumnRenderer();
        initActionsPopupButton();

        personelReservesTable.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent -> {
                    personelReservesTable.setDetailsVisible(personelReservesTable.getSingleSelected(), true);
                }));
    }

    private void initActionsPopupButton() {
        setActionToActionPopupButton(actionsButton, null);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        allCandidatesCheckBox.setValue(true);
        activesCheckBox.setValue(true);
        inNotWorkCheckBox.setValue(true);
        removedFromReserveCheckBox.setValue(false);

        initSignIconsDataContainer();
    }


    private void initSignIconsDataContainer() {
        signIconsDl.setParameter("user", (ExtUser) userSession.getUser());
        signIconsDl.load();
    }

    @Subscribe("removedFromReserveCheckBox")
    public void onRemovedFromReserveCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (removedFromReserveCheckBox.getValue()) {
            personelReservesDl.removeParameter("removedFromReserve");
        } else {
            personelReservesDl.setParameter("removedFromReserve", false);
        }

        personelReservesDl.load();
    }

    @Install(to = "personelReservesTable.fileImageFace", subject = "columnGenerator")
    private Component personelReservesTableFileImageFaceColumnGenerator(DataGrid.ColumnGeneratorEvent<PersonelReserve> event) {
        HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
        Image image = uiComponents.create(Image.NAME);

        if (event.getItem().getJobCandidate().getFileImageFace() != null) {
            try {
                image.setSource(FileDescriptorResource.class)
                        .setFileDescriptor(event
                                .getItem()
                                .getJobCandidate()
                                .getFileImageFace());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        }

        image.setWidth("30px");
        image.setStyleName("circle-30px");

        image.setScaleMode(Image.ScaleMode.CONTAIN);
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        hBox.setWidthFull();
        hBox.setHeightFull();
        hBox.add(image);

        return hBox;
    }

    private Boolean currentDateBeforeCheck(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        Date currentDate = calendar.getTime();

        return date.after(currentDate) ? false : true;
    }

    @Subscribe("inNotWorkCheckBox")
    public void onInNotWorkCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        String QUERY_ITERACTION_IN_WORK = "select e from itpearls_IteractionList e where e.iteractionDate ";

        if (event.getValue()) {
        }
    }

    private void initInWorkColumnRenderer() {
        personelReservesTable.addGeneratedColumn(inWorkColumn, entity -> {
            HBoxLayout retHBoxLayout = uiComponents.create(HBoxLayout.class);

            Label retLabel = uiComponents.create(Label.class);
            retLabel.setIconFromSet(CubaIcon.QUESTION_CIRCLE);
            retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retLabel.setStyleName("pic-center-large-gray");
            retLabel.setDescription(messageBundle.getMessage("msgEmptyWork"));

            for (IteractionList iteractionList :
                    entity.getItem().getJobCandidate().getIteractionList()) {
                if (entity.getItem().getDate() != null) {
                    if (iteractionList
                            .getDateIteraction()
                            .after(entity.getItem().getDate())) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");

                        retLabel.setStyleName("pic-center-large-red");
                        retLabel.setIconFromSet(CubaIcon.SIGN_IN);
                        String vacancy = "";

                        if (iteractionList.getVacancy() != null) {
                            if (iteractionList.getVacancy().getVacansyName() != null) {
                                iteractionList.getVacancy().getVacansyName();
                            }
                        }

                        retLabel.setDescription(messageBundle.getMessage("msgInWork")
                                + " "
                                + vacancy
                                + "\n\n"
                                + messageBundle.getMessage("msgLastInteraction")
                                + " \'"
                                + (iteractionList.getIteractionType() != null
                                ? iteractionList.getIteractionType().getIterationName() + "\' " : "")
                                + "\' "
                                + messageBundle.getMessage("msgFrom")
                                + " "
                                + sdf.format(iteractionList.getDateIteraction()));
                        break;
                    } else {
                        retLabel.setStyleName("pic-center-large-green");
                        retLabel.setIconFromSet(CubaIcon.CIRCLE_O);
                        retLabel.setDescription(messageBundle.getMessage("msgNotWork"));
                    }
                }
            }

            retHBoxLayout.setAlignment(Component.Alignment.MIDDLE_CENTER);

            retHBoxLayout.setWidthFull();
            retHBoxLayout.setHeightFull();
            retHBoxLayout.add(retLabel);

            return retHBoxLayout;
        });
    }

    private void initStatusColumnRenderer() {
        personelReservesTable.addGeneratedColumn(statusColumn, entity -> {
            HBoxLayout retHBoxLayout = uiComponents.create(HBoxLayout.class);
            retHBoxLayout.setWidthFull();
            retHBoxLayout.setHeightFull();

            Label retLabel = uiComponents.create(Label.class);
            retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (entity.getItem().getEndDate().before(new Date())) { // просрочено
                retLabel.setIconFromSet(CubaIcon.CANCEL);
                retLabel.setStyleName("pic-center-large-red");
                retLabel.setDescription(messageBundle.getMessage("msgReserveIsOverdue"));
            } else {

                if (currentDateBeforeCheck(entity.getItem().getEndDate(), 7)) {
                    retLabel.setIconFromSet(CubaIcon.CIRCLE);
                    retLabel.setStyleName("pic-center-large-orange");
                    retLabel.setDescription(messageBundle.getMessage("msgReserveIsSevenDays"));
                } else {
                    if (currentDateBeforeCheck(entity.getItem().getEndDate(), 30)) {
                        retLabel.setIconFromSet(CubaIcon.CIRCLE);
                        retLabel.setStyleName("pic-center-large-green");
                        retLabel.setDescription(messageBundle.getMessage("msgReserveLessMonth"));
                    } else {
                        retLabel.setIconFromSet(CubaIcon.CIRCLE);
                        retLabel.setStyleName("pic-center-large-gray");
                        retLabel.setDescription(messageBundle.getMessage("msgReserveMoreMonth"));
                    }
                }
            }

            if (entity.getItem().getRemovedFromReserve() != null) {
                if (entity.getItem().getRemovedFromReserve()) {
                    retLabel.setIconFromSet(CubaIcon.CANCEL);
                    retLabel.setStyleName("pic-center-large-red");
                    retLabel.setDescription(messageBundle.getMessage("msgReserveIsOverdue"));
                }
            }

            retHBoxLayout.setAlignment(Component.Alignment.MIDDLE_CENTER);

            retHBoxLayout.add(retLabel);

            return retHBoxLayout;
        });
    }

    private void candidateImageColumnRenderer() {
        personelReservesTable.addGeneratedColumn("jobCandidate", entity -> {
            HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
            Image image = uiComponents.create(Image.NAME);

            if (entity.getItem().getJobCandidate().getFileImageFace() != null) {
                try {
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(entity
                                    .getItem()
                                    .getJobCandidate()
                                    .getFileImageFace());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            image.setWidth("20px");
            image.setStyleName("circle-20px");

            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            hBox.setWidthFull();
            hBox.setHeightFull();
            hBox.add(image);

            return hBox;
        });
    }

    @Subscribe("allCandidatesCheckBox")
    public void onAllCandidatesCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue() != null) {
            if (event.getValue()) {
                personelReservesDl.setParameter("recruter", userSession.getUser());
            } else {
                personelReservesDl.removeParameter("recruter");
            }
        }

        personelReservesDl.load();
    }

    @Subscribe("activesCheckBox")
    public void onActivesCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            personelReservesDl.setParameter("actives", true);
        } else {
            personelReservesDl.removeParameter("actives");
        }

        personelReservesDl.load();
    }

    @Install(to = "personelReservesTable.recruter", subject = "styleProvider")
    private String personelReservesTableRecruterStyleProvider(PersonelReserve personelReserve) {
        return tableWordWrapStyle;
    }

    @Install(to = "personelReservesTable.personPosition", subject = "styleProvider")
    private String personelReservesTablePersonPositionStyleProvider(PersonelReserve personelReserve) {
        return tableWordWrapStyle;
    }

    @Install(to = "personelReservesTable.openPosition", subject = "styleProvider")
    private String personelReservesTableOpenPositionStyleProvider(PersonelReserve personelReserve) {
        return tableWordWrapStyle;
    }

/*    @Install(to = "personelReservesTable", subject = "detailsGenerator")
    private Component personelReservesTableDetailsGenerator(PersonelReserve entity) {
        VBoxLayout mainLayout = uiComponents.create(VBoxLayout.NAME);
        mainLayout.setWidth("100%");
        mainLayout.setMargin(true);

        PersonalReverveJobCandidateFragment jobCanidateDetailScreenFragment = fragments.create(this,
                PersonalReverveJobCandidateFragment.class);
        jobCanidateDetailScreenFragment.setJobCandidate(entity.getJobCandidate());

        HBoxLayout headerBox = uiComponents.create(HBoxLayout.NAME);
        headerBox.setWidthAuto();
        headerBox.setWidth("100%");
        headerBox.setHeight("100%");

        FlowBoxLayout footerBox = uiComponents.create(FlowBoxLayout.NAME);
        footerBox.setWidthAuto();
        footerBox.setHeight("100%");
        footerBox.setSpacing(false);

        HBoxLayout header2Box = uiComponents.create(HBoxLayout.NAME);
        header2Box.setWidth("100%");
        header2Box.setHeight("100%");

        Label<String> infoLabel = uiComponents.create(Label.NAME);
        infoLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        infoLabel.setHtmlEnabled(true);
        infoLabel.setStyleName("h3");
        infoLabel.setValue("Информация о кандидате:");

        Boolean checkBlockCandidate = false;
        if (personelReservesTable.getSingleSelected() != null) {
            if (personelReservesTable.getSingleSelected().getJobCandidate() != null) {
                checkBlockCandidate = !(personelReservesTable
                        .getSingleSelected()
                        .getJobCandidate()
                        .getBlockCandidate() == null
                        ? false : personelReservesTable
                        .getSingleSelected()
                        .getJobCandidate()
                        .getBlockCandidate());
            }
        }

        Label<String> candidateStatusLabel = uiComponents.create(Label.NAME);
        candidateStatusLabel.setHtmlEnabled(true);
        candidateStatusLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        candidateStatusLabel.setStyleName(checkBlockCandidate ? "h2" : "h2-red");
        candidateStatusLabel.setValue(checkBlockCandidate ? "Нормально" : "Заблокирован");

        Label<String> candidateTitle = uiComponents.create(Label.NAME);
        candidateTitle.setHtmlEnabled(true);
        candidateTitle.setStyleName("h3");
        candidateTitle.setValue("Кандидат:");

        Component suitableButton = findSuitableButton(entity.getJobCandidate());

        Label<String> iteractionLabelHeader = uiComponents.create(Label.NAME);
        iteractionLabelHeader.setHtmlEnabled(true);
        iteractionLabelHeader.setStyleName("h3");
        iteractionLabelHeader.setValue("Взаимодействия:");

        Label<String> resumeLabelHeader = uiComponents.create(Label.NAME);
        resumeLabelHeader.setHtmlEnabled(true);
        resumeLabelHeader.setStyleName("h3");
        resumeLabelHeader.setValue("Взаимодействия:");

        Component closeButton = createCloseButton(entity);
        Component editButton = createEditButton(entity);
        Component newIteraction = createNewIteractionButton(entity);
        Component copyLastIteraction = createButtonCopyLastIteraction(entity);
        Component listIteraction = createListIteractionButton(entity);
        Component cvSimpleBrowseButton = createCvSimpleBrowse(entity);
        Component popupButtonCopyLastInteraction = createPopupButtonCopyLastInteraction(entity.getJobCandidate());

        Label<String> cvLabelHeader = uiComponents.create(Label.NAME);
        cvLabelHeader.setHtmlEnabled(true);
        cvLabelHeader.setStyleName("h3");
        cvLabelHeader.setValue("Резюме:");

        Component newResumeButton = addNewResume(entity.getJobCandidate());
        Component editLastResumeButton = editLastResume(entity.getJobCandidate());

        headerBox.add(infoLabel);
        headerBox.add(candidateStatusLabel);

        headerBox.add(candidateTitle);
        headerBox.add(editButton);

        if (suitableButton != null)
            headerBox.add(suitableButton);

        headerBox.add(iteractionLabelHeader);
        headerBox.add(newIteraction);
        headerBox.add(copyLastIteraction);
        headerBox.add(listIteraction);
        headerBox.add(popupButtonCopyLastInteraction);

        headerBox.add(cvLabelHeader);
        headerBox.add(newResumeButton);
        headerBox.add(editLastResumeButton);
        headerBox.add(cvSimpleBrowseButton);

        headerBox.add(closeButton);
        headerBox.expand(infoLabel);
        headerBox.setSpacing(true);

        mainLayout.add(headerBox);
        mainLayout.add(header2Box);

        jobCanidateDetailScreenFragment.setVisibleLogo();
        jobCanidateDetailScreenFragment.setLastSalaryLabel("Зарплатные ожидания");
        jobCanidateDetailScreenFragment.setStatistics();
        jobCanidateDetailScreenFragment.setLinkButtonTelegrem();
        jobCanidateDetailScreenFragment.setLinkButtonTelegremGroup();
        jobCanidateDetailScreenFragment.setLinkButtonEmail();
        jobCanidateDetailScreenFragment.setLinkButtonSkype();
        jobCanidateDetailScreenFragment.setStatisticsLabel();

        Fragment fragment = jobCanidateDetailScreenFragment.getFragment();
        fragment.setWidth("100%");
        fragment.setAlignment(Component.Alignment.BOTTOM_LEFT);
        mainLayout.add(fragment);

        Skillsbar skillBoxFragment = fragments.create(this, Skillsbar.class);
        if (skillBoxFragment.generateSkillLabels(getLastCVText(personelReservesTable
                .getSingleSelected().getJobCandidate()))) {
            mainLayout.add(skillBoxFragment.getFragment());
        }

        mainLayout.expand(fragment);

        return mainLayout;
    } */


    private Component editLastResume(JobCandidate entity) {
        Button editLastResumeButton = uiComponents.create(Button.class);
        editLastResumeButton.setDescription("Редактировать последнее резюме");
        editLastResumeButton.setIconFromSet(CubaIcon.EDIT_ACTION);
        createDataComponentsResume();

        editLastResumeButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    candidateCVDl.setParameter("candidate", entity);
                    candidateCVDl.load();

                    List<CandidateCV> candidateCV = candidateCVDc.getItems();

                    if (candidateCV.size() != 0) {
                        CandidateCV lastCV = null;
                        for (CandidateCV a : candidateCV) {
                            lastCV = a;
                        }

                        Screen screenCV = screenBuilders.editor(CandidateCV.class, this)
                                .withScreenClass(CandidateCVEdit.class)
                                .editEntity(lastCV)
                                .withLaunchMode(OpenMode.NEW_TAB)
                                .withContainer(candidateCVDc)
                                .build();

                        try {
                            screenCV.show();
                        } catch (NullPointerException e) {
                            notifications.create(Notifications.NotificationType.ERROR)
                                    .withCaption("ОШИБКА")
                                    .withDescription("Не могу открыть форму резюме для редактирования.\n" +
                                            "Зайдите в карточку кандидата " +
                                            "и продолжите редактирование резюме во вкладке \"Резюме кандидата\"")
                                    .show();

                            e.printStackTrace();
                        }

                    } else {
                        dialogs.createOptionDialog()
                                .withCaption("Внимание")
                                .withMessage("У кандидата нет ни одного резюме.\nСоздать резюме?")
                                .withActions(
                                        new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                            addNewResumeAction(entity);
                                        }),
                                        new DialogAction(DialogAction.Type.NO)
                                )
                                .show();
                    }
                }));

        return editLastResumeButton;
    }

    private Component addNewResume(JobCandidate entity) {
        Button addResumeButton = uiComponents.create(Button.class);
        addResumeButton.setDescription("Добавить новое резюме кандидата");
        addResumeButton.setIconFromSet(CubaIcon.ADD_ACTION);

        createDataComponentsResume();

        addResumeButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    addNewResumeAction(entity);
                }));

        return addResumeButton;
    }

    protected void addNewResumeAction(JobCandidate entity) {
        candidateCVDl.setParameter("candidate", entity);
        candidateCVDl.load();

        screenBuilders.editor(CandidateCV.class, this)
                .withScreenClass(CandidateCVEdit.class)
                .withInitializer(candidateCV -> {
                    candidateCV.setCandidate(entity);
                })
                .newEntity()
                .withContainer(candidateCVDc)
                .build()
                .show();
    }

    private Component createPopupButtonCopyLastInteraction(PersonelReserve entity) {
        PopupButton lastInteractionPopupButton = uiComponents.create(PopupButton.class);
        lastInteractionPopupButton.setDescription("Копировать последнее взаимодействие с кандидатом");
        lastInteractionPopupButton.setIconFromSet(CubaIcon.FILE_TEXT);

        personelReservesTable.addSelectionListener(e -> {
            if (personelReservesTable.getSingleSelected() == null) {
                lastInteractionPopupButton.setEnabled(false);
                actionsButton.setEnabled(false);
            } else {
                lastInteractionPopupButton.setEnabled(true);
                actionsButton.setEnabled(true);
            }
        });

        Integer MAX_POPULAR_INTERACLION = 5;
        List<Iteraction> mostPopularInteraction = interactionService.getMostPolularIteraction(
                userSessionSource.getUserSession().getUser(), MAX_POPULAR_INTERACLION);

        if (mostPopularInteraction.size() != 0) {
            Integer count = 1;
            for (Iteraction iteraction : mostPopularInteraction) {
                lastInteractionPopupButton.addAction(
                        new BaseAction("setMostPopularInteractionPopupButton" + "-" + count++)
                                .withCaption(iteraction.getIterationName())
                                .withHandler(actionPerformedEvent -> setMostPopularInteractionPopupButton(iteraction)));
            }
            lastInteractionPopupButton.setEnabled(true);
        } else {
            lastInteractionPopupButton.setEnabled(false);
        }

        return lastInteractionPopupButton;
    }


    public void setMostPopularInteractionPopupButton(Iteraction iteraction) {
        if (personelReservesTable.getSingleSelected().getJobCandidate() != null) {
            screenBuilders.editor(IteractionList.class, this)
                    .newEntity()
                    .withInitializer(e -> {
                        e.setCandidate(personelReservesTable.getSingleSelected().getJobCandidate());
                        e.setIteractionType(iteraction);
                        BigDecimal maxNumberIteraction = BigDecimal.ZERO;
                        IteractionList lastIteraction = null;

                        if (personelReservesTable.getSingleSelected().getJobCandidate() != null) {
                            for (IteractionList list : personelReservesTable
                                    .getSingleSelected()
                                    .getJobCandidate()
                                    .getIteractionList()) {
                                if (maxNumberIteraction.compareTo(list.getNumberIteraction()) < 0) {
                                    maxNumberIteraction = list.getNumberIteraction();
                                    lastIteraction = list;
                                }
                            }

                            if (lastIteraction != null) {
                                e.setVacancy(lastIteraction.getVacancy());
                                e.setNumberIteraction(dataManager.loadValue(
                                        "select max(e.numberIteraction) " +
                                                "from itpearls_IteractionList e", BigDecimal.class)
                                        .one().add(BigDecimal.ONE));
                            }
                        }
                    })
                    .build()
                    .show();
        }
    }

    private Component createCvSimpleBrowse(PersonelReserve entity) {
        Button cvSimpleBrowseButton = uiComponents.create(Button.class);
        cvSimpleBrowseButton.setDescription("Копировать резюме кандидата");
        cvSimpleBrowseButton.setIconFromSet(CubaIcon.FILE_TEXT_O);

        createDataComponentsResume();

        cvSimpleBrowseButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    candidateCVDl.setParameter("candidate", entity);
                    candidateCVDl.load();

                    CandidateCVSimpleBrowse candidateCVSimpleBrowse = screens.create(CandidateCVSimpleBrowse.class);
                    candidateCVSimpleBrowse.setSelectedCandidate(entity.getJobCandidate());
                    candidateCVSimpleBrowse.setJobCandidate(entity.getJobCandidate());
                    screens.show(candidateCVSimpleBrowse);

                }));

        return cvSimpleBrowseButton;
    }


    private String getLastCVText(JobCandidate singleSelected) {
        if (singleSelected != null) {
            if (singleSelected.getCandidateCv() != null) {
                if (singleSelected.getCandidateCv().size() != 0) {
                    CandidateCV lastCV = singleSelected.getCandidateCv().get(0);

                    for (CandidateCV candidateCV : singleSelected.getCandidateCv()) {
                        if (lastCV.getDatePost().before((candidateCV.getDatePost()))) {
                            lastCV = candidateCV;
                        }
                    }

                    return lastCV.getTextCV();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void createDataComponents() {
        DataContext dataContext = dataComponents.createDataContext();
        getScreenData().setDataContext(dataContext);

        iteractionListDc = dataComponents.createCollectionContainer(IteractionList.class);

        iteractionListDl = dataComponents.createCollectionLoader();
        iteractionListDl.setContainer(iteractionListDc);
        iteractionListDl.setDataContext(dataContext);
        iteractionListDl.setView("iteractionList-view");
        iteractionListDl.setQuery("select e from itpearls_IteractionList e " +
                "where e.candidate = :candidate " +
                "order by e.numberIteraction desc");
    }

    private void createDataComponentsResume() {
        DataContext dataContext = dataComponents.createDataContext();
        getScreenData().setDataContext(dataContext);

        candidateCVDc = dataComponents.createCollectionContainer(CandidateCV.class);

        candidateCVDl = dataComponents.createCollectionLoader();
        candidateCVDl.setContainer(candidateCVDc);
        iteractionListDl.setDataContext(dataContext);
        candidateCVDl.setView("candidateCV-view");
        candidateCVDl.setQuery("select e from itpearls_CandidateCV e " +
                "where e.candidate = :candidate order by e.datePost");
    }

    private Component createListIteractionButton(PersonelReserve entity) {
        Button listIteractionButton = uiComponents.create(Button.class);
        listIteractionButton.setDescription("Список взаимодействий");
        listIteractionButton.setIconFromSet(CubaIcon.FILE_TEXT_O);
        createDataComponents();

        listIteractionButton.setAction(new BaseAction("listIteraction")
                .withHandler(actionPerformedEvent -> {
                    iteractionListDl.setParameter("candidate", entity.getJobCandidate());
                    iteractionListDl.load();

                    IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
                    iteractionListSimpleBrowse.setSelectedCandidate(entity.getJobCandidate());
                    iteractionListSimpleBrowse.setJobCandidate(entity.getJobCandidate());
                    screens.show(iteractionListSimpleBrowse);

                }));

        return listIteractionButton;

    }

    private Component createButtonCopyLastIteraction(PersonelReserve entity) {
        Button lastIteractionButton = uiComponents.create(Button.class);
        lastIteractionButton.setDescription("Копировать последнее взаимодействие с кандидатом");
        lastIteractionButton.setIconFromSet(CubaIcon.COPY);


        lastIteractionButton.setAction(new BaseAction("copyLastIteraction")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(IteractionList.class, this)
                            .newEntity()
                            .withScreenClass(IteractionListEdit.class)
                            .withInitializer(iteractionList1 -> {
                                iteractionList1.setCandidate(personelReservesTable
                                        .getSingleSelected()
                                        .getJobCandidate());

                                BigDecimal maxNumberIteraction = BigDecimal.ZERO;
                                IteractionList lastIteraction = null;

                                if (personelReservesTable.getSingleSelected().getJobCandidate() != null) {
                                    for (IteractionList list : personelReservesTable
                                            .getSingleSelected()
                                            .getJobCandidate()
                                            .getIteractionList()) {
                                        if (maxNumberIteraction.compareTo(list.getNumberIteraction()) < 0) {
                                            maxNumberIteraction = list.getNumberIteraction();
                                            lastIteraction = list;
                                        }
                                    }
                                } else {
                                    notifications.create(Notifications.NotificationType.WARNING)
                                            .withDescription("ВНИМАНИЕ!")
                                            .withCaption("Нужно выделить строку в таблице для добавления взаимодействия!")
                                            .show();
                                }

                                if (lastIteraction != null) {
                                    iteractionList1.setVacancy(lastIteraction.getVacancy());
                                    iteractionList1.setNumberIteraction(dataManager.loadValue(
                                            "select max(e.numberIteraction) " +
                                                    "from itpearls_IteractionList e", BigDecimal.class)
                                            .one().add(BigDecimal.ONE));
                                }
                            })
                            .withAfterCloseListener(e -> {
                                personelReservesDl.load();
                            })
                            .build()
                            .show();
                }));

        return lastIteractionButton;
    }

    private Component createNewIteractionButton(PersonelReserve entity) {
        Button newIteractionButton = uiComponents.create(Button.class);
        newIteractionButton.setDescription("Новое взаимодействие");
        newIteractionButton.setIconFromSet(CubaIcon.CREATE_ACTION);


        newIteractionButton.setAction(new BaseAction("newIteraction")
                .withHandler(actionPerformedEvent ->
                        screenBuilders.editor(IteractionList.class, this)
                                .newEntity()
                                .withScreenClass(IteractionListEdit.class)
                                .withInitializer(iteractionList1 -> {
                                    iteractionList1.setCandidate(personelReservesTable
                                            .getSingleSelected()
                                            .getJobCandidate());
                                })
                                .withAfterCloseListener(e -> {
                                    personelReservesDl.load();
                                })
                                .build()
                                .show())
        );

        return newIteractionButton;
    }

    private Component createEditButton(PersonelReserve entity) {
        Button editButton = uiComponents.create(Button.class);
        editButton.setDescription("Редактирование");
        editButton.setIconFromSet(CubaIcon.EDIT_ACTION);

        BaseAction editAction = new BaseAction("edit")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(JobCandidate.class, this)
                            .editEntity(entity.getJobCandidate())
                            .build()
                            .show();
                })
                .withCaption("");
        editButton.setAction(editAction);
        return editButton;
    }

    private Component findSuitableButton(JobCandidate entity) {
        if (dataManager.load(CandidateCV.class)
                .query("select e from itpearls_CandidateCV e where e.candidate = :candidate")
                .cacheable(true)
                .parameter("candidate", entity)
                .view("candidateCV-view")
                .list().size() != 0) {
            Button suitableButton = uiComponents.create(Button.class);
            suitableButton.setDescription("Подобрать вакансию по резюме");
            suitableButton.setIconFromSet(CubaIcon.EYE);

            suitableButton.setAction(new BaseAction("findSuitable")
                    .withHandler(actionPerformedEvent -> {
                        FindSuitable findSuitable = screens.create(FindSuitable.class);
                        findSuitable.setJobCandidate(entity);

                        findSuitable.show();
                    }));

            return suitableButton;
        } else
            return null;
    }

    public void closePersonalReserveButtonInvoke() {
        Iteraction iteractionPersonalReserveDelete = null;

        try {
            iteractionPersonalReserveDelete = dataManager
                    .load(Iteraction.class)
                    .query(QUERY_ITERACTION_DELETE_PERSONAL_RESERVE)
                    .view("iteraction-view")
                    .one();
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
            notifications.create(Notifications.NotificationType.WARNING)
                    .withType(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgNotIteractionPersonalReserveDelete"))
                    .show();
        }

        createIteraction(iteractionPersonalReserveDelete);

        PersonelReserve personelReserve = personelReservesTable.getSingleSelected();
        personelReserve.setInProcess(false);
        personelReserve.setRemovedFromReserve(true);

        dataManager.commit(personelReserve);

        personelReservesTable.repaint();
    }

    public void viewJobCandidateCardButtonInvoke() {
        screenBuilders.editor(JobCandidate.class, this)
                .withScreenClass(JobCandidateEdit.class)
                .editEntity(personelReservesTable.getSingleSelected().getJobCandidate())
                .build()
                .show();
    }

    public void sendEmailButtonInvoke() {
        InternalEmailerTemplateEdit screen = (InternalEmailerTemplateEdit) screenBuilders.editor(InternalEmailerTemplate.class, this)
                .newEntity()
                .withInitializer(e -> {
                    e.setFromEmail((ExtUser) userSession.getUser());
                    e.setToEmail(personelReservesTable.getSingleSelected().getJobCandidate());
                })
                .build();
        screen.setJobCandidate(personelReservesTable.getSingleSelected().getJobCandidate());
        screen.show();
    }


    @Install(to = "personelReservesTable.candidateActions", subject = "columnGenerator")
    private Component personelReservesTableCandidateActionsColumnGenerator(DataGrid.ColumnGeneratorEvent<PersonelReserve> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        PopupButton actionButton = uiComponents.create(PopupButton.class);
        actionButton.setIconFromSet(CubaIcon.BARS);
        actionButton.setWidthAuto();
        actionButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        actionButton.setShowActionIcons(true);

        setActionToActionPopupButton(actionButton, event.getItem());

        retBox.add(actionButton);

        return retBox;
    }

    private void setActionToActionPopupButton(PopupButton actionButton, PersonelReserve personelReserve) {

/*        actionButton.addAction(new BaseAction("selectedForActionAction")
                .withIcon(CubaIcon.STAR.source())
                .withCaption(messageBundle.getMessage("msgSelectForAction"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    selectForAction();
                    try {
                        personelReservesTable.scrollTo(personelReserve);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                })); */
        final String separatorChar = "⎯";

        String separator = separatorChar.repeat(15);

/*
        final String separator = "\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014";
*/

        actionButton.addAction(new BaseAction("sendEmailAction")
                .withIcon(CubaIcon.ENVELOPE.source())
                .withCaption(messageBundle.getMessage("msgEmail"))
                .withHandler(actionPerformedEvent -> {
                    personelReservesTable.setSelected(personelReserve);
                    sendEmailButtonInvoke();
                    selectForAction();
                    personelReservesTable.scrollTo(personelReserve);
                }));


        actionButton.addAction(new BaseAction("separator2Action")
                .withCaption(separator));
        actionButton.getAction("separator2Action").setEnabled(false);

        actionButton.addAction(new BaseAction("openCardAction")
                .withIcon(CubaIcon.CHILD.source())
                .withCaption(messageBundle.getMessage("msgJobCandidate"))
                .withHandler(actionPerformedEvent -> {
                    personelReservesTable.setSelected(personelReserve);
                    viewJobCandidateCardButtonInvoke();
                    personelReservesTable.scrollTo(personelReserve);
                }));


        actionButton.addAction(new BaseAction("createIntecactionAction")
                .withIcon(CubaIcon.BATH.source())
                .withCaption(messageBundle.getMessage("msgCreateInteraction"))
                .withHandler(actionPerformedEvent -> {
                    personelReservesTable.setSelected(personelReserve);
                    createInteractionButtonInvoke();
                    selectForAction();
                }));

        actionButton.addAction(new BaseAction("viewInteractionAction")
                .withIcon(CubaIcon.VIEW_ACTION.source())
                .withCaption(messageBundle.getMessage("msgViewInteraction"))
                .withHandler(actionPerformedEvent -> {
                    personelReservesTable.setSelected(personelReserve);
                    viewInteractionButtonInvoke();
                    personelReservesTable.scrollTo(personelReserve);
                }));


        actionButton.addAction(new BaseAction("separator3Action")
                .withCaption(separator));
        actionButton.getAction("separator3Action").setEnabled(false);

        if (personelReserve != null) {
            if (personelReserve.getRemovedFromReserve() != null) {
                if (personelReserve.getRemovedFromReserve() != true) {
                    actionButton.addAction(new BaseAction("clearPersonalReserveAction")
                            .withIcon(CubaIcon.CANCEL.source())
                            .withCaption(messageBundle.getMessage("msgClosePersonalReserve"))
                            .withHandler(actionPerformedEvent -> {
                                personelReservesTable.setSelected(personelReserve);
                                closePersonalReserveButtonInvoke();
                                personelReservesTable.scrollTo(personelReserve);
                            }));
                }
            } else {
                actionButton.addAction(new BaseAction("clearPersonalReserveAction")
                        .withIcon(CubaIcon.CANCEL.source())
                        .withCaption(messageBundle.getMessage("msgClosePersonalReserve"))
                        .withHandler(actionPerformedEvent -> {
                            personelReservesTable.setSelected(personelReserve);
                            closePersonalReserveButtonInvoke();
                            personelReservesTable.scrollTo(personelReserve);
                        }));
            }

            if (personelReserve.getRemovedFromReserve() != null) {
                if (personelReserve.getRemovedFromReserve() != true) {
                    actionButton.addAction(new BaseAction("clearPersonalReserveWithCommentAction")
                            .withIcon(CubaIcon.PICKERFIELD_CLEAR_READONLY.source())
                            .withCaption(messageBundle.getMessage("msgClosePersonalReserveWithComment"))
                            .withHandler(actionPerformedEvent -> {
                                personelReservesTable.setSelected(personelReserve);
                                closePersonalReserveButtonWithCommentInvoke();
                                personelReservesTable.scrollTo(personelReserve);
                            }));
                }
            } else {
                actionButton.addAction(new BaseAction("clearPersonalReserveWithCommentAction")
                        .withIcon(CubaIcon.PICKERFIELD_CLEAR_READONLY.source())
                        .withCaption(messageBundle.getMessage("msgClosePersonalReserveWithComment"))
                        .withHandler(actionPerformedEvent -> {
                            personelReservesTable.setSelected(personelReservesTable.getSingleSelected());
                            closePersonalReserveButtonWithCommentInvoke();
                            personelReservesTable.scrollTo(personelReservesTable.getSingleSelected());
                        }));
            }
        } else {
            actionButton.addAction(new BaseAction("clearPersonalReserveAction")
                    .withIcon(CubaIcon.CANCEL.source())
                    .withCaption(messageBundle.getMessage("msgClosePersonalReserve"))
                    .withHandler(actionPerformedEvent -> {
                        closePersonalReserveButtonInvoke();
                    }));

            actionButton.addAction(new BaseAction("clearPersonalReserveWithCommentAction")
                    .withIcon(CubaIcon.PICKERFIELD_CLEAR_READONLY.source())
                    .withCaption(messageBundle.getMessage("msgClosePersonalReserveWithComment"))
                    .withHandler(actionPerformedEvent -> {
                        closePersonalReserveButtonWithCommentInvoke();
                    }));
        }

        actionButton.addAction(new BaseAction("separator1Action")
                .withCaption(separator));
        actionButton.getAction("separator1Action").setEnabled(false);

/*        actionButton.addAction(new BaseAction("selectedForActionActionStarRed")
                .withIcon(CubaIcon.STAR.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionStarRed"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    selectForAction(StdSelections.STAR_RED);
                    try {
                        personelReservesTable.scrollTo(personelReserve);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionStarYellow")
                .withIcon(CubaIcon.STAR.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionStarYellow"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    selectForAction(StdSelections.STAR_YELLOW);
                    try {
                        personelReservesTable.scrollTo(personelReserve);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionStarGreen")
                .withIcon(CubaIcon.STAR.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionStarGreen"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    selectForAction(StdSelections.STAR_GREEN);
                    try {
                        personelReservesTable.scrollTo(personelReserve);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionFlagRed")
                .withIcon(CubaIcon.FLAG.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionFlagRed"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    selectForAction(StdSelections.FLAG_RED);
                    try {
                        personelReservesTable.scrollTo(personelReserve);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionFlagYellow")
                .withIcon(CubaIcon.FLAG.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionFlagYellow"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    selectForAction(StdSelections.FLAG_YELLOW);
                    try {
                        personelReservesTable.scrollTo(personelReserve);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionFlagGreen")
                .withIcon(CubaIcon.FLAG.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionFlagGreen"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    selectForAction(StdSelections.FLAG_GREEN);
                    try {
                        personelReservesTable.scrollTo(personelReserve);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("clearSelection")
                .withIcon(CubaIcon.PICKERFIELD_CLEAR.source())
                .withCaption(messageBundle.getMessage("msgClearSelection"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    clearSelection();
                    try {
                        personelReservesTable.scrollTo(personelReserve);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }

                }));
*/


        for (SignIcons icons : signIconsDc.getItems()) {
            actionButton.addAction(new BaseAction(
                    strSimpleService.deleteExtraCharacters(icons.getTitleEnd() + "Action"))
                    .withIcon(icons.getIconName())
                    .withCaption(icons.getTitleRu())
                    .withDescription(icons.getTitleDescription())
                    .withHandler(actionPerformedAction -> {
                        personelReservesTable.setSelected(personelReserve);
                        setSignIcons(icons, personelReservesTable.getSingleSelected());
                    }));
        }

        actionButton.addAction(new BaseAction("removeSignAction")
                .withIcon(CubaIcon.REMOVE_ACTION.source())
                .withCaption(messageBundle.getMessage("msgRemoveSignAction"))
                .withDescription(messageBundle.getMessage("msgRemoveSignActionDesc"))
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    removeSignAction(personelReservesTable.getSingleSelected());
                }));

        actionButton.addAction(new BaseAction("separator3Action")
                .withCaption(separator));

        actionButton.addAction(new BaseAction("editSignIconsAction")
                .withCaption(messageBundle.getMessage("msgEditSignIconsAction"))
                .withDescription("msgEditSignIconsActionDesc")
                .withIcon(CubaIcon.FONTICONS.source())
                .withHandler(actionPerformedAction -> {
                    personelReservesTable.setSelected(personelReserve);
                    SignIconsBrowse screen  = (SignIconsBrowse) screenBuilders.lookup(SignIcons.class, this)
                            .withOpenMode(OpenMode.DIALOG)
                            .build();
                    screen.setParentJobCandidateTable(personelReservesTable);

                    screen.show();
                }));

    }

    private void removeSignAction(PersonelReserve personelReserve) {
        List<JobCandidateSignIcon> jobCandidateSignIcons = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", personelReserve.getJobCandidate())
                .view("jobCandidateSignIcon-view")
                .list();

        if (jobCandidateSignIcons.size() > 0) {
            for (JobCandidateSignIcon jcsi : jobCandidateSignIcons) {
                dataManager.remove(jcsi);
            }
        }

        personelReservesTable.repaint();
        personelReservesTable.setSelected(personelReserve);
        personelReservesTable.scrollTo(personelReserve);
    }

    private void setSignIcons(SignIcons icons, PersonelReserve personelReserve) {
        List<JobCandidateSignIcon> jobCandidateSignIcon;

        jobCandidateSignIcon = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", personelReserve.getJobCandidate())
                .view("jobCandidateSignIcon-view")
                .list();

        if (jobCandidateSignIcon.size() == 0) {
            JobCandidateSignIcon jcsi = metadata.create(JobCandidateSignIcon.class);
            jcsi.setJobCandidate(personelReserve.getJobCandidate());
            jcsi.setSignIcon(icons);
            jcsi.setUser((ExtUser) userSession.getUser());

            dataManager.commit(jcsi);
        } else {
            jobCandidateSignIcon.get(0).setSignIcon(icons);
            dataManager.commit(jobCandidateSignIcon.get(0));
        }

        personelReservesTable.repaint();
        personelReservesTable.setSelected(personelReserve);
        personelReservesTable.scrollTo(personelReserve);
    }

    private void clearSelection() {
        PersonelReserve personelReserve = personelReservesTable.getSingleSelected();
        personelReserve.setSelectedForAction(null);
        personelReserve.setSelectionSymbolForActions(null);
        dataManager.commit(personelReserve);

        personelReservesDl.load();
        personelReservesTable.repaint();
        try {
            personelReservesTable.setSelected(personelReserve);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectForAction(StdSelections star) {
        PersonelReserve personelReserve = personelReservesTable.getSingleSelected();
        personelReserve.setSelectedForAction(true);
        personelReserve.setSelectionSymbolForActions(star.getId());
        dataManager.commit(personelReserve);

        personelReservesDl.load();
        personelReservesTable.repaint();
        try {
            personelReservesTable.setSelected(personelReserve);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closePersonalReserveButtonWithCommentInvoke() {
        dialogs.createInputDialog(this)
                .withCaption(messageBundle.getMessage("msgInputComment"))
                .withParameters(
                        InputParameter.stringParameter("comment")
                                .withCaption(messageBundle.getMessage("msgComment")))
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(inputDialogCloseEvent -> {
                    if (inputDialogCloseEvent.closedWith(DialogOutcome.OK)) {
                        String comment = inputDialogCloseEvent.getValue("comment");

                        personelReserveCloseComment = comment;
                        closePersonalReserveButtonInvoke();
                    }
                })
                .show();
    }

    private void selectForAction() {
        PersonelReserve personelReserve = personelReservesTable.getSingleSelected();
        personelReserve.setSelectedForAction(personelReserve.getSelectedForAction() != null ? !personelReserve.getSelectedForAction() : true);
        dataManager.commit(personelReserve);

        personelReservesDl.load();
        personelReservesTable.repaint();
        try {
            personelReservesTable.setSelected(personelReserve);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void viewInteractionButtonInvoke() {
        IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
        iteractionListSimpleBrowse.setSelectedCandidate(personelReservesTable.getSingleSelected().getJobCandidate());
        screens.show(iteractionListSimpleBrowse);
    }

    private void createInteractionButtonInvoke() {
        screenBuilders.editor(IteractionList.class, this)
                .withScreenClass(IteractionListEdit.class)
                .withInitializer(event -> {
                    event.setCandidate(personelReservesTable.getSingleSelected().getJobCandidate());
                    if (personelReservesTable.getSingleSelected().getOpenPosition() != null) {
                        event.setVacancy(personelReservesTable.getSingleSelected().getOpenPosition());
                        event.setCurrentOpenClose(personelReservesTable.getSingleSelected().getOpenPosition().getOpenClose());
                    }
                })
                .newEntity()
                .build()
                .show();
    }

    private IteractionList createIteraction(Iteraction iteraction) {

        IteractionList iteractionList = metadata.create(IteractionList.class);
        iteractionList.setDateIteraction(new Date());
        iteractionList.setIteractionType(iteraction);
        iteractionList.setCandidate(personelReservesTable.getSingleSelected().getJobCandidate());
        iteractionList.setRating(4);
        iteractionList.setRecrutierName(userSession.getUser().getName());
        iteractionList.setRecrutier((ExtUser) userSession.getUser());

        if (personelReserveCloseComment != null) {
            StringBuffer comment = new StringBuffer(personelReserveCloseComment);
            personelReserveCloseComment = null;
            iteractionList.setComment(comment.toString());
        }

        if (personelReservesTable.getSingleSelected().getOpenPosition() != null) {
            iteractionList.setVacancy(personelReservesTable.getSingleSelected().getOpenPosition());
            iteractionList.setCurrentOpenClose(personelReservesTable.getSingleSelected().getOpenPosition().getOpenClose());
        } else {
            OpenPosition openPositionDefault = null;

            try {
                openPositionDefault = dataManager.load(OpenPosition.class)
                        .query(QUERY_DEFAULT_OPEN_POSITION)
                        .view("openPosition-view")
                        .one();
            } catch (NullPointerException e) {
                e.printStackTrace();

                notifications.create(Notifications.NotificationType.WARNING)
                        .withType(Notifications.NotificationType.WARNING)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription(messageBundle.getMessage("msgNotDefaultOpenPosition"))
                        .show();
            }

            if (openPositionDefault != null) {
                iteractionList.setVacancy(openPositionDefault);
                iteractionList.setCurrentOpenClose(openPositionDefault.getOpenClose());
            }
        }

        BigDecimal numberIteraction = null;
        numberIteraction = dataManager.loadValue(QUERY_MAX_NUMBER_INTERACTION, BigDecimal.class)
                .one();

        numberIteraction.add(BigDecimal.ONE);
        iteractionList.setNumberIteraction(numberIteraction);
        dataManager.commit(iteractionList);

        return iteractionList;
    }

    @Install(to = "personelReservesTable.jobCandidate", subject = "columnGenerator")
    private Component personelReservesTableJobCandidateColumnGenerator(DataGrid.ColumnGeneratorEvent<PersonelReserve> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);

        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setSpacing(true);

        Label signIconLabel = getSignIconLabel(event.getItem().getJobCandidate());

        Label jobCandidateLabel = uiComponents.create(Label.class);
        jobCandidateLabel.setWidthFull();
        jobCandidateLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        jobCandidateLabel.setValue(event.getItem().getJobCandidate().getFullName()
                + " / "
                + event.getItem().getJobCandidate().getPersonPosition().getPositionRuName()
                + " / "
                + event.getItem().getJobCandidate().getPersonPosition().getPositionEnName()
                + " / "
                + event.getItem().getJobCandidate().getCityOfResidence().getCityRuName());
        jobCandidateLabel.setStyleName("table-wordwrap");

//        Label star = uiComponents.create(Label.class);
//        star.setIconFromSet(CubaIcon.STAR);
//        star.setAlignment(Component.Alignment.MIDDLE_LEFT);
//        star.setStyleName("pic-center-large-orange");

        Label newReserveLabel = uiComponents.create(Label.class);
        newReserveLabel.setValue(messageBundle.getMessage("msgNewReserve"));
        newReserveLabel.setStyleName("button_table_red");
        newReserveLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        newReserveLabel.setWidthAuto();
        newReserveLabel.setHeightAuto();
        newReserveLabel.setVisible(false);

        Label futureReserveLabel = uiComponents.create(Label.class);
        futureReserveLabel.setValue(messageBundle.getMessage("msgFutureReserve"));
        futureReserveLabel.setStyleName("button_table_blue");
        futureReserveLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        futureReserveLabel.setWidthAuto();
        futureReserveLabel.setHeightAuto();
        futureReserveLabel.setVisible(false);

/*        if (event.getItem().getSelectedForAction() != null) {
            if (event.getItem().getSelectionSymbolForActions() == null) {
                if (event.getItem().getSelectedForAction()) {
                    star.setVisible(true);
                } else {
                    star.setVisible(false);
                }
            } else {
                StdSelections s = StdSelections.fromId(event.getItem().getSelectionSymbolForActions());

                switch (s) {
                    case STAR_RED:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_RED.getId());
                        break;
                    case STAR_YELLOW:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_YELLOW.getId());
                        break;
                    case STAR_GREEN:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_GREEN.getId());
                        break;
                    case FLAG_RED:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_RED.getId());
                        break;
                    case FLAG_YELLOW:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_YELLOW.getId());
                        break;
                    case FLAG_GREEN:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_GREEN.getId());
                        break;
                    default:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_YELLOW.getId());
                        break;
                }
            }
        } else {
            star.setVisible(false);
        } */

        retHBox.add(signIconLabel);
//        retHBox.add(star);
        retHBox.add(newReserveLabel);
        retHBox.add(futureReserveLabel);
        retHBox.add(jobCandidateLabel);
        retHBox.expand(jobCandidateLabel);

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        Date currentDate = new Date();

        if (event.getItem().getDate() != null) {
            gregorianCalendar.setTime(event.getItem().getDate());

            gregorianCalendar.add(Calendar.DAY_OF_MONTH, 3);

            if (currentDate.before(gregorianCalendar.getTime())) {
                newReserveLabel.setVisible(true);
            } else {
                newReserveLabel.setVisible(false);
            }

            futureReserveLabel.setVisible(false);
        }

        if (event.getItem().getDate() != null) {
            if (event.getItem().getDate().after(currentDate)) {
                futureReserveLabel.setVisible(true);
                newReserveLabel.setVisible(false);
            }
        }

        return retHBox;
    }

    private Label getSignIconLabel(JobCandidate jobCandidate) {
        Label retLabel = uiComponents.create(Label.class);

        List<JobCandidateSignIcon> jobCandidateSignIcons = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", jobCandidate)
                .view("jobCandidateSignIcon-view")
                .list();

        if (jobCandidateSignIcons.size() > 0) {
            retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retLabel.setIcon(jobCandidateSignIcons.get(0).getSignIcon().getIconName());

            if (jobCandidateSignIcons.get(0).getSignIcon().getTitleDescription() != null) {
                retLabel.setDescription(jobCandidateSignIcons.get(0).getSignIcon().getTitleDescription());
            } else {
                retLabel.setDescription(jobCandidateSignIcons.get(0).getSignIcon().getTitleRu());
            }

            injectColorCss(jobCandidateSignIcons.get(0).getSignIcon().getIconColor());
            retLabel.setStyleName("pic-center-large-"
                    + jobCandidateSignIcons.get(0).getSignIcon().getIconColor());
        }

        return retLabel;
    }

    protected void injectColorCss(String color) {
        Page.Styles styles = Page.getCurrent().getStyles();
        String style = String.format(
                ".pic-center-large-%s {" +
                        "color: #%s;" +
                        "text-align: center;" +
                        "text-color: gray;" +
                        "font-size: large;" +
                        "margin: 0 auto;" +
                        "}",
                color, color);

        styles.add(style);
    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        personelReservesDl.setParameter("currentDate", new Date());
        personelReservesDl.load();

        showBetweenAndOther.setValue(true);
    }

    @Subscribe("showBetweenAndOther")
    public void onShowBetweenAndOtherValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            personelReservesDl.setParameter("currentDate", new Date());
        } else {
            personelReservesDl.removeParameter("currentDate");
        }

        personelReservesDl.load();
    }

/*    @Install(to = "personelReservesTable", subject = "detailsGenerator")
    private Component personelReservesTableDetailsGenerator(PersonelReserve entity) throws IOException, ClassNotFoundException {
        VBoxLayout mainLayout = uiComponents.create(VBoxLayout.NAME);
        mainLayout.setWidth("100%");
        mainLayout.setMargin(true);

        JobCanidateDetailScreenFragment jobCanidateDetailScreenFragment = fragments.create(this,
                JobCanidateDetailScreenFragment.class);
        jobCanidateDetailScreenFragment.setJobCandidate(entity.getJobCandidate());

        HBoxLayout headerBox = uiComponents.create(HBoxLayout.NAME);
        headerBox.setWidthAuto();
        headerBox.setWidth("100%");
        headerBox.setHeight("100%");

        FlowBoxLayout footerBox = uiComponents.create(FlowBoxLayout.NAME);
        footerBox.setWidthAuto();
        footerBox.setHeight("100%");
        footerBox.setSpacing(false);

        HBoxLayout header2Box = uiComponents.create(HBoxLayout.NAME);
        header2Box.setWidth("100%");
        header2Box.setHeight("100%");

        Label<String> infoLabel = uiComponents.create(Label.NAME);
        infoLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        infoLabel.setHtmlEnabled(true);
        infoLabel.setStyleName("h3");
        infoLabel.setValue("Информация о кандидате:");

        Boolean checkBlockCandidate = false;
        if (personelReservesTable.getSingleSelected() != null) {
            checkBlockCandidate = !(personelReservesTable.getSingleSelected().getJobCandidate().getBlockCandidate() == null
                    ? false : personelReservesTable.getSingleSelected().getJobCandidate().getBlockCandidate());
        }

        Label<String> candidateStatusLabel = uiComponents.create(Label.NAME);
        candidateStatusLabel.setHtmlEnabled(true);
        candidateStatusLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        candidateStatusLabel.setStyleName(checkBlockCandidate ? "h2" : "h2-red");
        candidateStatusLabel.setValue(checkBlockCandidate ? "Нормально" : "Заблокирован");

        Label<String> candidateTitle = uiComponents.create(Label.NAME);
        candidateTitle.setHtmlEnabled(true);
        candidateTitle.setStyleName("h3");
        candidateTitle.setValue("Кандидат:");

        Component suitableButton = findSuitableButton(entity.getJobCandidate());

        Label<String> iteractionLabelHeader = uiComponents.create(Label.NAME);
        iteractionLabelHeader.setHtmlEnabled(true);
        iteractionLabelHeader.setStyleName("h3");
        iteractionLabelHeader.setValue("Взаимодействия:");

        Label<String> resumeLabelHeader = uiComponents.create(Label.NAME);
        resumeLabelHeader.setHtmlEnabled(true);
        resumeLabelHeader.setStyleName("h3");
        resumeLabelHeader.setValue("Взаимодействия:");

        Component closeButton = createCloseButton(entity);
        Component editButton = createEditButton(entity);
        Component newIteraction = createNewIteractionButton(entity);
        Component copyLastIteraction = createButtonCopyLastIteraction(entity);
        Component listIteraction = createListIteractionButton(entity);
        Component cvSimpleBrowseButton = createCvSimpleBrowse(entity);
        Component popupButtonCopyLastInteraction = createPopupButtonCopyLastInteraction(entity);

        Label<String> cvLabelHeader = uiComponents.create(Label.NAME);
        cvLabelHeader.setHtmlEnabled(true);
        cvLabelHeader.setStyleName("h3");
        cvLabelHeader.setValue("Резюме:");

        Component newResumeButton = addNewResume(entity.getJobCandidate());
        Component editLastResumeButton = editLastResume(entity.getJobCandidate());

        headerBox.add(infoLabel);
        headerBox.add(candidateStatusLabel);

        headerBox.add(candidateTitle);
        headerBox.add(editButton);

        if (suitableButton != null)
            headerBox.add(suitableButton);

        headerBox.add(iteractionLabelHeader);
        headerBox.add(newIteraction);
        headerBox.add(copyLastIteraction);
        headerBox.add(listIteraction);
        headerBox.add(popupButtonCopyLastInteraction);

        headerBox.add(cvLabelHeader);
        headerBox.add(newResumeButton);
        headerBox.add(editLastResumeButton);
        headerBox.add(cvSimpleBrowseButton);

        headerBox.add(closeButton);
        headerBox.expand(infoLabel);
        headerBox.setSpacing(true);

        mainLayout.add(headerBox);
        mainLayout.add(header2Box);

        jobCanidateDetailScreenFragment.setVisibleLogo();
        jobCanidateDetailScreenFragment.setLastSalaryLabel("Зарплатные ожидания");
        jobCanidateDetailScreenFragment.setStatistics();
        jobCanidateDetailScreenFragment.setLinkButtonTelegrem();
        jobCanidateDetailScreenFragment.setLinkButtonTelegremGroup();
        jobCanidateDetailScreenFragment.setLinkButtonEmail();
        jobCanidateDetailScreenFragment.setLinkButtonSkype();
        jobCanidateDetailScreenFragment.setStatisticsLabel();

        Fragment fragment = jobCanidateDetailScreenFragment.getFragment();
        fragment.setWidth("100%");
        fragment.setAlignment(Component.Alignment.BOTTOM_LEFT);
        mainLayout.add(fragment);

        Skillsbar skillBoxFragment = fragments.create(this, Skillsbar.class);
        if (skillBoxFragment.generateSkillLabels(getLastCVText(
                personelReservesTable.getSingleSelected().getJobCandidate()))) {
            mainLayout.add(skillBoxFragment.getFragment());
        }

        mainLayout.expand(fragment);

        return mainLayout;
    }

    private Component createCloseButton(PersonelReserve entity) {
        Button closeButton = uiComponents.create(Button.class);
        closeButton.setDescription(messageBundle.getMessage("msgClose"));
        closeButton.setIcon("icons/close.png");
        BaseAction closeAction = new BaseAction("closeAction")
                .withHandler(actionPerformedEvent ->
                        personelReservesTable.setDetailsVisible(entity, false))
                .withCaption("");
        closeButton.setAction(closeAction);
        return closeButton;
    } */
}