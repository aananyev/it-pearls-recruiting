package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.core.InteractionService;
import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.screens.candidatecv.CandidateCVEdit;
import com.company.itpearls.web.screens.candidatecv.CandidateCVSimpleBrowse;
import com.company.itpearls.web.screens.iteractionlist.IteractionListEdit;
import com.company.itpearls.web.screens.iteractionlist.IteractionListSimpleBrowse;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataComponents;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

@UiController("itpearls_JobCandidate.browse")
@UiDescriptor("job-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class JobCandidateBrowse extends StandardLookup<JobCandidate> {

    private static final String RECRUTIER_GROUP = "Хантинг";
    private static final String RESEARCHER_GROUP = "Ресерчинг";

    @Inject
    private CheckBox checkBoxShowOnlyMy;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox checkBoxOnWork;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private Button buttonExcel;
    @Inject
    private DataGrid<JobCandidate> jobCandidatesTable;
    @Inject
    private DataManager dataManager;
    private String QUERY_RESUME = "select e from itpearls_CandidateCV e where e.candidate = :candidate";
    @Inject
    private UiComponents uiComponents;

    private List<IteractionList> iteractionList = new ArrayList<>();
    @Inject
    private Fragments fragments;
    @Inject
    private DataComponents dataComponents;
    @Inject
    private Screens screens;
    @Inject
    private Notifications notifications;
    @Inject
    private Dialogs dialogs;

    private CollectionContainer<IteractionList> iteractionListDc;
    private CollectionLoader<IteractionList> iteractionListDl;
    private CollectionContainer<CandidateCV> candidateCVDc;
    private CollectionLoader<CandidateCV> candidateCVDl;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private LookupField ratingFieldNotLower;
    @Inject
    private CollectionContainer<JobCandidate> jobCandidatesDc;
    @Inject
    private CheckBox withCVCheckBox;
    @Inject
    private InteractionService interactionService;
    @Inject
    private UserSessionSource userSessionSource;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (userSession.getUser().getGroup().getName().equals("Стажер")) {
            jobCandidatesDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");

            checkBoxShowOnlyMy.setValue(true);
            checkBoxShowOnlyMy.setEditable(false);
        }

        checkBoxOnWork.setValue(false);
        jobCandidatesDl.removeParameter("param1");
        jobCandidatesDl.removeParameter("param3");

        jobCandidatesDl.load();

        buttonExcel.setVisible(getRoleService.isUserRoles(userSession.getUser(), "Manager"));
    }

    @Subscribe("checkBoxOnWork")
    public void onCheckBoxOnWorkValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (!checkBoxOnWork.getValue()) {
            jobCandidatesDl.removeParameter("param1");
            jobCandidatesDl.removeParameter("param3");
        } else {
            jobCandidatesDl.setParameter("param1", null);
            jobCandidatesDl.setParameter("param3", 10);
        }

        jobCandidatesDl.load();

    }

    @Subscribe("checkBoxShowOnlyMy")
    public void onCheckBoxShowOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxShowOnlyMy.getValue()) {
            jobCandidatesDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        } else {
            jobCandidatesDl.removeParameter("userName");
        }

        jobCandidatesDl.load();
    }

    @Install(to = "jobCandidatesTable.status", subject = "styleProvider")
    private String jobCandidatesTableStatusStyleProvider(JobCandidate jobCandidate) {
        Integer s = getPictString(jobCandidate);
        String retStr = "";

        if (s != null) {
            switch (s) {
                case 0: // WHITE
                    retStr = "pic-center-large-grey";
                    break;
                case 1: // red
                    retStr = "pic-center-large-red";
                    break;
                case 2: // yellow
                    retStr = "pic-center-large-yellow";
                    break;
                case 3: // green
                    retStr = "pic-center-large-green";
                    break;
                case 4: // to client
                    retStr = "pic-center-large-grey";
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10: // recruiting
                    retStr = "pic-center-large-grey";
                    break;
                default:
                    retStr = "pic-center-large";
                    break;
            }
        }

        return retStr;
    }


    @Install(to = "jobCandidatesTable.lastIteraction", subject = "columnGenerator")
    private String jobCandidatesTableLastIteractionColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        IteractionList iteractionList = getLastIteraction(event.getItem());

        String date = null;

        try {
            date = new SimpleDateFormat("dd-MM-yyyy").format(iteractionList.getDateIteraction());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        String retStr = "";
        Boolean checkBlockCandidate = event.getItem().getBlockCandidate() == null ? false : event.getItem().getBlockCandidate();

        if( checkBlockCandidate != null) {
            if (checkBlockCandidate != true) {
                if (iteractionList != null) {
                    Calendar calendar = Calendar.getInstance();

                    if (iteractionList.getDateIteraction() != null) {
                        calendar.setTime(iteractionList.getDateIteraction());
                    } else {
                        calendar.setTime(event.getItem().getCreateTs());
                    }

                    calendar.add(Calendar.MONTH, 1);

                    Calendar calendar1 = Calendar.getInstance();

                    if (calendar.after(calendar1)) {
                        if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
                            retStr = "button_table_red";
                        } else {
                            retStr = "button_table_yellow";
                        }
                    } else {
                        retStr = "button_table_green";
                    }
                } else {
                    retStr = "button_table_white";
                }
            } else {
                retStr = "button_table_black";
            }
        } else {
            retStr = "button_table_green";
        }

        return
                "<div class=\"" +
                        retStr
                        + "\">" +
                        (date != null ? date : "нет")
                        + "</div>"
                ;
    }

    @Install(to = "jobCandidatesTable.lastIteraction", subject = "styleProvider")
    private String jobCandidatesTableLastIteractionStyleProvider(JobCandidate jobCandidate) {
        String retStr = null;

        IteractionList iteractionList = getLastIteraction(jobCandidate);

        if (iteractionList != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(iteractionList.getDateIteraction());
            calendar.add(Calendar.MONTH, 1);

            Calendar calendar1 = Calendar.getInstance();

            if (calendar.after(calendar1)) {
                if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
                    retStr = "button_table_red";
                } else {
                    retStr = "button_table_yellow";
                }
            } else {
                retStr = "button_table_green";
            }

            return retStr;
        } else {
            return "button_table_yellow";
        }
    }

    @Install(to = "jobCandidatesTable.lastIteraction", subject = "descriptionProvider")
    private String jobCandidatesTableLastIteractionDescriptionProvider(JobCandidate jobCandidate) {
        IteractionList iteractionList = getLastIteraction(jobCandidate);
        String recrutierName = "";

        if (iteractionList != null) {
            if (iteractionList.getRecrutier() != null) {
                if (iteractionList.getRecrutier().getName() != null) {
                    recrutierName = iteractionList.getRecrutier().getName();
                }
            }
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Boolean checkBlockCandidate = jobCandidate.getBlockCandidate() == null ? false : jobCandidate.getBlockCandidate();

        if (!checkBlockCandidate) {
            return iteractionList != null ?
                    simpleDateFormat.format(iteractionList.getDateIteraction())
                            + "\n"
                            + iteractionList.getIteractionType().getIterationName()
                            + "\n"
                            + recrutierName : "";
        } else {
            return "ЗАПРЕЩЕНО ВЗАИМОДЕЙСТВИЕ С КАНДИДАТОМ";
        }
    }

    private IteractionList getLastIteraction(JobCandidate jobCandidate) {
        if (jobCandidate.getIteractionList() != null) {
            IteractionList maxIteraction = null;

            for (IteractionList iteractionList : jobCandidate.getIteractionList()) {
                if (maxIteraction == null)
                    maxIteraction = iteractionList;

                if(iteractionList.getNumberIteraction() != null) {
                    if (maxIteraction.getNumberIteraction().compareTo(iteractionList.getNumberIteraction()) < 0) {
                        maxIteraction = iteractionList;
                    }
                }
            }

            return maxIteraction;
        } else
            return null;
    }

    @Install(to = "jobCandidatesTable.resume", subject = "columnGenerator")
    private Icons.Icon jobCandidatesTableResumeColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        String retStr = "";

        try {
            if (event.getItem().getCandidateCv() != null) {
                if (event.getItem().getCandidateCv().size() == 0) {
                    retStr = "FILE";
                } else {
                    retStr = "FILE_TEXT";
                }
            } else {
                retStr = "FILE";
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return CubaIcon.valueOf(retStr);
    }

    @Install(to = "jobCandidatesTable.resume", subject = "styleProvider")
    private String jobCandidatesTableResumeStyleProvider(JobCandidate jobCandidate) {
        String retStr = "";

        if (dataManager.loadValues(QUERY_RESUME)
                .parameter("candidate", jobCandidate)
                .list()
                .size() == 0) {
            retStr = "pic-center-large-red";
        } else {
            retStr = "pic-center-large-green";
        }

        return retStr;
    }

    @Install(to = "jobCandidatesTable", subject = "detailsGenerator")
    private Component jobCandidatesTableDetailsGenerator(JobCandidate entity) {
        VBoxLayout mainLayout = uiComponents.create(VBoxLayout.NAME);
        mainLayout.setWidth("100%");
        mainLayout.setMargin(true);

        JobCanidateDetailScreenFragment jobCanidateDetailScreenFragment = fragments.create(this,
                JobCanidateDetailScreenFragment.class);
        jobCanidateDetailScreenFragment.setJobCandidate(entity);

        HBoxLayout headerBox = uiComponents.create(HBoxLayout.NAME);
        headerBox.setWidthAuto();
        headerBox.setWidth("100%");
        headerBox.setHeight("100%");

        HBoxLayout header2Box = uiComponents.create(HBoxLayout.NAME);
        header2Box.setWidth("100%");
        header2Box.setHeight("100%");

        Label<String> infoLabel = uiComponents.create(Label.NAME);
        infoLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        infoLabel.setHtmlEnabled(true);
        infoLabel.setStyleName("h3");
        infoLabel.setValue("Информация о кандидате:");

        Boolean checkBlockCandidate = !(jobCandidatesTable.getSingleSelected().getBlockCandidate() == null
                ? false : jobCandidatesTable.getSingleSelected().getBlockCandidate());
        Label<String> candidateStatusLabel = uiComponents.create(Label.NAME);
        candidateStatusLabel.setHtmlEnabled(true);
        candidateStatusLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        candidateStatusLabel.setStyleName(checkBlockCandidate ? "h2" : "h2-red");
        candidateStatusLabel.setValue(checkBlockCandidate ? "Нормально" : "Заблокирован");

        Label<String> candidateTitle = uiComponents.create(Label.NAME);
        candidateTitle.setHtmlEnabled(true);
        candidateTitle.setStyleName("h3");
        candidateTitle.setValue("Кандидат:");

        Component suitableButton = findSuitableButton(entity);

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

        Component newResumeButton = addNewResume(entity);
        Component editLastResumeButton = editLastResume(entity);

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
        mainLayout.expand(fragment);

        return mainLayout;
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

    private Component createPopupButtonCopyLastInteraction(JobCandidate entity) {
        PopupButton lastInteractionPopupButton = uiComponents.create(PopupButton.class);
        lastInteractionPopupButton.setDescription("Копировать последнее взаимодействие с кандидатом");
        lastInteractionPopupButton.setIconFromSet(CubaIcon.FILE_TEXT);

        jobCandidatesTable.addSelectionListener(e -> {
            if(jobCandidatesTable.getSingleSelected() == null) {
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
        if (jobCandidatesTable.getSingleSelected() != null) {
            screenBuilders.editor(IteractionList.class, this)
                    .newEntity()
                    .withInitializer(e -> {
                        e.setCandidate(jobCandidatesTable.getSingleSelected());
                        e.setIteractionType(iteraction);
                        BigDecimal maxNumberIteraction = BigDecimal.ZERO;
                        IteractionList lastIteraction = null;

                        if (jobCandidatesTable.getSingleSelected() != null) {
                            for (IteractionList list : jobCandidatesTable
                                .getSingleSelected()
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

    private Component createButtonCopyLastIteraction(JobCandidate entity) {
        Button lastIteractionButton = uiComponents.create(Button.class);
        lastIteractionButton.setDescription("Копировать последнее взаимодействие с кандидатом");
        lastIteractionButton.setIconFromSet(CubaIcon.COPY);


        lastIteractionButton.setAction(new BaseAction("copyLastIteraction")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(IteractionList.class, this)
                            .newEntity()
                            .withScreenClass(IteractionListEdit.class)
                            .withInitializer(iteractionList1 -> {
                                iteractionList1.setCandidate(jobCandidatesTable.getSingleSelected());

                                BigDecimal maxNumberIteraction = BigDecimal.ZERO;
                                IteractionList lastIteraction = null;

                                if (jobCandidatesTable.getSingleSelected() != null) {
                                    for (IteractionList list : jobCandidatesTable
                                            .getSingleSelected()
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
                                jobCandidatesDl.load();
                            })
                            .build()
                            .show();
                }));

        return lastIteractionButton;
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

    private Component getLastCVtoClipboard(JobCandidate entity) {
        Button getLastLetterButton = uiComponents.create(Button.class);
        getLastLetterButton.setDescription("Получить последнее сопроводительное письмо");
        getLastLetterButton.setIconFromSet(CubaIcon.TABLET);

        getLastLetterButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    notifications.create().withCaption("Пока не реализовано");
                }));

        return getLastLetterButton;
    }

    private Component getLastLetterToClipboard(JobCandidate entity) {
        Button getLastResumeButton = uiComponents.create(Button.class);
        getLastResumeButton.setDescription("Получить последнее резюме");
        getLastResumeButton.setIconFromSet(CubaIcon.GET_POCKET);

        getLastResumeButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    notifications.create().withCaption("Пока не реализовано");
                }));

        return getLastResumeButton;
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

                        screenBuilders.editor(CandidateCV.class, this)
                                .withScreenClass(CandidateCVEdit.class)
                                .editEntity(lastCV)
                                .withContainer(candidateCVDc)
                                .build()
                                .show();

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

    private Component createListIteractionButton(JobCandidate entity) {
        Button listIteractionButton = uiComponents.create(Button.class);
        listIteractionButton.setDescription("Список взаимодействий");
        listIteractionButton.setIconFromSet(CubaIcon.FILE_TEXT_O);
        createDataComponents();

        listIteractionButton.setAction(new BaseAction("listIteraction")
                .withHandler(actionPerformedEvent -> {
                    iteractionListDl.setParameter("candidate", entity);
                    iteractionListDl.load();

                    IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
                    iteractionListSimpleBrowse.setSelectedCandidate(entity);
                    iteractionListSimpleBrowse.setJobCandidate(entity);
                    screens.show(iteractionListSimpleBrowse);

                }));

        return listIteractionButton;

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

    private Component createNewIteractionButton(JobCandidate entity) {
        Button newIteractionButton = uiComponents.create(Button.class);
        newIteractionButton.setDescription("Новое взаимодействие");
        newIteractionButton.setIconFromSet(CubaIcon.CREATE_ACTION);


        newIteractionButton.setAction(new BaseAction("newIteraction")
                .withHandler(actionPerformedEvent ->
                        screenBuilders.editor(IteractionList.class, this)
                                .newEntity()
                                .withScreenClass(IteractionListEdit.class)
                                .withInitializer(iteractionList1 -> {
                                    iteractionList1.setCandidate(jobCandidatesTable.getSingleSelected());
                                })
                                .withAfterCloseListener(e -> {
                                    jobCandidatesDl.load();
                                })
                                .build()
                                .show())
        );

        return newIteractionButton;
    }

    private HBoxLayout getLastIteraction(List<IteractionList> iteractionList) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.NAME);
        Label<String> dateIteraction = uiComponents.create(Label.NAME);
        Label<String> retLab = uiComponents.create(Label.NAME);
        Label<String> add = uiComponents.create((Label.NAME));
        String addInfo = "";

        if (iteractionList.size() != 0) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

            dateIteraction.setValue(simpleDateFormat.format(iteractionList.get(0).getDateIteraction()) + ":");
            retLab.setValue(iteractionList.get(0).getIteractionType().getIterationName());

            if (iteractionList.get(0).getAddDate() != null) {
                add.setValue(" (" + simpleDateFormat.format(iteractionList.get(0).getAddDate()) + ")");
            }
        } else {
            return null;
        }

        retBox.add(dateIteraction);
        retBox.add(retLab);
        if (!addInfo.equals(""))
            retBox.add(add);

        return retBox;
    }

    private void setIteraction(VBoxLayout iteraction) {
    }

    private void setStatistics(VBoxLayout statistics) {
        Label<String> titleStatistics = uiComponents.create(Label.NAME);
        titleStatistics.setValue("Статистика по кандидату");
        titleStatistics.setStyleName("h3");

        String QUERY_LAST_SALARY = "select e from itpearls_IteractionList e where e.iteractionType = " +
                "(select f from itpearls_Iteraction f where f.iterationName like :iteractionName) and " +
                "e.candidate = :candidate";
        String iteractionName = "Зарплатные ожидания";

        IteractionList iteractionList = null;

        try {
            iteractionList = dataManager.load(IteractionList.class)
                    .query(QUERY_LAST_SALARY)
                    .cacheable(true)
                    .view("iteractionList-view")
                    .parameter("iteractionName", iteractionName)
                    .parameter("candidate", jobCandidatesTable.getSingleSelected())
                    .one();
        } catch (Exception e) {
        }


        VBoxLayout vBoxLayout = uiComponents.create(VBoxLayout.NAME);
        vBoxLayout.setWidth("100%");

        if (iteractionList != null) {
            Label<String> lastSalary = uiComponents.create(Label.NAME);
            lastSalary.setValue("Зарплатные ожидания: " + iteractionList.getAddString());
            vBoxLayout.add(lastSalary);
        }

        statistics.add(titleStatistics);
        statistics.add(vBoxLayout);
    }

    private String getLastProject(List<IteractionList> iteractionList) {
        String retStr = "";

        for (IteractionList a : iteractionList) {
            if (a.getVacancy().getProjectName() != null)
                retStr = a.getVacancy().getProjectName().getProjectName();
            else
                retStr = "";
        }

        return retStr;
    }

    private String getLastContacter(List<IteractionList> iteractionList, String contecter) {
        for (IteractionList a : iteractionList) {
            if (a.getRecrutier().getGroup().getName() != null) {
                if (a.getRecrutier().getGroup().getName().equals(contecter)) {
                    return a.getRecrutier().getName();
                }
            }
        }

        return "";
    }


    private String getLastVacansy(List<IteractionList> iteractionList) {
        for (IteractionList a : iteractionList) {
            return a.getVacancy() != null ? a.getVacancy().getVacansyName() : "";
        }

        return "";
    }

    private String getLastIteraction(List<IteractionList> iteractionList, String contecter) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

        for (IteractionList a : iteractionList) {
            if (a.getRecrutier().getGroup().getName() != null) {
                if (a.getRecrutier().getGroup().getName().equals(contecter)) {
                    return simpleDateFormat.format(a.getDateIteraction());
                }
            }
        }

        return "";
    }

    private List<IteractionList> getIteractionLists(JobCandidate singleSelected) {
        String QUERY_GET_LASTRECRUTIER = "select e from itpearls_IteractionList e " +
                "where e.candidate = :candidate " +
                "order by e.numberIteraction desc";

        List<IteractionList> listIteracion = dataManager.load(IteractionList.class)
                .query(QUERY_GET_LASTRECRUTIER)
                .cacheable(true)
                .parameter("candidate", singleSelected)
                .view("iteractionList-job-candidate")
                .list();

        return listIteracion;

    }

    private Component createEditButton(JobCandidate entity) {
        Button editButton = uiComponents.create(Button.class);
        editButton.setDescription("Редактирование");
        editButton.setIconFromSet(CubaIcon.EDIT_ACTION);

        BaseAction editAction = new BaseAction("edit")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(JobCandidate.class, this)
                            .editEntity(entity)
                            .build()
                            .show();
                })
                .withCaption("");
        editButton.setAction(editAction);
        return editButton;
    }

    private Label<String> setContacts(String labelName) {
        Label<String> label = uiComponents.create(Label.NAME);

        if (labelName != null) {
            label.setValue(labelName);
        }

        return label;
    }


    private Component createCloseButton(JobCandidate entity) {
        Button closeButton = uiComponents.create(Button.class);
        closeButton.setDescription("Закрыть");
        closeButton.setIcon("icons/close.png");
        BaseAction closeAction = new BaseAction("closeAction")
                .withHandler(actionPerformedEvent ->
                        jobCandidatesTable.setDetailsVisible(entity, false))
                .withCaption("");
        closeButton.setAction(closeAction);
        return closeButton;
    }

    @Install(to = "jobCandidatesTable.status", subject = "columnGenerator")
    private Icons.Icon jobCandidatesTableStatusColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Integer s = getPictString(event.getItem());
        String retStr = "";

        if (s != null) {
            switch (s) {
                case 0: // WHITE
                    retStr = "QUESTION_CIRCLE";
                    break;
                case 1: // red
                    retStr = "BOMB";
                    break;
                case 2: // yellow
                    retStr = "BOMB";
                    break;
                case 3: // green
                    retStr = "BOMB";
                    break;
                case 4: // to client
                    retStr = "BOMB";
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10: // recruiting
                    retStr = "BOMB";
                    break;
                default:
                    retStr = "QUESTION_CIRCLE";
                    break;
            }
        }

        return CubaIcon.valueOf(retStr);
    }

    private Integer getPictString(JobCandidate jobCandidate) {
        // если только имя и отчество - красный сигнал светофора
        // если имя, день рождения и один из контактов - желтый
        // если больше двух контактов - зеленый, если нет др - все равно желтый
        if (((jobCandidate.getEmail() != null && jobCandidate.getPhone() != null && jobCandidate.getSkypeName() != null) ||
                (jobCandidate.getEmail() != null && jobCandidate.getPhone() != null) ||
                (jobCandidate.getEmail() != null && jobCandidate.getSkypeName() != null) ||
                (jobCandidate.getPhone() != null && jobCandidate.getSkypeName() != null)) &&
                jobCandidate.getBirdhDate() != null) {
            return 3;
        } else {
            if (jobCandidate.getPhone() != null ||
                    jobCandidate.getEmail() != null ||
                    jobCandidate.getSkypeName() != null) {
                return 2;
            } else {
                if (jobCandidate.getFirstName() == null ||
                        jobCandidate.getMiddleName() == null ||
                        jobCandidate.getCityOfResidence() == null ||
                        jobCandidate.getCurrentCompany() == null ||
                        jobCandidate.getPersonPosition() == null) {
                    return 0;
                }
            }
        }

        return 0;
    }

    public void onButtonSubscribeClick() {
        screenBuilders.editor(SubscribeCandidateAction.class, this)
                .newEntity()
                .withInitializer(e -> {
                    e.setCandidate(jobCandidatesTable.getSingleSelected());
                    e.setSubscriber(userSession.getUser());
                    e.setStartDate(new Date());
                })
                .withOpenMode(OpenMode.DIALOG)
                .build()
                .show();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        jobCandidatesTable.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent -> {
                    jobCandidatesTable.setDetailsVisible(jobCandidatesTable.getSingleSelected(), true);
                }));


        candidateImageColumnRenderer();

        DataGrid.ClickableTextRenderer<JobCandidate> jobCandidatesTableLastIteractionRenderer =
                jobCandidatesTable.createRenderer(DataGrid.ClickableTextRenderer.class);

        jobCandidatesTableLastIteractionRenderer.setRendererClickListener(clickableTextRendererClickEvent -> {
            IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
            iteractionListSimpleBrowse.setSelectedCandidate(clickableTextRendererClickEvent.getItem());
            screens.show(iteractionListSimpleBrowse);
        });

        jobCandidatesTable.getColumn("lastIteraction").setStyleProvider(e -> {
            IteractionList iteractionList = getLastIteraction(e);
            String retStr = "";

            if (iteractionList != null) {
                Calendar calendar = Calendar.getInstance();

                if (iteractionList.getDateIteraction() != null) {
                    calendar.setTime(iteractionList.getDateIteraction());
                }

                calendar.add(Calendar.MONTH, 1);

                Calendar calendar1 = Calendar.getInstance();

                if (calendar.after(calendar1)) {
                    if(iteractionList.getRecrutier() != null) {
                        if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
                            retStr = "button_table_red";
                        } else {
                            retStr = "button_table_yellow";
                        }
                    } else {
                        retStr = "button_table_white";
                    }
                } else {
                    retStr = "button_table_green";
                }
            } else {
                retStr = "button_table_white";
            }

            return retStr;
        });


