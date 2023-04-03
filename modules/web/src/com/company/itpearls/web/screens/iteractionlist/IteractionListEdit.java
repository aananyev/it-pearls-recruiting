package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.core.EmailGenerationService;
import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.service.SubscribeDateService;
import com.company.itpearls.web.StandartPriorityVacancy;
import com.company.itpearls.web.screens.internalemailer.InternalEmailerEdit;
import com.company.itpearls.web.screens.recrutiestasks.RecrutiesTasksEdit;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

@UiController("itpearls_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEdit extends StandardEditor<IteractionList> {

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
    @Inject
    private LookupPickerField<User> recrutierField;
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
    @Inject
    private Notifications notifications;
    @Inject
    private LookupField ratingField;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Label<String> ratingLabel;
    @Inject
    private StarsAndOtherService starsAndOtherService;

    private Boolean newProject;
    static Boolean myClient;
    private Boolean transferFlag = false;
    private IteractionList oldIteraction = null;
    private JobCandidate candidate = null;
    private Boolean askFlag = false;
    private Boolean askFlag2 = false;
    protected Boolean noSubscribe = false;
    private List<Iteraction> mostPopular = new ArrayList<>();
    private Boolean afterCommitSign = true;

    @Inject
    private EmailGenerationService emailGenerationService;
    @Inject
    private Logger log;
    @Inject
    private HBoxLayout mostPopularHbox;
    @Inject
    private Label<String> currentPriorityLabel;

    private Map<String, Integer> priorityMap = new LinkedHashMap<>();
    @Inject
    private Image trafficLighterImage;
    @Inject
    private Label<String> statusOfVacansyLabel;
    @Inject
    private Resources resources;
    @Inject
    private Label<String> projectLabel;
    @Inject
    private Image ratingImage;
    @Inject
    private HBoxLayout outstaffingCostHBox;
    @Inject
    private SuggestionPickerField<JobCandidate> candidateField;
    @Inject
    private LinkButton alternativeVacancyLinkButton;

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
    }

    @Subscribe
    public void onAfterShow2(AfterShowEvent event) {
        candidateField.addValueChangeListener(e -> {
            copyAndCheckCandidate();
        });
    }

    private void changeField() {
        Integer _addType = 0;
        Boolean _addFlag = false;

        try {
            if (iteractionTypeField.getValue() != null)
                if (iteractionTypeField.getValue().getAddType() != null)
                    _addType = iteractionTypeField.getValue().getAddType();
        } catch (NullPointerException e) {
            log.error("Error", e);
            _addType = 0;
        }

        try {
            if (iteractionTypeField.getValue() != null)
                if (iteractionTypeField.getValue().getAddFlag() != null)
                    _addFlag = iteractionTypeField.getValue().getAddFlag();
        } catch (NullPointerException e) {
            log.error("Error", e);
            _addFlag = false;
        }

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
            Boolean callForm = false;

            try {
                if (iteractionTypeField.getValue() != null) {
                    callForm = iteractionTypeField.getValue().getCallForm();
                }
            } catch (NullPointerException e) {
                callForm = false;
                log.error("Error", e);
            }

            if (callForm != null) {
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
            } else {
                addDate.setVisible(false);
                addString.setVisible(false);
                addInteger.setVisible(false);
                buttonCallAction.setVisible(false);
            }
        }
    }

    private void vacancyFieldValueChange(HasValue.ValueChangeEvent<OpenPosition> event) {
        ifDiscrepancyOfVacansy(event);

        if (vacancyFiels.getValue() != null) {
            if (vacancyFiels.getValue().getProjectName() != null) {
                // getEditedEntity().setProject(vacancyFiels.getValue().getProjectName());
            }
        }

        if (vacancyFiels.getValue() != null)
            if (vacancyFiels.getValue().getProjectName() != null)
                if (vacancyFiels.getValue().getProjectName().getProjectDepartment() != null)
                    if (vacancyFiels.getValue().getProjectName().getProjectDepartment().getCompanyName() != null)
                        if (vacancyFiels.getValue().getProjectName().getProjectDepartment().getCompanyName().getCompanyShortName() != null) {
                            String labetText = "<h3><b>" +
                                    vacancyFiels.getValue()
                                            .getProjectName()
                                            .getProjectDepartment()
                                            .getCompanyName()
                                            .getCompanyShortName() +
                                    "</b>/ " +
                                    vacancyFiels.getValue()
                                            .getProjectName()
                                            .getProjectDepartment()
                                            .getDepartamentRuName() +
                                    "</h3>";

                            companyLabel.setValue(labetText);

                            projectLabel.setValue(event.getValue().getProjectName().getProjectName());
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
            if (countIteraction.compareTo(a) != 0) {
                newProject = false;
            } else {
                // новое взаимодействие с кандидатом
                newProject = true;
            }

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

                                        ratingImage.setValueSource(null);
                                        trafficLighterImage.setValueSource(null);
                                        currentPriorityLabel.setValue(null);

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
                                    ".\nПодписаться до будущего понедельника?")
                            .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                        screenBuilders.editor(RecrutiesTasks.class, this)
                                                .newEntity()
                                                .withScreenClass(RecrutiesTasksEdit.class)
                                                .withLaunchMode(OpenMode.DIALOG)
                                                .withInitializer(data -> {
                                                    data.setReacrutier((ExtUser) userSession.getUser());
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

                notifications.create()
                        .withType(Notifications.NotificationType.HUMANIZED)
                        .withCaption("ВНИМАНИЕ!!!")
                        .withDescription(msg)
                        .withPosition(Notifications.Position.MIDDLE_CENTER)
                        .show();
            }
        }
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if (afterCommitSign) {
            if (iteractionTypeField.getValue() != null) {
                if (iteractionTypeField.getValue().getNumber() != null) {
                    String s = iteractionTypeField.getValue().getNumber();

                    Integer i = Integer.parseInt(s.contains(".") ? s.substring(0, s.indexOf(".")) : s);

                    if (candidateField.getValue() != null) {
                        candidateField.getValue().setStatus(i);
                    }
                }
            }

            if (!afterCommitSendMessage) {
                sendMessages();
            }

            if (!afterCommitEmailToCandidateSended) {
                sendMessagesToCandidate();
            }

            afterCommitSign = false;
        }
    }

    Boolean afterCommitEmailToCandidateSended = false;
    Boolean afterCommitSendMessage = false;

    private void sendMessagesToCandidate() {
        if (getEditedEntity().getIteractionType() != null) {
            if (getEditedEntity().getIteractionType().getNeedSendLetter() != null) {
                if (getEditedEntity().getIteractionType().getNeedSendLetter()) {
                    if (getEditedEntity().getIteractionType().getTextEmailToSend() != null) {
                        if (getEditedEntity().getCandidate().getEmail() != null) {

                            String message = preparingMessage(getEditedEntity());

                            dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                                    .withContentMode(ContentMode.HTML)
                                    .withType(Dialogs.MessageType.CONFIRMATION)
                                    .withWidth("800px")
                                    .withHeight("600px")
                                    .withMessage("<h3>Высылать ли кандидату оповещение по электронной почте?</h3><br><br><i>"
                                            + message
                                            + "</i>")
                                    .withActions(new DialogAction(DialogAction.Type.YES,
                                                    Action.Status.PRIMARY).withHandler(e -> {
                                                IteractionList newsItem = getEditedEntity();
                                                String bodyMessage = preparingMessage(newsItem);

                                                if (newsItem.getVacancy().getNeedMemoForInterview() != null) {
                                                    if (newsItem.getVacancy().getNeedMemoForInterview()) {
                                                        if (newsItem.getVacancy().getMemoForInterview() != null) {
                                                            if (newsItem.getIteractionType().getNeedSendMemo() != null) {
                                                                if (newsItem.getIteractionType().getNeedSendMemo()) {
                                                                    bodyMessage = bodyMessage
                                                                            + "<br><br><b>Памятка для собеседования</b><br><br>"
                                                                            + newsItem.getVacancy().getMemoForInterview();

                                                                    String finalBodyMessage = bodyMessage;
                                                                    screenBuilders.editor(InternalEmailerTemplate.class, this)
                                                                            .newEntity()
                                                                            .withInitializer(ev -> {
                                                                                ev.setFromEmail((ExtUser) userSession.getUser());
                                                                                ev.setToEmail(candidateField.getValue());
                                                                                ev.setBodyEmail(finalBodyMessage);
                                                                            })
                                                                            .build()
                                                                            .show();
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

/*                                                EmailInfo emailInfo = EmailInfoBuilder.create()
                                                        .setAddresses(newsItem.getCandidate().getEmail())
                                                        .setCaption("IT Pearls - "
                                                                + newsItem.getVacancy().getVacansyName()
                                                                + "/"
                                                                + newsItem.getIteractionType().getIterationName())
                                                        .setFrom(null)
                                                        .setBody(bodyMessage)
                                                        .setTemplateParameters(Collections.singletonMap("newsItem", newsItem))
                                                        .build();

                                                emailInfo.setBodyContentType("text/html; charset=UTF-8");
                                                emailService.sendEmailAsync(emailInfo); */


                                                afterCommitEmailToCandidateSended = true;
                                            }),
                                            new DialogAction(DialogAction.Type.NO))
                                    .show();
                        }
                    }
                }
            }
        }
    }

    private String preparingMessage(IteractionList newsItem) {
        HashMap<String, String> emailKeys = emailGenerationService.generateKeys();

        String retStr = newsItem.getIteractionType().getTextEmailToSend();

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
            retStr = retStr.replace(emailKeys.get(EmailKeys.DATE),
                    simpleDateFormat.format(newsItem.getAddDate()).toString());

            SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("hh:mm");
            retStr = retStr.replace(emailKeys.get(EmailKeys.TIME),
                    simpleDateFormat1.format(newsItem.getAddDate()).toString());

            if (newsItem.getCandidate().getFirstName() != null)
                retStr = retStr.replace(emailKeys.get(EmailKeys.FIRST_NAME), newsItem.getCandidate().getFirstName());
            if (newsItem.getCandidate().getMiddleName() != null)
                retStr = retStr.replace(emailKeys.get(EmailKeys.MIDDLE_NAME), newsItem.getCandidate().getMiddleName());
            if (newsItem.getCandidate().getSecondName() != null)
                retStr = retStr.replace(emailKeys.get(EmailKeys.SECOND_NAME), newsItem.getCandidate().getSecondName());
            if (newsItem.getVacancy().getVacansyName() != null)
                retStr = retStr.replace(emailKeys.get(EmailKeys.VACANCY), newsItem.getVacancy().getVacansyName());
            if (newsItem.getVacancy().getProjectName() != null)
                if (newsItem.getVacancy().getProjectName().getProjectName() != null)
                    retStr = retStr.replace(emailKeys.get(EmailKeys.PROJECT), newsItem.getVacancy().getProjectName().getProjectName());
            if (newsItem.getVacancy() != null)
                if (newsItem.getVacancy().getProjectName() != null)
                    if (newsItem.getVacancy().getProjectName().getProjectDepartment() != null)
                        if (newsItem.getVacancy().getProjectName().getProjectDepartment().getCompanyName() != null)
                            if (newsItem.getVacancy().getProjectName().getProjectDepartment().getCompanyName().getComanyName() != null)
                                retStr = retStr.replace(emailKeys.get(EmailKeys.COMPANY), newsItem.getVacancy().getProjectName().getProjectDepartment().getCompanyName().getComanyName());
            if (newsItem.getVacancy().getProjectName() != null)
                if (newsItem.getVacancy().getProjectName().getProjectName() != null)
                    retStr = retStr.replace(emailKeys.get(EmailKeys.DEPARTAMENT), newsItem.getVacancy().getProjectName().getProjectName());
            if (newsItem.getRecrutier() != null)
                if (newsItem.getRecrutier().getName() != null)
                    retStr = retStr.replace(emailKeys.get(EmailKeys.RESEARCHER_NAME), newsItem.getRecrutier().getName());
            if (newsItem.getVacancy() != null)
                if (newsItem.getVacancy().getComment() != null)
                    retStr = retStr.replace(emailKeys.get(EmailKeys.JOB_DESCRIPTION), newsItem.getVacancy().getComment());
            if (newsItem.getVacancy().getPositionType() != null)
                if (newsItem.getVacancy().getPositionType().getPositionEnName() != null)
                    retStr = retStr.replace(emailKeys.get(EmailKeys.POSITION),
                            newsItem.getVacancy().getPositionType().getPositionEnName());
            if (newsItem.getVacancy().getSalaryMin() != null)
                retStr = retStr.replace(emailKeys.get(EmailKeys.SALARY_MIN),
                        newsItem.getVacancy().getSalaryMin().toString());
            if (newsItem.getVacancy().getSalaryMax() != null)
                retStr = retStr.replace(emailKeys.get(EmailKeys.SALARY_MAX),
                        newsItem.getVacancy().getSalaryMax().toString());

        } catch (NullPointerException e) {
            log.error("Error", e);
        }

        return retStr;

    }

    Boolean deleteTwiceEvent = true;

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        beforeCommitFlag = true;

        if (commentField.getValue() == null)
            commentField.setValue("");

        getEditedEntity().setCurrentPriority(vacancyFiels.getValue().getPriority());
        getEditedEntity().setCurrentOpenClose(vacancyFiels.getValue().getOpenClose());

        setChainInteraction(event);
    }

    Boolean setChainFlag = false;

    private void setChainInteraction(BeforeCommitChangesEvent event) {
        if (!setChainFlag) {
            String QUERY_CHAIN = "select e from itpearls_IteractionList e " +
                    "where e.vacancy = :vacancy " +
                    "and e.candidate = :candidate";

            List<IteractionList> iteractionLists = dataManager.load(IteractionList.class)
                    .query(QUERY_CHAIN)
                    .parameter("vacancy", vacancyFiels.getValue())
                    .parameter("candidate", candidateField.getValue())
                    .view("iteractionList-view")
                    .list();

            if (iteractionLists.size() > 0) {
                IteractionList lastInteraction = iteractionLists.get(0);

                for (IteractionList iL : iteractionLists) {
                    if (iL.getIteractionType() != null) {
                        if (iL.getDateIteraction().after(lastInteraction.getDateIteraction())) {
                            lastInteraction = iL;
                        }
                    }
                }

                getEditedEntity().setChainInteraction(lastInteraction);
            } else {
                getEditedEntity().setChainInteraction(null);
            }

            setChainFlag = true;
        }
    }

    @Subscribe
    public void onAfterCommitChanges1(AfterCommitChangesEvent event) {
        if (deleteTwiceEvent) {
            setSubscribe();
            setOpenPositionNewsAutomatedMessage(vacancyFiels.getValue(),
                    iteractionTypeField.getValue().getIterationName(),
                    commentField.getValue(),
                    dateIteractionField.getValue(),
                    candidateField.getValue(),
                    recrutierField.getValue(),
                    iteractionTypeField.getValue().getSignPriorityNews());
            deleteTwiceEvent = false;
        }
    }

    private void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                                     String subject,
                                                     String comment,
                                                     Date date,
                                                     JobCandidate jobCandidate,
                                                     User user,
                                                     Boolean priority) {
        try {
            OpenPositionNews openPositionNews = metadata.create(OpenPositionNews.class);

            openPositionNews.setOpenPosition(editedEntity);
            openPositionNews.setAuthor(user);
            openPositionNews.setDateNews(date);
            openPositionNews.setSubject(subject);
            openPositionNews.setComment(comment);
            openPositionNews.setCandidates(jobCandidate);
            openPositionNews.setPriorityNews(priority != null ? priority : false);

            CommitContext commitContext = new CommitContext();
            commitContext.addInstanceToCommit(openPositionNews);
            dataManager.commit(commitContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSubscribe() {
        // подписать меня на все варианты позиций
    }

    private void sendMessages() {
        if (candidateField.getValue() != null) {
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
                if (candidateField.getValue() != null && iteractionTypeField.getValue() != null) {
                    EmailInfo emailInfo = new EmailInfo(userSession.getUser().getEmail(),
                            candidateField.getValue().getFullName() + " : " +
                                    iteractionTypeField.getValue().getIterationName(), null,
                            "com/company/itpearls/templates/iteraction.html",
                            Collections.singletonMap("IteractionList", getEditedEntity()));

                    emailInfo.setBodyContentType("text/html; charset=UTF-8");
                    emailService.sendEmailAsync(emailInfo);

                    afterCommitSendMessage = true;
                }
            }

            // высплывающее сообщение
            if (iteractionTypeField.getValue() != null) {
                if (iteractionTypeField.getValue().getNotificationNeedSend() != null) {
                    if (iteractionTypeField.getValue().getNotificationNeedSend()) {
                        if (iteractionTypeField.getValue().getNotificationWhenSend() == 1) { // слать на момент создания
                            if (iteractionTypeField.getValue() != null) {
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
                                                            "\"> <b>"
                                                            + getEditedEntity().getCandidate().getFullName()
                                                            + " : "
                                                            + getEditedEntity().getIteractionType().getIterationName()
                                                            + "</b><br><svg width=\"100%\" align=\"right\"><i>"
                                                            + userSession.getUser().getName()
                                                            + "</i></svg>"));
                                            afterCommitSendMessage = true;
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setGregorianChange(getEditedEntity().getDateIteraction());
        gregorianCalendar.add(Calendar.HOUR, 1);

        candidateField.addValueChangeListener(e -> {
        });

/*        if (getEditedEntity().getEndDateIteraction() == null) {
            if (getEditedEntity().getDateIteraction() != null) {
                getEditedEntity().setEndDateIteraction(gregorianCalendar.getTime());
            }
        }*/

    }

    private boolean yourCandidate() {
        // твой ли это кандидат?
        myClient = false;
        BigDecimal numberIteraction = null;
        // проверка прошлого взаимодействия
        try {
            if (candidateField.getValue() != null) {
                numberIteraction = dataManager.loadValue("select e.numberIteraction " +
                        "from itpearls_IteractionList e " +
                        "where e.candidate = :candidate and " +
                        "e.numberIteraction = " +
                        "(select max(f.numberIteraction) " +
                        "from itpearls_IteractionList f " +
                        "where f.candidate = :candidate)", BigDecimal.class)
                        .parameter("candidate", candidateField.getValue())
                        .one();
            }
        } catch (IllegalStateException e) {
            // не было взаимодействий с кандидатом
            numberIteraction = null;

            myClient = true;
            log.error("SQLError", e);
        }

        if (numberIteraction != null) {
            // больше месяца назад?
            try {
                oldIteraction = dataManager.load(IteractionList.class).query("select e " +
                        "from itpearls_IteractionList e " +
                        "where e.numberIteraction = :number")
                        .parameter("number", numberIteraction)
                        .view("iteractionList-view")
                        .cacheable(true)
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
        Integer d = null;

        try {
            if (candidateField.getValue() != null) {
                d = dataManager.loadValue(
                        "select count(e) " +
                                "from itpearls_IteractionList e " +
                                "where e.candidate = :candidate",
                        Integer.class)
                        .parameter("candidate", candidateField.getValue())
                        .one();
            }
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

        if (parentCandidate != null) {
            getEditedEntity().setCandidate(parentCandidate);
        }
        ;
        setMostPopularIteraction();

        if (PersistenceHelper.isNew(getEditedEntity())) {
            dateIteractionField.setEditable(true);
        } else {
            dateIteractionField.setEditable(false);
        }
    }

    private void setMostPopularIteraction() {
        int maxCount = 5;
        mostPopular = getMostPolularIteraction(userSession.getUser(), maxCount);

        if (maxCount > mostPopular.size())
            maxCount = mostPopular.size();

        for (int i = 0; i < maxCount; i++) {
            LinkButton mostPopularLabel = uiComponents.create(LinkButton.NAME);
            mostPopularLabel.setCaption(mostPopular.get(i).getIterationName() + " (" + i + ")");
            mostPopularLabel.setStyleName("label_button_green");
            mostPopularLabel.addClickListener(e -> {
                String mostPopNumber = e.getSource().getCaption().substring(e.getSource().getCaption().length() - 2, e.getSource().getCaption().length() - 1);

                iteractionTypeField.setValue(mostPopular.get(Integer.parseInt(mostPopNumber)));
            });

            mostPopularHbox.add(mostPopularLabel);
        }
    }

    public List<Iteraction> getMostPolularIteraction(User user, int maxCount) {
        String QUERY = "select e.iteractionType, count(e.iteractionType) "
                + "from itpearls_IteractionList e "
                + "where "
                + "(e.dateIteraction between :endDate and :startDate) and "
                + "e.iteractionType is not null and "
                + "e.recrutier = :user "
                + "group by e.iteractionType "
                + "order by count(e.iteractionType) desc";

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());

        Date startDate = gregorianCalendar.getTime();
        gregorianCalendar.add(Calendar.MONTH, -1);
        Date endDate = gregorianCalendar.getTime();

        List<KeyValueEntity> list = dataManager.loadValues(QUERY)
                .properties("iteractionType", "count")
                .parameter("user", user)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .list();

        List<Iteraction> retIteraction = new ArrayList<>();

        if (maxCount > list.size())
            maxCount = list.size();

        for (int i = 0; i < maxCount; i++) {
            retIteraction.add(list.get(i).getValue("iteractionType"));
        }

        return retIteraction;
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
        setCommentMandatory(event.getValue());

        try {
            if (!event.getValue().equals(event.getPrevValue())) {
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
        } catch (NullPointerException e) {
            log.error("Error", e);
        }
    }

    private void setCommentMandatory(Iteraction iteraction) {
        if (iteraction != null) {
            if (iteraction.getSignComment() != null) {
                if (iteraction.getSignComment()) {
                    commentField.setRequired(true);
                } else {
                    commentField.setRequired(false);
                }
            }
        }
    }

    private void ifDiscrepancyOfVacansy(HasValue.ValueChangeEvent<OpenPosition> event) {
        String candidatePosition = null;
        String vacansyPosition = null;
        String dialogStartMessage = "ВНИМАНИЕ! В вакансии заявлена позиция:<br>";
        String dialogEndMessage = "<br><br>Вы хотите выбрать другую вакансию?";
        String dialogMessage = "";

        try {
            if (candidateField.getValue() != null) {
                if (candidateField.getValue().getPersonPosition() != null) {
                    if (candidateField.getValue().getPersonPosition().getPositionRuName() != null) {
                        candidatePosition = candidateField
                                .getValue()
                                .getPersonPosition()
                                .getPositionRuName();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error", e);
        }

        try {
            if (event.getValue() != null) {
                if (event.getValue().getPositionType() != null) {
                    if (event.getValue().getPositionType().getPositionRuName() != null) {
                        vacansyPosition = event.getValue()
                                .getPositionType()
                                .getPositionRuName();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error", e);
        }

        if (vacansyPosition != null && candidatePosition != null) {
            if (vacancyFiels.getValue() != null) {
                if ((!candidatePosition.equals(vacansyPosition)) &&
                        candidatePosition != null) {
                    dialogMessage =
                            "<br>- позиция <b><i>"
                                    + vacansyPosition
                                    + "</i></b>, а кандидат в настоящее время занимает позицию <b><i>"
                                    + candidatePosition + "</i></b>";
                }
            }
        }

        String vacansyCity = null;
        String candidateCity = null;
        int remoteWork = 0;

        try {
            vacansyCity = event.getValue().getCityPosition().getCityRuName();
        } catch (Exception e) {
            log.error("Error", e);
        }

        try {
            candidateCity = candidateField.getValue().getCityOfResidence().getCityRuName();
        } catch (Exception e) {
            log.error("Error", e);
        }

        try {
            remoteWork = event.getValue().getRemoteWork();
        } catch (Exception e) {
            log.error("Error", e);
        }

        if (vacansyCity != null && candidateCity != null) {
            String cities = "";

            if (vacancyFiels.getValue().getCities() != null) {
                for (City c : event.getValue().getCities()) {
                    if (candidateCity.equals(c.getCityRuName())) {
                        cities = "";
                        break;
                    } else {
                        cities = cities + ", " + c.getCityRuName();
                    }
                }
            }

            if (!vacansyCity.equals(candidateCity) && remoteWork == 0 && !cities.equals("")) {
                dialogMessage = dialogMessage
                        + "<br>- локация <b><i>"
                        + vacansyCity
                        + (!cities.equals("") ? "" : ", " + cities)
                        + "</i></b>, а кандидат находится в настоящее время кандидат находится в городе <b><i>"
                        + candidateCity + "</i></b>";
            }
        }

        if (!dialogMessage.equals("")) {
            dialogs.createOptionDialog()
                    .withContentMode(ContentMode.HTML)
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
        setRatingField();
        setPriorityMap();

    }

    private void setRatingField() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(starsAndOtherService.setStars(1) + " Полный негатив", 0);
        map.put(starsAndOtherService.setStars(2) + " Сомнительно", 1);
        map.put(starsAndOtherService.setStars(3) + " Нейтрально", 2);
        map.put(starsAndOtherService.setStars(4) + " Положительно", 3);
        map.put(starsAndOtherService.setStars(5) + " Отлично!", 4);
        ratingField.setOptionsMap(map);

        ratingField.addValueChangeListener(e -> {
            ratingLabel.setValue(String.valueOf(ratingField.getValue() != null ?
                    ((int) ratingField.getValue()) + 1 :
                    0));
            String rating_color = "";

            if (ratingField.getValue() != null) {
                switch ((int) ratingField.getValue()) {
                    case 0:
                        rating_color = "red";
                        break;
                    case 1:
                        rating_color = "orange";
                        break;
                    case 2:
                        rating_color = "yellow";
                        break;
                    case 3:
                        rating_color = "green";
                        break;
                    case 4:
                        rating_color = "blue";
                        break;
                    default:
                        rating_color = "red";
                        break;
                }
            }

            String rating_style = "rating_" + rating_color + "_"
                    + String.valueOf(ratingField.getValue() != null ? ((int) ratingField.getValue()) + 1 : 1);

//            ratingField.setStyleName(rating_style);
            ratingLabel.setStyleName(rating_style);
        });
    }

/*    @Install(to = "candidateField", subject = "optionImageProvider")
    private Resource candidateFieldOptionImageProvider(JobCandidate jobCandidate) {
        if (jobCandidate.getFileImageFace() != null) {
            Image image = uiComponents.create(Image.NAME);
            image.setStyleName("round-photo");
            FileDescriptorResource resource = image.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(jobCandidate.getFileImageFace());
            return resource;
        }

        return null;
    } */

    @Install(to = "ratingField", subject = "optionStyleProvider")
    private String ratingFieldOptionStyleProvider(Object object) {
        int a = ratingField.getValue() != null ? (int) ratingField.getValue() : 0;
        return "rating_star_" + (a + 1);
    }

    Boolean beforeCommitFlag = false;

    @Subscribe("vacancyFiels")
    public void onVacancyFielsValueChange(HasValue.ValueChangeEvent<OpenPosition> event) {
        if (!beforeCommitFlag) {
            vacancyFieldValueChange(event);
            setPriorityLabel(event);
            setStatusOfVacancyLabel(event);
        }
    }

    private void setStatusOfVacancyLabel(HasValue.ValueChangeEvent<OpenPosition> event) {
        String QUERY
                = "select v from itpearls_OpenPosition v where not (v.openClose = true) and v.positionType = :positionType";

        if (event.getValue() != null) {
            if (event.getValue().getOpenClose()) {
                statusOfVacansyLabel.setValue("ЗАКРЫТА");
                statusOfVacansyLabel.setStyleName("h3-red");


                List<OpenPosition> alternatives = dataManager
                        .load(OpenPosition.class)
                        .query(QUERY)
                        .view("openPosition-view")
                        .parameter("positionType", event.getValue().getPositionType())
                        .list();

                if (alternatives.size() > 0) {
                    alternativeVacancyLinkButton.setVisible(true);
                    alternativeVacancyLinkButton.addStyleName("transition-red");

                    String description = "<b>Альтернативные вакансии для кандидата:</b></br><ul>";
                    for (OpenPosition openPosition : alternatives) {
                        description += "<li>" + openPosition.getVacansyName();
                    }

                    description += "</ul>";

                    alternativeVacancyLinkButton.setDescription(description);
                } else {
                    alternativeVacancyLinkButton.setVisible(false);
                }
            } else {
                statusOfVacansyLabel.setValue("ОТКРЫТА");
                statusOfVacansyLabel.setStyleName("h3-green");

                alternativeVacancyLinkButton.setVisible(false);
            }
        } else {
            statusOfVacansyLabel.setValue("");
            alternativeVacancyLinkButton.setVisible(false);
        }
    }

    private void setPriorityMap() {
        priorityMap.put(StandartPriorityVacancy.DRAFT_STR,
                StandartPriorityVacancy.DRAFT_INT);
        priorityMap.put(StandartPriorityVacancy.PAUSED_STR,
                StandartPriorityVacancy.PAUSED_INT);
        priorityMap.put(StandartPriorityVacancy.LOW_STR,
                StandartPriorityVacancy.LOW_INT);
        priorityMap.put(StandartPriorityVacancy.NORMAL_STR,
                StandartPriorityVacancy.NORMAL_INT);
        priorityMap.put(StandartPriorityVacancy.HIGH_STR,
                StandartPriorityVacancy.HIGH_INT);
        priorityMap.put(StandartPriorityVacancy.CRITICAL_STR,
                StandartPriorityVacancy.CRITICAL_INT);
    }

    private void setPriorityLabel(HasValue.ValueChangeEvent<OpenPosition> event) {
        String priorityStr = "";

        for (Map.Entry<String, Integer> pair : priorityMap.entrySet()) {
            if (event.getValue() != null && pair.getValue() != null) {
                if (event.getValue().getPriority().equals(pair.getValue())) {
                    priorityStr = pair.getKey();
                    break;
                }
            } else {
                break;
            }
        }

        String icon = "";

        if (event.getValue() != null) {
            switch (event.getValue().getPriority()) {
                case -1:
                    icon = StandartPriorityVacancy.DRAFT_ICON;
                    break;
                case 0: //"Paused"
                    icon = StandartPriorityVacancy.PAUSED_ICON;
                    break;
                case 1: //"Low"
                    icon = StandartPriorityVacancy.LOW_ICON;
                    break;
                case 2: //"Normal"
                    icon = StandartPriorityVacancy.NORMAL_ICON;
                    break;
                case 3: //"High"
                    icon = StandartPriorityVacancy.HIGH_ICON;
                    break;
                case 4: //"Critical"
                    icon = StandartPriorityVacancy.CRITICAL_ICON;
                    break;
            }
        } else {
            icon = null;
        }

        if (!priorityStr.equals("")) {
            currentPriorityLabel.setValue(priorityStr);
            trafficLighterImage.setSource(ThemeResource.class).setPath(icon);
            ratingImage.setSource(ThemeResource.class).setPath(icon);
            ;
        }
    }


    @Install(to = "vacancyFiels", subject = "optionImageProvider")
    private Resource vacancyFielsOptionImageProvider(OpenPosition openPosition) {
        String icon = "";

        switch (openPosition.getPriority()) {
            case -1:
                icon = StandartPriorityVacancy.DRAFT_ICON;
                break;
            case 0: //"Paused"
                icon = StandartPriorityVacancy.PAUSED_ICON;
                break;
            case 1: //"Low"
                icon = StandartPriorityVacancy.LOW_ICON;
                break;
            case 2: //"Normal"
                icon = StandartPriorityVacancy.NORMAL_ICON;
                break;
            case 3: //"High"
                icon = StandartPriorityVacancy.HIGH_ICON;
                break;
            case 4: //"Critical"
                icon = StandartPriorityVacancy.CRITICAL_ICON;
                break;
        }

        return (Resource) resources.getResource(icon);
    }

    @Install(to = "vacancyFiels", subject = "optionIconProvider")
    private String vacancyFielsOptionIconProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }

    @Install(to = "vacancyFiels", subject = "optionStyleProvider")
    private String vacancyFielsOptionStyleProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return "open-position-lookup-field-black";
        } else {
            return "open-position-lookup-field-gray";
        }
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
        if (iteractionTypeField.getValue() != null) {
            if (iteractionTypeField.getValue().getCallForm() != null) {
                if (!iteractionTypeField.getValue().getCallForm()) {
                } else {
                    // Это лишнее - тут надо вызов формы сиска резюме или других документов
                    if (iteractionTypeField.getValue() != null) {
                        if (iteractionTypeField.getValue().getFindToDic()) {
                            Screen a = screenBuilders.editor(metadata.getClassNN(calledClass).getJavaClass(), this)
                                    .newEntity()
                                    .withScreenId(calledClass + ".edit")
                                    .withLaunchMode(OpenMode.NEW_TAB)
                                    .withInitializer(e -> {
                                    })
                                    .build();

                            a.show();
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
            }
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

    private JobCandidate parentCandidate = null;

    public void setParentCandidate(JobCandidate candidate) {
        this.parentCandidate = candidate;
    }

    @Subscribe("vacancyFiels")
    public void onVacancyFielsValueChange2(HasValue.ValueChangeEvent<OpenPosition> event) {
        if (event.getValue() != null) {
            outstaffingCostHBox.setVisible(event.getValue().getOutstaffingCost() != null);
        }
    }

    @Install(to = "recrutierField", subject = "optionIconProvider")
    private String recrutierFieldOptionIconProvider(User user) {
        return user.getActive() ? CubaIcon.PLUS_CIRCLE.source() : CubaIcon.MINUS_CIRCLE.source();
    }

    @Subscribe("addDate")
    public void onAddDateValueChange(HasValue.ValueChangeEvent<Date> event) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy hh.mm");

        if (event.getValue() != null) {
            String retStr = commentField.getValue() != null ? commentField.getValue() : "";

            commentField.setValue((commentField.getValue() != null ? retStr
                    + "\n" : "")
                    + iteractionTypeField.getValue().getIterationName()
                    + ": "
                    + sdf.format(addDate.getValue()));
        }
    }

    @Subscribe("addString")
    public void onAddStringValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            String retStr = commentField.getValue() != null ? commentField.getValue() : "";

            commentField.setValue((commentField.getValue() != null ? retStr
                    + "\n" : "")
                    + iteractionTypeField.getValue().getIterationName()
                    + ": "
                    + event.getValue());
        }
    }

    @Subscribe("addInteger")
    public void onAddIntegerValueChange(HasValue.ValueChangeEvent<Integer> event) {
        if (event.getValue() != null) {
            String retStr = commentField.getValue() != null ? commentField.getValue() : "";

            commentField.setValue((commentField.getValue() != null ? retStr
                    + "\n" : "")
                    + iteractionTypeField.getValue().getIterationName()
                    + ": "
                    + event.getValue());
        }
    }
}
