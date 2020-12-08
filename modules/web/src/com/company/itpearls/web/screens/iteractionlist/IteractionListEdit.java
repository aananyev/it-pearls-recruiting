package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.service.SubscribeDateService;
import com.company.itpearls.web.screens.recrutiestasks.RecrutiesTasksEdit;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.actions.picker.LookupAction;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;

@UiController("itpearls_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEdit extends StandardEditor<IteractionList> {
    protected Boolean noSubscribe = false;

    @Inject
    private UserSession userSession;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Button buttonCallAction;
    @Inject
    private LookupPickerField<Iteraction> iteractionTypeField;
    @Inject
    private DataManager dataManager;
    @Inject
    private Dialogs dialogs;
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;
    private Boolean newProject;
    static Boolean myClient;
    private Boolean transferFlag = false;
    private IteractionList oldIteraction = null;
    private JobCandidate candidate = null;
    private Boolean askFlag = false;
    private Boolean askFlag2 = false;

    @Inject
    private CollectionLoader<Iteraction> iteractionTypesLc;
    @Inject
    private DateField<Date> addDate;
    @Inject
    private TextField<String> addString;
    @Inject
    private TextField<Integer> addInteger;
    @Inject
    private LookupPickerField<OpenPosition> vacancyFiels;
    @Named("candidateField.lookup")
    private LookupAction candidateFieldLookup;
    @Inject
    private LookupPickerField<User> recrutierField;
    @Inject
    private LookupPickerField<JobCandidate> candidateField;
    @Inject
    private EmailService emailService;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Metadata metadata;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private Events events;
    @Inject
    private TextArea<String> commentField;
    @Inject
    private DateField<Date> dateIteractionField;
    @Inject
    private SubscribeDateService subscribeDateService;
    @Inject
    private TextField<BigDecimal> numberIteractionField;
    @Inject
    private Label<String> companyLabel;
    @Inject
    private InstanceLoader<IteractionList> iteractionListDl;

    static String RESEARCHER = "Researcher";
    static String RECRUITER = "Recruiter";
    static String MANAGER = "Manager";
    static String ADMINISTRATOR = "Administrators";
    @Inject
    private Notifications notifications;

    @Subscribe(id = "iteractionListDc", target = Target.DATA_CONTAINER)
    private void onIteractionListDcItemChange(InstanceContainer.ItemChangeEvent<IteractionList> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            setIteractionNumber();
            setCurrentDate();
            setCurrentUserName(); // имя пользователя в нередактируемое поле
        }
    }

    @Subscribe("candidateField")
    public void onCandidateFieldValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
        // запомним кандидата
        candidate = candidateField.getValue();
        // предложить копирование если до этого было взаимодействие и узнать чей это был кандидат
        copyAndCheckCandidate();
    }

    private void changeField() {
        Integer _addType;
        Boolean _addFlag;

        try {
            _addType = iteractionTypeField.getValue().getAddType();
        } catch (NullPointerException e) {
            _addType = 0;
        }

        try {
            _addFlag = iteractionTypeField.getValue().getAddFlag();
        } catch (NullPointerException e) {
            _addFlag = false;
        }

        if (_addFlag != null) {
            if (_addFlag) {
                if (_addType != 0) {
                    switch (_addType) {
                        case 1:
                            addDate.setVisible(true);
                            addDate.setRequired(true);
                            addDate.setCaption(iteractionTypeField.getValue().getAddCaption());
                            addDate.setRequired(true);

                            addString.setVisible(false);
                            addInteger.setVisible(false);
                            buttonCallAction.setVisible(false);
                            break;
                        case 2:
                            addDate.setVisible(false);
                            addString.setVisible(true);
                            addString.setRequired(true);
                            addString.setCaption(iteractionTypeField.getValue().getAddCaption());
                            addString.setRequired(true);

                            addInteger.setVisible(false);
                            buttonCallAction.setVisible(false);
                            break;
                        case 3:
                            addDate.setVisible(false);
                            addString.setVisible(false);
                            addInteger.setVisible(true);

                            try {
                                addInteger.setCaption(iteractionTypeField.getValue().getAddCaption());
                            } catch (NullPointerException e) {
                                addInteger.setCaption(iteractionTypeField.getValue().getIterationName());
                            }
                            addInteger.setRequired(true);

                            addInteger.setRequired(true);
                            buttonCallAction.setVisible(false);
                            break;
                        default:
                            addDate.setVisible(false);
                            addString.setVisible(false);
                            addInteger.setVisible(false);
                            addDate.setVisible(false);

                            addDate.setRequired(false);
                            addInteger.setRequired(false);
                            addString.setRequired(false);

                            addDate.setCaption("");
                            addInteger.setCaption("");
                            addString.setCaption("");
                            break;
                    }
                }
            } else {
                Boolean callForm;

                try {
                    callForm = iteractionTypeField.getValue().getCallForm();
                } catch (NullPointerException e) {
                    callForm = false;
                }

                if (callForm) {
                    addDate.setVisible(false);
                    addString.setVisible(false);
                    addInteger.setVisible(false);
                    buttonCallAction.setVisible(true);
                } else {
                    addDate.setVisible(false);
                    addString.setVisible(false);
                    addInteger.setVisible(false);
                    buttonCallAction.setVisible(false);
                }
            }
        }
    }

    @Subscribe("vacancyFiels")
    public void onVacancyFielsValueChange(HasValue.ValueChangeEvent<OpenPosition> event) {

        if (vacancyFiels.getValue() != null)
            if (vacancyFiels.getValue().getProjectName() != null)
                if (vacancyFiels.getValue().getProjectName().getProjectDepartment() != null)
                    if (vacancyFiels.getValue().getProjectName().getProjectDepartment().getCompanyName() != null)
                        if (vacancyFiels.getValue().getProjectName().getProjectDepartment().getCompanyName().getCompanyShortName() != null) {
                            String labetText = "<h2><b>" +
                                    vacancyFiels.getValue()
                                            .getProjectName()
                                            .getProjectDepartment()
                                            .getCompanyName()
                                            .getCompanyShortName() +
                                    "</b>: " +
                                    vacancyFiels.getValue()
                                            .getProjectName()
                                            .getProjectDepartment()
                                            .getDepartamentRuName() +
                                    "</h2>";

                            companyLabel.setValue(labetText);
                        }

        if (!isClosedVacancy()) {
            BigDecimal a = new BigDecimal("0.0");

            // проверка на наличие записей по этой вакансии
            BigDecimal countIteraction = dataManager.loadValue("select count(e.numberIteraction) " +
                    "from itpearls_IteractionList e " +
                    "where e.candidate = :candidate and " +
                    "e.vacancy = :vacancy", BigDecimal.class)
                    .parameter("candidate", getEditedEntity().getCandidate())
                    .parameter("vacancy", getEditedEntity().getVacancy())
                    .one();

            // есть
            // взаимодействия с кандидатом по этой позиции
            if (countIteraction.compareTo(a) != 0)
                newProject = false;
            else
                // новое взаимодействие с кандидатом
                newProject = true;

            if (newProject) {
                // это начало цепочки - обрезаем выбор
                iteractionTypesLc.setParameter("number", "001");

                dialogs.createMessageDialog()
                        .withCaption("ВНИМАНИЕ!")
                        .withMessage("С кандидатом начат новый процесс. " +
                                "Начните взаимодействие с ним с типом из группы \"001 Ресерчинг\"")
                        .withModal(true)
                        .show();
            } else {
                iteractionTypesLc.removeParameter("number");
            }

            iteractionTypesLc.load();
        } else {
            if (!askFlag2) {
                askFlag2 = true;

                if (PersistenceHelper.isNew(getEditedEntity())) {
                    dialogs.createOptionDialog()
                            .withCaption("WARNING")
                            .withMessage("Вы пытаетесь зарегистрировать взаимодействие по закрытой позиции.\n" +
                                    "Отменить действие?")
                            .withActions(new DialogAction(DialogAction.Type.YES,
                                            Action.Status.PRIMARY).withHandler(e -> {
                                        this.vacancyFiels.setValue(null);
                                        vacancyFiels.focus();

                                    }),
                                    new DialogAction(DialogAction.Type.NO))
                            .show();
                }
            }
        }
        // проверить - подписан ли ресерчер на это позицию
        if (getRoleService.isUserRoles(userSession.getUser(), "Researcher"))
            isUnsubscribedPosition(getEditedEntity().getVacancy());
    }

    private void isUnsubscribedPosition(OpenPosition op) {
        if (!askFlag) {
            // ну типа уже спрашивали
            askFlag = true;

            Integer a = dataManager
                    .loadValue("select count(e.reacrutier) from itpearls_RecrutiesTasks e " +
                            "where e.reacrutier = :recrutier and " +
                            "e.openPosition = :openPosition and " +
                            ":nowDate between e.startDate and e.endDate", Integer.class)
                    .parameter("recrutier", userSession.getUser())
                    .parameter("openPosition", op)
                    .parameter("nowDate", new Date())
                    .one();

            if (a == 0) {
                if (vacancyFiels.getValue() != null) {

                    dialogs.createOptionDialog()
                            .withCaption("ВНИМАНИЕ !")
                            .withMessage("Вы не подписаны на вакансию " + op.getVacansyName() +
                                    ".\nПодписаться до будущео понедельника?")
                            .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                        screenBuilders.editor(RecrutiesTasks.class, this)
                                                .newEntity()
                                                .withScreenClass(RecrutiesTasksEdit.class)
                                                .withLaunchMode(OpenMode.DIALOG)
                                                .withInitializer(data -> {
                                                    data.setReacrutier(userSession.getUser());
                                                    data.setOpenPosition(op);
                                                    data.setStartDate(new Date());
                                                    data.setEndDate(subscribeDateService.dateOfNextMonday());
                                                })
                                                .build()
                                                .show();
                                    }),
                                    new DialogAction(DialogAction.Type.NO))
                            .show();
                }
            }
        }
    }

    private Boolean isClosedVacancy() {
        Boolean r = true;
        Boolean oc;

        if (vacancyFiels.getValue() != null) {
            // if( !PersistenceHelper.isDetached( getEditedEntity().getVacancy() ) )
            r = getEditedEntity().getVacancy().getOpenClose();
        } else
            r = false;

        return r == null ? false : r;
    }

    private BigDecimal getCountIteraction() {
        IteractionList e = dataManager.load(IteractionList.class)
                .query("select e from itpearls_IteractionList e where e.numberIteraction = " +
                        "(select max(f.numberIteraction) from itpearls_IteractionList f)")
                .view("iteractionList-view")
                .cacheable(true)
                .one();

        return e.getNumberIteraction().add(BigDecimal.ONE);
    }

    protected void setIteractionNumber() {
        numberIteractionField.setValue(getCountIteraction());
    }

    protected void setCurrentDate() {
        dateIteractionField.setValue(new Date());
    }

    // не могу получить имя пользователя и записать его в базу
    protected String getCurrentUser() {
        String currentUser =
                userSessionSource.getUserSession().getUser().getName();
        return currentUser;
    }

    void copyAndCheckCandidate() {
        if (yourCandidate()) {
            if (PersistenceHelper.isNew(getEditedEntity())) {
                // сколько записей есть по этому кандидату
                if (getIteractionCount() != 0) {
                    // ввели кандидата - предложи скопировать предыдущую запись
                    OpenPosition op = getEditedEntity().getVacancy();
                    if (op == null) {
                        dialogs.createOptionDialog()
                                .withCaption("Warning")
                                .withMessage("Скопировать предыдущую запись кандидата?")
                                .withActions(
                                        new DialogAction(DialogAction.Type.YES,
                                                Action.Status.PRIMARY).withHandler(e -> {
                                            this.copyPrevionsItems();
                                        }),
                                        new DialogAction(DialogAction.Type.NO)
                                )
                                .show();
                    }
                }
            }
        } else {
            if (PersistenceHelper.isNew(getEditedEntity())) {
                String msg = "С этим кандидатом " + oldIteraction.getRecrutier().getName() + " контактировал " +
                        oldIteraction.getDateIteraction().toString() + " МЕНЕЕ МЕСЯЦА НАЗАД!";

                dialogs.createOptionDialog()
                        .withCaption("Warning")
                        .withMessage(msg + "\nПродолжить?")
                        .withActions(
                                new DialogAction(DialogAction.Type.YES,
                                        Action.Status.PRIMARY).withHandler(y -> {
                                    this.copyPrevionsItems();
                                }),
                                new DialogAction(DialogAction.Type.NO)
                        )
                        .show();
            }
        }
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if (iteractionTypeField.getValue().getNumber() != null) {
            String s = iteractionTypeField.getValue().getNumber();
            Integer i = Integer.parseInt(s);

            candidateField.getValue().setStatus(i);
        }

        sendMessages();
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (commentField.getValue() == null)
            commentField.setValue("");

        setSubscribe();
    }

    private void setSubscribe() {
        // подписать меня на все варианты позиций
    }

    private void sendMessages() {
        LoadContext<SubscribeCandidateAction> loadContext = LoadContext.create(SubscribeCandidateAction.class)
                .setQuery(LoadContext.createQuery("select e from itpearls_SubscribeCandidateAction e " +
                        "where e.candidate = :candidate and " +
                        "e.subscriber = :subscriber and " +
                        ":curDate between e.startDate and e.endDate")
                        .setCacheable(true)
                        .setParameter("candidate", candidateField.getValue())
                        .setParameter("subscriber", userSession.getUser())
                        .setParameter("curDate", new Date()))
                .setView("subscribeCandidateAction-view");

        if (dataManager.getCount(loadContext) != 0) {
            EmailInfo emailInfo = new EmailInfo(userSession.getUser().getEmail(),
                    candidateField.getValue().getFullName() + " : " +
                            iteractionTypeField.getValue().getIterationName(), null,
                    "com/company/itpearls/templates/iteraction.html",
                    Collections.singletonMap("IteractionList", getEditedEntity()));

            emailInfo.setBodyContentType("text/html; charset=UTF-8");

            emailService.sendEmailAsync(emailInfo);
        }
        // высплывающее сообщение
        if (iteractionTypeField.getValue().getNotificationType() != null) {
            switch (iteractionTypeField.getValue().getNotificationType()) {
                case 0: // ???
                    break;
                case 1: // Нет
                    break;
                case 2: // Только менеджеру"
                    break;
                case 3: // Подписчику вакансии
                    break;
                case 4: // Подписчику кандидата
                    break;
                case 5: // Определенным адресам (список)
                    break;
                case 6: // всем
                    events.publish(new UiNotificationEvent(this,
                            "<img src=\"VAADIN/themes/halo/" + iteractionTypeField.getValue().getPic() +
                                    "\"> <b>" +
                                    getEditedEntity().getCandidate().getFullName() + " : " +
                                    getEditedEntity().getIteractionType().getIterationName() + "</b>"));
                    break;
                default:
                    break;
            }
        }
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setGregorianChange(getEditedEntity().getDateIteraction());
        gregorianCalendar.add(Calendar.HOUR, 1);

        if (getEditedEntity().getEndDateIteraction() == null) {
            if (getEditedEntity().getDateIteraction() != null) {
                getEditedEntity().setEndDateIteraction(gregorianCalendar.getTime());
            }
        }

    }

    private boolean yourCandidate() {
        // твой ли это кандидат?
        User user = userSession.getUser();

        myClient = false;
        BigDecimal numberIteraction;
        // проверка прошлого взаимодействия
        try {
            numberIteraction = dataManager.loadValue("select e.numberIteraction " +
                    "from itpearls_IteractionList e " +
                    "where e.candidate = :candidate and " +
                    "e.numberIteraction = " +
                    "(select max(f.numberIteraction) " +
                    "from itpearls_IteractionList f " +
                    "where f.candidate = :candidate)", BigDecimal.class)
                    .parameter("candidate", candidateField.getValue())
                    .one();
        } catch (IllegalStateException e) {
            // не было взаимодействий с кандидатом
            numberIteraction = null;

            myClient = true;
        }

        if (numberIteraction != null) {
            // больше месяца назад?
            try {
                oldIteraction = dataManager.load(IteractionList.class).query("select e " +
                        "from itpearls_IteractionList e " +
                        "where e.numberIteraction = :number")
                        .parameter("number", numberIteraction)
                        .view("iteractionList-view")
                        .one();
            } catch (IllegalStateException e) {
                myClient = false;
            }

            if (oldIteraction.getDateIteraction().before(new Date(System.currentTimeMillis() - 2628000000l))) {
                // все зашибись и кандидат свободен
                myClient = true;
            } else {
                if (oldIteraction.getRecrutier().equals(userSession.getUser())) {
                    myClient = true;
                } else {
                    myClient = false;
                }
            }
        }

        return myClient;
    }

    private Integer getIteractionCount() {
        Integer d;

        try {
            d = dataManager.loadValue(
                    "select count(e) " +
                            "from itpearls_IteractionList e " +
                            "where e.candidate = :candidate",
                    Integer.class)
                    .parameter("candidate", candidateField.getValue())
                    .one();
        } catch (NullPointerException | IllegalStateException e) {
            d = 0;
        }

        return d;
    }

    private void copyPrevionsItems() {
        // а вдруг позиция уже закрыта? надо разрешить в вакансию писать даже закрытые
        openPositionsDl.setQuery("select e from itpearls_OpenPosition e " +
                "order by e.vacansyName");
        openPositionsDl.load();
        // вакансия
        // а вдруг в результате экспорта не были заполнены поля
        IteractionList duplicateIteraction = null;
        try {
            duplicateIteraction = dataManager.load(IteractionList.class)
                    .query("select e " +
                            "from itpearls_IteractionList e " +
                            "where e.candidate = :candidate and " +
                            "e.numberIteraction = " +
                            "(select max(f.numberIteraction) " +
                            "from itpearls_IteractionList f " +
                            "where f.candidate = :candidate)")
                    .parameter("candidate", getEditedEntity().getCandidate())
                    .view("iteractionList-view")
                    .cacheable(true)
                    .one();
        } catch (IllegalStateException e) {
            duplicateIteraction = null;
        }
        // а вдруг пустое поле?
        if (duplicateIteraction.getVacancy() != null) {
            vacancyFiels.setValue(duplicateIteraction.getVacancy());
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        buttonCallAction.setVisible(false);
        // запомнить текущий проект
        candidate = candidateField.getValue();

        changeField();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            recrutierField.setValue(userSession.getUser());
        }
    }

    protected void setCurrentUserName() {
        String currentUser = getCurrentUser();
    }

    // изменение надписи на кнопке в зависимости от щначения поля ItercationType
    @Subscribe("iteractionTypeField")
    public void onIteractionTypeFieldValueChange(HasValue.ValueChangeEvent<Iteraction> event) {
        changeField();
        // надпись на кнопке
        if (iteractionTypeField.getValue().getCallButtonText() != null)
            buttonCallAction.setCaption(iteractionTypeField.getValue().getCallButtonText());
        // если установлен тип взаиподейтвия и нужно действие
        if (iteractionTypeField.getValue() != null)
            if (iteractionTypeField.getValue().getCallForm() != null)
                if (iteractionTypeField.getValue().getCallForm())
                    buttonCallAction.setVisible(true);
                else
                    buttonCallAction.setVisible(false);

        if (iteractionTypeField.getValue() != null) {
            if (iteractionTypeField.getValue().getSetDateTime() != null) {
                if (iteractionTypeField.getValue().getSetDateTime()) {
                    if (addDate.getValue() == null) {
                        Date date = new Date();
                        addDate.setValue(date);
                    }
                }
            }
        }
    }

    @Subscribe("vacancyFiels")
    public void onVacancyFielsValueChange2(HasValue.ValueChangeEvent<OpenPosition> event) {
        ifDiscrepancyOfVacansy(event);
    }

    private void ifDiscrepancyOfVacansy(HasValue.ValueChangeEvent<OpenPosition> event) {
        String candidatePosition = null;
        String vacansyPosition = null;
        String dialogStartMessage = "ВНИМАНИЕ! В вакансии заявлена позиция:\n";
        String dialogEndMessage = "\nВы хотите выбрать другую вакансию?";
        String dialogMessage = "";

        try {
            candidatePosition = candidateField
                    .getValue()
                    .getPersonPosition()
                    .getPositionRuName();
        } catch (Exception e) {

        }

        try {
            vacansyPosition = vacancyFiels
                    .getValue()
                    .getPositionType()
                    .getPositionRuName();
        } catch (Exception e) {

        }

        if (vacansyPosition != null || candidatePosition != null) {
            if (!candidatePosition.equals(vacansyPosition) && vacancyFiels.getValue() != null) {
                dialogMessage =
                        "\n- позиция <b><i>"
                                + vacansyPosition
                                + "</i></b>, а кандидат в настоящее время занимает позицию <b><i>"
                                + candidatePosition + "</i></b>";
            }
        }

        String vacansyCity = null;
        String candidateCity = null;
        int remoteWork = 0;

        try {
            vacansyCity = vacancyFiels.getValue().getCityPosition().getCityRuName();
        } catch (Exception e) {
        }

        try {
            candidateCity = candidateField.getValue().getCityOfResidence().getCityRuName();
        } catch (Exception e) {
        }

        try {
            remoteWork = vacancyFiels.getValue().getRemoteWork();
        } catch (Exception e) {
        }

        if (vacansyCity != null && candidateCity != null) {
            if (!vacansyCity.equals(candidateCity) && remoteWork == 0) {
                dialogMessage = dialogMessage
                        + "\n- локация <b><i>"
                        + vacansyCity
                        + "</i></b>, а кандидат находится в настоящее время кандидат находится в городе <b><i>"
                        + candidateCity + "</i></b>";
            }
        }

        if (!dialogMessage.equals("")) {
            dialogs.createOptionDialog()
                    .withType(Dialogs.MessageType.WARNING)
                    .withMessage(dialogStartMessage + dialogMessage + dialogEndMessage)
                    .withActions(new DialogAction(DialogAction.Type.YES,
                                    Action.Status.PRIMARY).withHandler(e -> {
                                vacancyFiels.setValue(null);
                            }),
                            new DialogAction(DialogAction.Type.NO))
                    .show();
        }
    }

    @Subscribe
    public void onInit(InitEvent event) {
        // изначально предполагаем, что это продолжение проекта
        newProject = false;
        // вся сортировка в поле IteractionType
        iteractionTypesLc.removeParameter("number");

        addDate.setVisible(false);
        addString.setVisible(false);
        addInteger.setVisible(false);
        addDate.setVisible(false);

        askFlag = false;
        askFlag2 = false;

        getScreenOptionsNoSubscribers(event);
    }

    // получить параметны экрана
    private void getScreenOptionsNoSubscribers(InitEvent event) {
        ScreenOptions options = event.getOptions();

        if (options instanceof IteracionListScreenOptions) {
            noSubscribe = ((IteracionListScreenOptions) options).getNoSubscribers();
        }
    }

    public void callActionEntity() {

        String calledClass = iteractionTypeField.getValue().getCallClass();
        // еслп установлено разрешение в Iteraction показать кнопку и установить на ней надпсит
        if (iteractionTypeField.getValue() != null)
            if (iteractionTypeField.getValue().getCallForm() != null)
                if (!iteractionTypeField.getValue().getCallForm()) {
                } else {
                    // Это лишнее - тут надо вызов формы сиска резюме или других документов
                    if (iteractionTypeField.getValue().getFindToDic()) {
                        Screen a = screenBuilders.editor(metadata.getClassNN(calledClass).getJavaClass(), this)
                                .newEntity()
                                .withScreenId(calledClass + ".edit")
                                .withLaunchMode(OpenMode.NEW_TAB)
                                .withInitializer(e -> {
                                })
                                .build();
                    } else {
                        Screen a = screenBuilders.editor(metadata.getClassNN(calledClass).getJavaClass(), this)
                                .newEntity()
                                .withScreenId(calledClass + ".edit")
                                .withLaunchMode(OpenMode.NEW_TAB)
                                .withInitializer(e -> {
                                    // e.setValue( "exchange", exchangeBean );
                                })
                                .build();

                        a.show();
                    }
                }
    }

    @Subscribe("vacancyFiels")
    public void onVacancyFielsValueChange1(HasValue.ValueChangeEvent<OpenPosition> event) {
        if (vacancyFiels.getValue() != null)
            if (vacancyFiels.getValue().getProjectName() != null) {
                getEditedEntity().setProject(vacancyFiels.getValue().getProjectName());

                notifications.create(Notifications.NotificationType.TRAY)
                        .withContentMode(ContentMode.TEXT)
                        .withCaption("Проект")
                        .withDescription(vacancyFiels.getValue().getProjectName().getProjectName())
                        .withHideDelayMs(5000)
                        .show();
            }

    }

    public void addNewIteraction() {
        String classIL = "itpearls_" + getEditedEntity().getClass().getSimpleName() + ".edit";

        closeWithCommit();

        screenBuilders.editor(IteractionList.class, this)
                .newEntity()
                .withScreenClass(IteractionListEdit.class)
                .withLaunchMode(OpenMode.NEW_TAB)
                .withInitializer(e -> {
                    e.setCandidate(this.getEditedEntity().getCandidate());
                    e.setVacancy(this.getEditedEntity().getVacancy());
//                    e.setCompanyDepartment(this.getEditedEntity().getCompanyDepartment());
                })
                .build()
                .show();

        iteractionListDl.load();
    }

    public void setTransferFlag(Boolean flag) {
        transferFlag = flag;
    }

    public Boolean getTransferFlag() {
        return transferFlag;
    }

    public void onButtonSubscribeClick() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            dialogs.createOptionDialog()
                    .withCaption("WARNING!")
                    .withMessage("Сохранить изменения?")
                    .withActions(new DialogAction(DialogAction.Type.YES,
                                    Action.Status.PRIMARY).withHandler(e -> {
                                commitChanges();

                                screenBuilders.editor(SubscribeCandidateAction.class, this)
                                        .newEntity()
                                        .withInitializer(k -> {
                                            k.setCandidate(candidateField.getValue());
                                            k.setSubscriber(userSession.getUser());
                                            k.setStartDate(new Date());
                                        })
                                        .withOpenMode(OpenMode.DIALOG)
                                        .build()
                                        .show();

                            }),
                            new DialogAction(DialogAction.Type.NO))
                    .show();

        } else {
            screenBuilders.editor(SubscribeCandidateAction.class, this)
                    .newEntity()
                    .withInitializer(e -> {
                        e.setCandidate(candidateField.getValue());
                        e.setSubscriber(userSession.getUser());
                        e.setStartDate(new Date());
                    })
                    .withOpenMode(OpenMode.DIALOG)
                    .build()
                    .show();
        }
    }
}
