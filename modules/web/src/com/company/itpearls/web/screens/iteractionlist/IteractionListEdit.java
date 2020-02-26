package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;

@UiController("itpearls_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEdit extends StandardEditor<IteractionList> {
    @Inject
    private DataManager iteractionListEditDataManager;
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

    protected Project currentProject;
    protected Boolean newProject;
    static Boolean myClient;
    protected User lastUser = null;
    protected Date lastIteraction;
    protected JobCandidate candidate;
    private static Boolean isCopyButton;


    @Inject
    private CollectionLoader<Iteraction> iteractionTypesLc;
    @Inject
    private UiComponents uiComponents;

    @Subscribe(id = "iteractionListDc", target = Target.DATA_CONTAINER)
    private void onIteractionListDcItemChange(InstanceContainer.ItemChangeEvent<IteractionList> event) {
        if( PersistenceHelper.isNew(getEditedEntity()) ) {
            setIteractionNumber();
            setCurrentDate();
            setCurrentUserName(); // имя пользователя в нередактируемое поле
        }
    }

    @Subscribe("candidateField")
    public void onCandidateFieldValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
        // запомним кандидата
       candidate = getEditedEntity().getCandidate(); 
    }

    @Subscribe("vacancyFiels")
    public void onVacancyFielsValueChange(HasValue.ValueChangeEvent<OpenPosition> event) {
        BigDecimal a = new BigDecimal("0.0");

        // проверка на наличие записей по этой вакансии
        BigDecimal countIteraction = dataManager.loadValue("select count(e.numberIteraction) " +
                        "from itpearls_IteractionList e " +
                        "where e.candidate = :candidate and " +
                        "e.vacancy = :vacancy", BigDecimal.class )
            .parameter( "candidate", getEditedEntity().getCandidate() )
            .parameter( "vacancy", getEditedEntity().getVacancy() )
            .one();

            // есть взаимодействия с кандидатом по этой позиции
        if( countIteraction.compareTo( a ) != 0)
            newProject = false;
        else
            // новое взаимодействие с кандидатом
        newProject = true;

//        getEditedEntity().setIteractionChain(parentChain);

        if ( newProject ) {
        // это начало цепочки - обрезаем выбор
            iteractionTypesLc.setParameter("number", "001");

            dialogs.createMessageDialog()
                   .withCaption("Warnind!")
                   .withMessage("С кандидатом начат новый процесс. " +
                                "Начните взаимодействие с ним с типом из группы \"001 Ресерчинг\"")
                   .show();
            } else {
                iteractionTypesLc.removeParameter("number");
            }

            iteractionTypesLc.load();

            // заполнить другие поля
            if (getEditedEntity().getVacancy() != null)
                if (getEditedEntity().getVacancy().getProjectName() != null)
                    getEditedEntity().setProject(getEditedEntity().getVacancy().getProjectName());
    }

    @Subscribe("projectField")
    public void onProjectFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        if( getEditedEntity().getVacancy() != null )
            if( getEditedEntity().getVacancy().getCompanyDepartament() != null )
                getEditedEntity().setCompanyDepartment(getEditedEntity().getVacancy().getCompanyDepartament());
    }


    private BigDecimal getCountIteraction() {
        BigDecimal maxValue = iteractionListEditDataManager.loadValue(
                "select max(a.numberIteraction) from itpearls_IteractionList a",
                            BigDecimal.class).one().add(BigDecimal.ONE);

        return maxValue;
    }

    protected void setIteractionNumber() {
        getEditedEntity().setNumberIteraction(getCountIteraction());
    }

    protected void setCurrentDate() {
        getEditedEntity().setDateIteraction(new Date());
    }

    // не могу получить имя пользователя и записать его в базу
    protected String getCurrentUser() {
        String currentUser =
                userSessionSource.getUserSession().getUser().getName();
        return currentUser;
    }

    @Subscribe("candidateField")
    public void onCandidateFieldValueChange1(HasValue.ValueChangeEvent<JobCandidate> event) {
        if( yourCandidate() ) {
            if( PersistenceHelper.isNew( getEditedEntity() )) {
                // сколько записей есть по этому кандидату
                if (getIteractionCount() != 0) {
                    // ввели кандидата - предложи скопировать предыдущую запись
                    if( !isCopyButton ) {
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
            String msg = "С этим кандидатом " + lastUser.getName() + " контактировал " + lastIteraction.toString() +
                    " МЕНЕЕ МЕСЯЦА НАЗАД!";

            dialogs.createOptionDialog()
                    .withCaption( "Warning" )
                    .withMessage( msg + "\nПродолжить?" )
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

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if( getEditedEntity().getIteractionType().getNumber() != null ) {
            String s = getEditedEntity().getIteractionType().getNumber();
            Integer i = Integer.parseInt(s);

            getEditedEntity().getCandidate().setStatus(i);
        }
    }

    private boolean yourCandidate() {
        // твой ли это кандидат?
        User user = userSession.getUser();

        myClient = false;
        BigDecimal numberIteraction;
        // проверка прошлого взаимодействия
        try {
            numberIteraction = dataManager.loadValue( "select e.numberIteraction " +
                    "from itpearls_IteractionList e " +
                    "where e.candidate = :candidate and " +
                    "e.numberIteraction = " +
                    "(select max(f.numberIteraction) " +
                    "from itpearls_IteractionList f " +
                    "where f.candidate = :candidate)", BigDecimal.class )
                    .parameter( "candidate", getEditedEntity().getCandidate() )
                    .one();
        } catch ( IllegalStateException e) {
            // не было взаимодействий с кандидатом
            numberIteraction = null;

            myClient = true;
        }

        if( numberIteraction != null ) {
            // больше месяца назад?
            lastIteraction = dataManager.loadValue( "select e.dateIteraction " +
                    "from itpearls_IteractionList e " +
                    "where e.numberIteraction = :number", Date.class )
                    .parameter( "number", numberIteraction )
                    .one();
            if( lastIteraction.before( new Date(System.currentTimeMillis() - 2628000000l)  ) ) {
                // все зашибись и кандидат свободен
                myClient = true;
            } else {
                // а это точно не ты?
                try {
                    lastUser = dataManager.loadValue("select e.recrutier " +
                            "from itpearls_IteractionList e " +
                            "where e.numberIteraction = :number", User.class)
                            .parameter("number", numberIteraction)
                            .one();
                } catch ( IllegalStateException e ) {
                    // не записан пользователь из старых записей
                    myClient = false;
                    lastUser = null;
                }

                if( user.equals( lastUser ) )
                    myClient = true;
                else
                    myClient = false;
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
                    .parameter("candidate", getEditedEntity().getCandidate() )
                    .one();
        } catch ( NullPointerException | IllegalStateException e ) {
            d = 0;
        }

        return d;
    }

    private void copyPrevionsItems() {
        // а вдруг позиция уже закрыта? надо разрешить в вакансию писать даже закрытые
        openPositionsDl.setQuery("select e from itpearls_OpenPosition e " +
                "order by e.vacansyName");
        openPositionsDl.load();

        OpenPosition openPos;
        // вакансия
        // а вдруг в результате экспорта не были заполнены поля
        try {
            openPos = dataManager.load(OpenPosition.class)
                    .query("select e.vacancy " +
                            "from itpearls_IteractionList e " +
                            "where e.candidate = :candidate and " +
                            "e.numberIteraction = " +
                            "(select max(f.numberIteraction) " +
                            "from itpearls_IteractionList f " +
                            "where f.candidate = :candidate)")
                    .parameter("candidate", getEditedEntity().getCandidate() )
                    .view("openPosition-view")
                    .one();
        } catch ( IllegalStateException e ) {
            openPos = null;
        }
        // а вдруг пустое поле?
        if( openPos != null ) {
            getEditedEntity().setVacancy( openPos );
        }
        // проект
        getEditedEntity().setProject( dataManager.loadValue(
                "select e.project " +
                        "from itpearls_IteractionList e " +
                        "where e.candidate = :candidate and " +
                        "e.numberIteraction = " +
                        "(select max(f.numberIteraction) " +
                        "from itpearls_IteractionList f " +
                        "where f.candidate = :candidate)", Project.class )
                .parameter( "candidate", getEditedEntity().getCandidate() )
                .one() );
        // департамент
        CompanyDepartament companyDepartment = dataManager.load( CompanyDepartament.class )
                .query(
                        "select e.companyDepartment " +
                                "from itpearls_IteractionList e " +
                                "where e.candidate = :candidate and " +
                                "e.numberIteraction = " +
                                "(select max(f.numberIteraction) " +
                                "from itpearls_IteractionList f " +
                                "where f.candidate = :candidate)" )
                .parameter( "candidate", getEditedEntity().getCandidate() )
                .view( "companyDepartament-view" )
                .one();

        if( companyDepartment != null )
            getEditedEntity().setCompanyDepartment( companyDepartment );
        /*
        // создание цепочки
        parentChain = dataManager.load( IteractionList.class )
                .query( "select e " +
                        "from itpearls_IteractionList e " +
                        "where e.candidate.fullName = :candidate and " +
                        "e.numberIteraction = " +
                        "(select max(f.numberIteraction) " +
                        "from itpearls_IteractionList f " +
                        "where f.candidate.fullName = :candidate)" )
                .parameter( "candidate", getEditedEntity().getCandidate().getFullName() )
                .view( "iteractionList-view" )
                .one();
        getEditedEntity().setIteractionChain( parentChain ); */
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        buttonCallAction.setVisible(false);
        // запомнить текущий проект
        currentProject = getEditedEntity().getProject();

        candidate = getEditedEntity().getCandidate();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if(PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setRecrutier(userSession.getUser());
        }
    }

    protected void setCurrentUserName() {
        String currentUser = getCurrentUser();
    }

    // изменение надписи на кнопке в зависимости от щначения поля ItercationType
    @Subscribe("iteractionTypeField")
    public void onIteractionTypeFieldValueChange(HasValue.ValueChangeEvent<Iteraction> event) {
        // надпись на кнопке
        if( iteractionTypeField.getValue().getCallButtonText() != null )
            buttonCallAction.setCaption(iteractionTypeField.getValue().getCallButtonText());
        // если установлен тип взаиподейтвия и нужно действие
        if( getEditedEntity().getIteractionType() != null )
            if( iteractionTypeField.getValue().getCallForm() != null )
                if(iteractionTypeField.getValue().getCallForm())
                    buttonCallAction.setVisible(true);
                else
                    buttonCallAction.setVisible(false);
                // если надо специальное поле
                if( iteractionTypeField.getValue().getAddFlag() ) {
                    // удалить кнопку
                    TextField<Integer> textFieldInteger;
                    TextField<String> textFieldString;
                    TextField<Date> textFieldDate;

                    switch( iteractionTypeField.getValue().getAddType() ) {
                        case 1: // поле Data
                            textFieldDate = uiComponents.create( TextField.class );
                            textFieldDate.setCaption( iteractionTypeField.getValue().getAddCaption() );
                            textFieldDate.setWidth("50%");
                            break;
                        case 2: // поле String
                            textFieldString = uiComponents.create( TextField.class );
                            textFieldString.setCaption( iteractionTypeField.getValue().getAddCaption() );
                            textFieldString.setWidth("50%");
                            break;
                        case 3: // поле Integer
                            textFieldInteger = uiComponents.create( TextField.TYPE_INTEGER );
                            textFieldInteger.setCaption( iteractionTypeField.getValue().getAddCaption() );
                            textFieldInteger.setWidth("50%");
                            break;
                    }
                }
    }

    @Subscribe
    public void onInit(InitEvent event) {
        isCopyButton = false;
        // изначально предполагаем, что это продолжение проекта
        newProject = false;
        // вся сортировка в поле IteractionType
        iteractionTypesLc.removeParameter("number");
    }

    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Metadata metadata;

    public void callActionEntity() {
        String calledClass = getEditedEntity().getIteractionType().getCallClass();
        // еслп установлено разрешение в Iteraction показать кнопку и установить на ней надпсит
        if( getEditedEntity().getIteractionType() != null)
            if( getEditedEntity().getIteractionType().getCallForm() != null )
                if(!getEditedEntity().getIteractionType().getCallForm()) {
                }
                else {
                    screenBuilders.editor(metadata.getClassNN(calledClass).getJavaClass(), this)
                            .newEntity()
                            .withScreenId(calledClass + ".edit")
                            .withLaunchMode(OpenMode.NEW_TAB)
                            .build()
                            .show();
        }
    }

    public void addNewIteraction() {
        String classIL = "itpearls_" + getEditedEntity().getClass().getSimpleName() + ".edit";

        closeWithCommit();
        isCopyButton = true;

        screenBuilders.editor( IteractionList.class, this )
                .newEntity()
                .withScreenId( classIL )
                .withLaunchMode( OpenMode.NEW_TAB )
                .withInitializer( e -> {
                    e.setCandidate(this.getEditedEntity().getCandidate());
                    e.setVacancy(this.getEditedEntity().getVacancy());
                    e.setCompanyDepartment(this.getEditedEntity().getCompanyDepartment());
                })
                .build()
                .show();

        isCopyButton = false;
    }
}
