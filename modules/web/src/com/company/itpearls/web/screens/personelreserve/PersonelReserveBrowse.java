package com.company.itpearls.web.screens.personelreserve;

import com.company.itpearls.core.InteractionService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.candidatecv.CandidateCVEdit;
import com.company.itpearls.web.screens.candidatecv.CandidateCVSimpleBrowse;
import com.company.itpearls.web.screens.fragments.Skillsbar;
import com.company.itpearls.web.screens.iteractionlist.IteractionListEdit;
import com.company.itpearls.web.screens.iteractionlist.IteractionListSimpleBrowse;
import com.company.itpearls.web.screens.jobcandidate.FindSuitable;
import com.company.itpearls.web.screens.jobcandidate.JobCanidateDetailScreenFragment;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@UiController("itpearls_PersonelReserve.browse")
@UiDescriptor("personel-reserve-browse.xml")
@LookupComponent("personelReservesTable")
@LoadDataBeforeShow
public class PersonelReserveBrowse extends StandardLookup<PersonelReserve> {
    @Inject
    private CheckBox allCandidatesCheckBox;
    @Inject
    private CollectionLoader<PersonelReserve> personelReservesDl;
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

    private final static String statusColumn = "statusColumn";
    private final static String inWorkColumn = "inWorkColumn";
    private final static String faceImage = "faceImage";
    private final static String tableWordWrapStyle = "table-wordwrap";
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

    private CollectionContainer<IteractionList> iteractionListDc;
    private CollectionLoader<IteractionList> iteractionListDl;
    private CollectionContainer<CandidateCV> candidateCVDc;
    private CollectionLoader<CandidateCV> candidateCVDl;
    @Inject
    private InteractionService interactionService;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Dialogs dialogs;

    @Subscribe
    public void onInit(InitEvent event) {
//        candidateImageColumnRenderer();
        initInWorkColumnRenderer();
        initStatusColumnRenderer();

        personelReservesTable.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent -> {
                    personelReservesTable.setDetailsVisible(personelReservesTable.getSingleSelected(), true);
                }));
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        allCandidatesCheckBox.setValue(true);
        activesCheckBox.setValue(true);
        inNotWorkCheckBox.setValue(true);
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
                            + iteractionList.getIteractionType().getIterationName()
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

            retHBoxLayout.setAlignment(Component.Alignment.MIDDLE_CENTER);

            retHBoxLayout.setWidthFull();
            retHBoxLayout.setHeightFull();
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
        if (event.getValue()) {
            personelReservesDl.setParameter("recruter", userSession.getUser());
        } else {
            personelReservesDl.removeParameter("recruter");
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

    @Install(to = "personelReservesTable.jobCandidate", subject = "styleProvider")
    private String personelReservesTableJobCandidateStyleProvider(PersonelReserve personelReserve) {
        return tableWordWrapStyle;
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

    @Install(to = "personelReservesTable", subject = "detailsGenerator")
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
        Component cvSimpleBrowseButton = createCvSimpleBrowse(entity.getJobCandidate());
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
    }


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

    private Component createPopupButtonCopyLastInteraction(JobCandidate entity) {
        PopupButton lastInteractionPopupButton = uiComponents.create(PopupButton.class);
        lastInteractionPopupButton.setDescription("Копировать последнее взаимодействие с кандидатом");
        lastInteractionPopupButton.setIconFromSet(CubaIcon.FILE_TEXT);

        personelReservesTable.addSelectionListener(e -> {
            if (personelReservesTable.getSingleSelected() == null) {
                lastInteractionPopupButton.setEnabled(false);
            } else {
                lastInteractionPopupButton.setEnabled(true);
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

    private Component createCvSimpleBrowse(JobCandidate entity) {
        Button cvSimpleBrowseButton = uiComponents.create(Button.class);
        cvSimpleBrowseButton.setDescription("Копировать резюме кандидата");
        cvSimpleBrowseButton.setIconFromSet(CubaIcon.FILE_TEXT_O);

        createDataComponentsResume();

        cvSimpleBrowseButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    candidateCVDl.setParameter("candidate", entity);
                    candidateCVDl.load();

                    CandidateCVSimpleBrowse candidateCVSimpleBrowse = screens.create(CandidateCVSimpleBrowse.class);
                    candidateCVSimpleBrowse.setSelectedCandidate(entity);
                    candidateCVSimpleBrowse.setJobCandidate(entity);
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


    private Component createCloseButton(PersonelReserve entity) {
        Button closeButton = uiComponents.create(Button.class);
        closeButton.setDescription("Закрыть");
        closeButton.setIcon("icons/close.png");
        BaseAction closeAction = new BaseAction("closeAction")
                .withHandler(actionPerformedEvent ->
                        personelReservesTable.setDetailsVisible(entity, false))
                .withCaption("");
        closeButton.setAction(closeAction);
        return closeButton;
    }

    public void closePersonalReserveButtonInvoke() {
    }
}