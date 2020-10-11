package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.screens.iteractionlist.IteractionListEdit;
import com.company.itpearls.web.screens.iteractionlist.IteractionListSimpleBrowse;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    @Install(to = "jobCandidatesTable.photo", subject = "columnGenerator")
    private Icons.Icon jobCandidatesTablePhotoColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        String retStr = "";

        if (event.getItem().getFileImageFace() == null) {
            retStr = "MINUS_CIRCLE";
        } else {
            retStr = "PLUS_CIRCLE";
        }

        return CubaIcon.valueOf(retStr);
    }

    @Install(to = "jobCandidatesTable.freeCandidate", subject = "styleProvider")
    private String jobCandidatesTableFreeCandidateStyleProvider(JobCandidate jobCandidate) {
        String retStr = null;

        IteractionList iteractionList = getLastIteraction(jobCandidate);

        if (iteractionList != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(iteractionList.getDateIteraction());
            calendar.add(Calendar.MONTH, 1);

            Calendar calendar1 = Calendar.getInstance();

            if (calendar.after(calendar1)) {
                if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
                    retStr = "pic-center-large-red";
                } else {
                    retStr = "pic-center-large-yellow";
                }
            } else {
                retStr = "pic-center-large-green";
            }

            return retStr;
        } else {
            return "pic-center-large-green";
        }
    }

    @Install(to = "jobCandidatesTable.freeCandidate", subject = "descriptionProvider")
    private String jobCandidatesTableFreeCandidateDescriptionProvider(JobCandidate jobCandidate) {
        IteractionList iteractionList = getLastIteraction(jobCandidate);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

        return iteractionList != null ?
                simpleDateFormat.format(iteractionList.getDateIteraction())
                        + "\n"
                        + iteractionList.getIteractionType().getIterationName()
                        + "\n"
                        + iteractionList.getRecrutier().getName() : "";
    }

    private IteractionList getLastIteraction(JobCandidate jobCandidate) {
        String query = "select e from itpearls_IteractionList e " +
                "where e.numberIteraction = " +
                "(select max(f.numberIteraction) from itpearls_IteractionList f where f.candidate = :candidate) " +
                "and e.candidate = :candidate";

        IteractionList iteractionList = null;

        try {
            iteractionList = dataManager.load(IteractionList.class)
                    .query(query)
                    .parameter("candidate", jobCandidate)
                    .view("iteractionList-view")
                    .one();
        } catch (Exception e) {
        }

        return iteractionList;
    }

    @Install(to = "jobCandidatesTable.freeCandidate", subject = "columnGenerator")
    private Icons.Icon jobCandidatesTableFreeCandidateColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        // FREE_CODE_CAMP check square minus square
        String retSrt = null;

        IteractionList iteractionList = getLastIteraction(event.getItem());

        if (iteractionList != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(iteractionList.getDateIteraction());
            calendar.add(Calendar.MONTH, 1);

            Calendar calendar1 = Calendar.getInstance();

            if (calendar.after(calendar1)) {
                retSrt = "MINUS_SQUARE";
            } else {
                retSrt = "CHECK_SQUARE";
            }

            return CubaIcon.valueOf(retSrt);
        } else {
            return CubaIcon.valueOf("CHECK_SQUARE");
        }
    }

    @Install(to = "jobCandidatesTable.resume", subject = "columnGenerator")
    private Icons.Icon jobCandidatesTableResumeColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        String retStr = "";

        if (dataManager.loadValues(QUERY_RESUME)
                .parameter("candidate", event.getItem())
                .list()
                .size() == 0) {
            retStr = "MINUS_CIRCLE";
        } else {
            retStr = "PLUS_CIRCLE";
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


    @Install(to = "jobCandidatesTable.photo", subject = "styleProvider")
    private String jobCandidatesTablePhotoStyleProvider(JobCandidate jobCandidate) {
        String retStr = "";

        if (jobCandidate.getFileImageFace() == null) {
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

/*
        Image candidatePhoto = uiComponents.create(Image.NAME);
        candidatePhoto.setAlignment(Component.Alignment.TOP_RIGHT);
        candidatePhoto.setHeight("150px");
        candidatePhoto.setScaleMode(Image.ScaleMode.FILL);
        candidatePhoto.setStyleName("widget-border");

        FileDescriptor fileDescriptor = entity.getFileImageFace();

        if (fileDescriptor != null) {
            FileDescriptorResource fileDescriptorResource = candidatePhoto.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(fileDescriptor);
            candidatePhoto.setSource(fileDescriptorResource);
            candidatePhoto.setVisible(true);
        } else {
            candidatePhoto.setVisible(false);
        }
*/
        HBoxLayout headerBox = uiComponents.create(HBoxLayout.NAME);
        headerBox.setWidth("100%");
        headerBox.setHeight("100%");

        HBoxLayout header2Box = uiComponents.create(HBoxLayout.NAME);
        header2Box.setWidth("100%");
        header2Box.setHeight("100%");

        Label infoLabel = uiComponents.create(Label.NAME);
        infoLabel.setHtmlEnabled(true);
        infoLabel.setStyleName("h3");
        infoLabel.setValue("Информация о кандидате:");
/*
        Label personPosition = uiComponents.create(Label.NAME);
        personPosition.setHtmlEnabled(true);
        personPosition.setStyleName("h4");
        personPosition.setValue(entity.getPersonPosition().getPositionRuName()
                + (entity.getPersonPosition().getPositionEnName() != null ? "/" + entity.getPersonPosition().getPositionEnName() : "")
                + ", " + entity.getCityOfResidence().getCityRuName());

        VBoxLayout contacts = uiComponents.create(VBoxLayout.NAME);
        contacts.setHeightAuto();
        contacts.setHeight("100%");

        VBoxLayout iteraction = uiComponents.create(VBoxLayout.NAME);
        iteraction.setHeightAuto();
        iteraction.setWidth("100%");

        VBoxLayout statistics = uiComponents.create(VBoxLayout.NAME);
        statistics.setHeightAuto();
        statistics.setWidth("100%");

        Label titleContacts = uiComponents.create(Label.NAME);
        titleContacts.setValue("Контактная информация");
        titleContacts.setStyleName("h3");

        Label titleIteraction = uiComponents.create(Label.NAME);
        titleIteraction.setValue("Последнее взаимодействия");
        titleIteraction.setStyleName("h3");

        contacts.add(titleContacts);
        contacts.add(personPosition);

        if (entity.getEmail() != null) {
            Label email = uiComponents.create(Label.NAME);
            email.setValue("Email: " + entity.getEmail());
            contacts.add(email);
        }

        if (entity.getPhone() != null) {
            Label phone = uiComponents.create(Label.NAME);
            phone.setValue("Phone: " + entity.getPhone());
            contacts.add(phone);
        }

        if (entity.getTelegramName() != null) {
            Label skype = uiComponents.create(Label.NAME);
            skype.setValue("Skype: " + entity.getSkypeName());
            contacts.add(skype);
        }

        if (entity.getTelegramName() != null) {
            Label telegramm = uiComponents.create(Label.NAME);
            telegramm.setValue("Telegramm: " + entity.getTelegramName());
            contacts.add(telegramm);
        }

        if (entity.getWhatsupName() != null) {
            Label watsup = uiComponents.create(Label.NAME);
            watsup.setValue("WhatsApp: " + entity.getSkypeName());
            contacts.add(watsup);
        }

        if (entity.getWiberName() != null) {
            Label viber = uiComponents.create(Label.NAME);
            viber.setValue("Viber: " + entity.getWiberName());
        }

        iteractionList = getIteractionLists(jobCandidatesTable.getSingleSelected());

        Label lastProject = uiComponents.create(Label.NAME);
        lastProject.setValue(getLastProject(iteractionList));
        lastProject.setStyleName("h4");

        Label vacansyName = uiComponents.create(Label.NAME);
        vacansyName.setValue(getLastVacansy(iteractionList));
        vacansyName.setStyleName("h4");

        String getLastContacter = getLastContacter(iteractionList, RECRUTIER_GROUP);

        Label lastRecrutier = uiComponents.create(Label.NAME);
        lastRecrutier.setValue(!getLastContacter.equals("") ?
                (RECRUTIER_GROUP
                        + ": " + getLastContacter
                        + " (" + getLastIteraction(iteractionList, RECRUTIER_GROUP) + ")") :
                "");

        getLastContacter = getLastContacter(iteractionList, RESEARCHER_GROUP);
        Label lastResearcher = uiComponents.create(Label.NAME);
        lastResearcher.setValue(!getLastContacter.equals("") ?
                (RESEARCHER_GROUP
                        + ": " + getLastContacter
                        + " (" + getLastIteraction(iteractionList, RESEARCHER_GROUP) + ")") : "");

        iteraction.add(titleIteraction);
        if(!getLastProject(iteractionList).equals("")) iteraction.add(lastProject);
        iteraction.add(vacansyName);
        if(getLastIteraction(iteractionList) != null) iteraction.add(getLastIteraction(iteractionList));
        if (!lastRecrutier.getValue().equals("")) iteraction.add(lastRecrutier);
        if (!lastResearcher.getValue().equals("")) iteraction.add(lastResearcher);

        setIteraction(iteraction);
        setStatistics(statistics);

        HBoxLayout hBoxLayout = uiComponents.create(HBoxLayout.NAME);
        hBoxLayout.setHeight("100%");
        hBoxLayout.setWidth("100%");

        hBoxLayout.add(contacts);
        hBoxLayout.add(iteraction);
        hBoxLayout.add(statistics);
        hBoxLayout.add(candidatePhoto);
*/
        Component closeButton = createCloseButton(entity);
        Component editButton = createEditButton(entity);
        Component newIteraction = createNewIteractionButton(entity);
        Component listIteraction = createListIteractionButton(entity);

        headerBox.add(infoLabel);
        headerBox.add(newIteraction);
        headerBox.add(listIteraction);
        headerBox.add(editButton);
        headerBox.add(closeButton);
        headerBox.expand(infoLabel);
        headerBox.setSpacing(true);

        mainLayout.add(headerBox);
        mainLayout.add(header2Box);
//        mainLayout.add(hBoxLayout);
//        mainLayout.expand(hBoxLayout);


        jobCanidateDetailScreenFragment.setVisibleLogo();
        jobCanidateDetailScreenFragment.setLastSalaryLabel("Зарплатные ожидания");
        jobCanidateDetailScreenFragment.setStatistics();

        Fragment fragment = jobCanidateDetailScreenFragment.getFragment();
        fragment.setWidth("100%");
        fragment.setAlignment(Component.Alignment.BOTTOM_LEFT);

        mainLayout.add(fragment);
        mainLayout.expand(fragment);

        return mainLayout;
    }

    private Component createListIteractionButton(JobCandidate entity) {
        Button listIteractionButton = uiComponents.create(Button.class);
        listIteractionButton.setCaption("Список взаимодействий");

        listIteractionButton.setAction(new BaseAction("listIteraction")
                .withHandler(actionPerformedEvent -> {
                            screenBuilders.lookup(IteractionList.class, this)
                                    .withScreenClass(IteractionListSimpleBrowse.class)
                                    .withLaunchMode(OpenMode.DIALOG)
                                    .build()
                                    .show();

                        }
                ));

        return listIteractionButton;

    }

    private Component createNewIteractionButton(JobCandidate entity) {
        Button newIteractionButton = uiComponents.create(Button.class);
        newIteractionButton.setCaption("Новое взаимодействие");


        newIteractionButton.setAction(new BaseAction("newIteraction")
                .withHandler(actionPerformedEvent ->
                        screenBuilders.editor(IteractionList.class, this)
                                .newEntity()
                                .withScreenClass(IteractionListEdit.class)
                                .withInitializer(iteractionList1 -> {
                                    iteractionList1.setCandidate(jobCandidatesTable.getSingleSelected());
                                })
                                .build()
                                .show())
        );

        return newIteractionButton;
    }

    private HBoxLayout getLastIteraction(List<IteractionList> iteractionList) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.NAME);
        Label dateIteraction = uiComponents.create(Label.NAME);
        Label retLab = uiComponents.create(Label.NAME);
        Label add = uiComponents.create((Label.NAME));
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
        Label titleStatistics = uiComponents.create(Label.NAME);
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
                    .view("iteractionList-view")
                    .parameter("iteractionName", iteractionName)
                    .parameter("candidate", jobCandidatesTable.getSingleSelected())
                    .one();
        } catch (Exception e) {
        }


        VBoxLayout vBoxLayout = uiComponents.create(VBoxLayout.NAME);
        vBoxLayout.setWidth("100%");

        if (iteractionList != null) {
            Label lastSalary = uiComponents.create(Label.NAME);
            lastSalary.setValue("Зарплатные ожидания: " + iteractionList.getAddString());
            vBoxLayout.add(lastSalary);
        }

        statistics.add(titleStatistics);
        statistics.add(vBoxLayout);
    }

    private String getLastProject(List<IteractionList> iteractionList) {
        String retStr = "";

        for (IteractionList a : iteractionList) {
            if (a.getProject() != null)
                retStr = a.getProject().getProjectName();
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
                .parameter("candidate", singleSelected)
                .view("iteractionList-job-candidate")
                .list();

        return listIteracion;

    }

    private Component createEditButton(JobCandidate entity) {
        Button editButton = uiComponents.create(Button.class);
        editButton.setCaption("Редактирование");
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

    private Label setContacts(String labelName) {
        Label label = uiComponents.create(Label.NAME);

        if (labelName != null) {
            label.setValue(labelName);
        }

        return label;
    }


    private Component createCloseButton(JobCandidate entity) {
        Button closeButton = uiComponents.create(Button.class);
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
                        jobCandidate.getPersonPosition() == null ||
                        jobCandidate.getPositionCountry() == null) {
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
    }
}