//        jobCandidatesTable.getColumn("lastIteraction")
//                .setRenderer(jobCandidatesTableLastIteractionRenderer);
        jobCandidatesTable.getColumn("lastIteraction")
                .setRenderer(jobCandidatesTable.createRenderer(DataGrid.HtmlRenderer.class));

        setRatingField();
        setWithCVCheckBox();
    }

    private void setWithCVCheckBox() {
        withCVCheckBox.addValueChangeListener(e -> {
            if (withCVCheckBox.getValue() != null) {
                if (!withCVCheckBox.getValue()) {
                    if (ratingFieldNotLower.getValue() == null) {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "order by e.secondName, e.firstName");
                    } else {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select f.candidate "
                                + "from itpearls_IteractionList f "
                                + "where f.candidate = e) "
                                + "and e in (select g.candidate from itpearls_IteractionList g where g.candidate = e and g.rating >= " + ratingFieldNotLower.getValue().toString() + ") "
                                + "order by e.secondName, e.firstName");
                    }
                } else {
                    if (ratingFieldNotLower.getValue() == null) {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select g.candidate "
                                + "from itpearls_CandidateCV g "
                                + "where g.candidate = e) "
                                + "order by e.secondName, e.firstName");
                    } else {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select g.candidate "
                                + "from itpearls_CandidateCV g "
                                + "where g.candidate = e) "
                                + "and e in (select g.candidate from itpearls_IteractionList g where g.candidate = e and g.rating >= " + ratingFieldNotLower.getValue().toString() + ") "
                                + "order by e.secondName, e.firstName");
                    }
                }

                jobCandidatesDl.load();
            }
        });
    }


    private void candidateImageColumnRenderer() {
        jobCandidatesTable.addGeneratedColumn("fileImageFace", entity -> {
            Image image = uiComponents.create(Image.NAME);

            if (entity.getItem().getFileImageFace() != null) {
                try {
                    image.setValueSource(new ContainerValueSource<JobCandidate, FileDescriptor>(entity.getContainer(),
                            "fileImageFace"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                image.setWidth("50px");
//            image.setStyleName("image-candidate-face-little-image");
                image.setStyleName("round-photo");
            } else {
                image.setStyleName("pic-center");
            }
            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);
            return image;
        });
    }

    @Install(to = "jobCandidatesTable.personPosition", subject = "descriptionProvider")
    private String jobCandidatesTablePersonPositionDescriptionProvider(JobCandidate jobCandidate) {
        String retStr = "";

        if (jobCandidate.getPositionList() != null) {
            for (JobCandidatePositionLists s : jobCandidate.getPositionList()) {
                if (!retStr.equals("")) {
                    retStr = retStr + ",";
                }

                retStr = retStr + s.getPositionList().getPositionRuName();
            }
        }

        return retStr;
    }

    @Install(to = "jobCandidatesTable.rating", subject = "columnGenerator")
    private String jobCandidatesTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        return avgRating(event.getItem());
    }

    private String avgRating(JobCandidate jobCandidate) {
        float countRating = 0,
                sumRating = 0;

        if (jobCandidate.getIteractionList() != null) {
            for (IteractionList iteractionList : jobCandidate.getIteractionList()) {
                if (iteractionList.getRating() != null) {
                    countRating++;
                    sumRating = sumRating + iteractionList.getRating();
                }
            }
        }

        if (countRating != 0) {
            int avgRating = (int) (sumRating / countRating + 1);

            return starsAndOtherService.setStars(avgRating);
        } else
            return "";
    }

    @Install(to = "jobCandidatesTable.rating", subject = "styleProvider")
    private String jobCandidatesTableRatingStyleProvider(JobCandidate jobCandidate) {
        String retStr = "rating";
        String avg = avgRating(jobCandidate);
        if (!avg.equals("")) {
            String s = avg.substring(0, 1);

            switch (s) {
                case "1":
                    retStr = retStr + "_red_" + s;
                    break;
                case "2":
                    retStr = retStr + "_orange" + s;
                    break;
                case "3":
                    retStr = retStr + "_yellow_" + s;
                    break;
                case "4":
                    retStr = retStr + "_green_" + s;
                    break;
                case "5":
                    retStr = retStr + "_blue_" + s;
                    break;
                default:
                    break;
            }

            return retStr;
        } else
            return null;
    }

    private void setRatingField() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(starsAndOtherService.setStars(1) + " Полный негатив", 0);
        map.put(starsAndOtherService.setStars(2) + " Сомнительно", 1);
        map.put(starsAndOtherService.setStars(3) + " Нейтрально", 2);
        map.put(starsAndOtherService.setStars(4) + " Положительно", 3);
        map.put(starsAndOtherService.setStars(5) + " Отлично!", 4);
        ratingFieldNotLower.setOptionsMap(map);

        ratingFieldNotLower.addValueChangeListener(e -> {
            if (ratingFieldNotLower.getValue() != null) {
                if (withCVCheckBox.getValue() != null) {
                    if (!withCVCheckBox.getValue()) {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "order by e.secondName, e.firstName");
                    } else {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select f.candidate "
                                + "from itpearls_IteractionList f "
                                + "where f.candidate = e) "
                                + "and e in (select g.candidate from itpearls_IteractionList g where g.candidate = e and g.rating >= " + ratingFieldNotLower.getValue().toString() + ") "
                                + "order by e.secondName, e.firstName");
                    }
                } else {
                    jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                            + "order by e.secondName, e.firstName");
                }
            } else {
                if (withCVCheckBox.getValue() != null) {
                    if (!withCVCheckBox.getValue()) {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select g.candidate "
                                + "from itpearls_CandidateCV g "
                                + "where g.candidate = e) "
                                + "order by e.secondName, e.firstName");
                    } else {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select g.candidate "
                                + "from itpearls_CandidateCV g "
                                + "where g.candidate = e) "
                                + "and e in (select g.candidate from itpearls_IteractionList g where g.candidate = e and g.rating >= " + ratingFieldNotLower.getValue().toString() + ") "
                                + "order by e.secondName, e.firstName");
                    }
                } else {
                    jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                            + "where e in (select g.candidate "
                            + "from itpearls_CandidateCV g "
                            + "where g.candidate = e) "
                            + "order by e.secondName, e.firstName");
                }
            }

            jobCandidatesDl.load();
        });
    }

    @Subscribe("showOnlyWithMyParticipationCheckBox")
    public void onShowOnlyWithMyParticipationCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            jobCandidatesDl.setParameter("recrutier", userSession.getUser());
        } else {
            jobCandidatesDl.removeParameter("recrutier");
        }

        jobCandidatesDl.load();
    }

    public void quickLoadCVButton() {
    }
}