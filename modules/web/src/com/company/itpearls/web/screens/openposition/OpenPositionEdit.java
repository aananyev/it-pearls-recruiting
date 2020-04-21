package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.BeanNotificationEvent;
import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.service.GetUserRoleService;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.context.event.EventListener;

import java.util.*;
import javax.inject.Inject;


@UiController("itpearls_OpenPosition.edit")
@UiDescriptor("open-position-edit.xml")
@EditedEntityContainer("openPositionDc")
@LoadDataBeforeShow
public class OpenPositionEdit extends StandardEditor<OpenPosition> {
    @Inject
    private LookupPickerField<City> cityOpenPositionField;
    @Inject
    private LookupPickerField<CompanyDepartament> companyDepartamentField;
    @Inject
    private LookupPickerField<Company> companyNameField;
    @Inject
    private TextField<Integer> numberPositionField;
    @Inject
    private LookupPickerField<Position> positionTypeField;
    @Inject
    private LookupPickerField<Project> projectNameField;
    @Inject
    private TextField<String> vacansyNameField;
    @Inject
    private CollectionLoader<RecrutiesTasks> recrutiesTasksesDl;
    @Inject
    private EmailService emailService;
    @Inject
    private Notifications notifications;
    @Inject
    private LookupField<Integer> priorityField;
    @Inject
    private CheckBox openClosePositionCheckBox;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;

    private Boolean booOpenClosePosition = false;
    private Boolean entityIsChanged = false;
    private EmailInfo emailInfo;
    private String  emails = "";
    private Boolean setOK;

    @Inject
    private Dialogs dialogs;
    private boolean r;
    @Inject
    private Events events;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionLoader<Person> personsDl;
    @Inject
    private Table<Person> personTable;
    @Inject
    private RadioButtonGroup radioButtonGroupPaymentsType;
    @Inject
    private RadioButtonGroup radioButtonGroupResearcherSalary;
    @Inject
    private RadioButtonGroup radioButtonGroupRecrutierSalary;
    @Inject
    private GroupBoxLayout groupBoxPaymentsDetail;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private GroupBoxLayout groupBoxPaymentsResearcher;
    @Inject
    private GroupBoxLayout groupBoxPaymentsRecrutier;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            recrutiesTasksesDl.setParameter("openPosition", getEditedEntity().getVacansyName());
        } else {
            recrutiesTasksesDl.removeParameter("openPosition");
        }
        recrutiesTasksesDl.load();

        booOpenClosePosition = getEditedEntity().getOpenClose();

        // проверка на ноль
        booOpenClosePosition = booOpenClosePosition == null ? false : booOpenClosePosition;
    }

    @Subscribe("companyDepartamentField")
    public void onCompanyDepartamentFieldValueChange(HasValue.ValueChangeEvent<CompanyDepartament> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getCompanyDepartament().getDepartamentRuName() != null) {
                getEditedEntity().setCompanyName(getEditedEntity().getCompanyDepartament().getCompanyName());

                personTable.setVisible(false);
            }
        } else {
            if( companyDepartamentField.getValue() != null ) {
                personsDl.setParameter("companyDepartment", companyDepartamentField.getValue());
                personsDl.load();

                personTable.setVisible(true);
            }
        }
    }

    @Subscribe("companyNameField")
    public void onCompanyNameFieldValueChange(HasValue.ValueChangeEvent<Company> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getCompanyName().getCityOfCompany() != null)
                getEditedEntity().setCityPosition(getEditedEntity().getCompanyName().getCityOfCompany());
        }
    }

    @Subscribe("projectNameField")
    public void onProjectNameFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getProjectName().getProjectDepartment() != null)
                getEditedEntity().setCompanyDepartament(getEditedEntity().getProjectName().getProjectDepartment());
        }
    }

    @Subscribe("openClosePositionCheckBox")
    public void onOpenClosePositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (getEditedEntity().getOpenClose()) {
            cityOpenPositionField.setEditable(false);
            companyDepartamentField.setEditable(false);
            companyNameField.setEditable(false);
            numberPositionField.setEditable(false);
            positionTypeField.setEditable(false);
            projectNameField.setEditable(false);
            vacansyNameField.setEditable(false);
        } else {
            cityOpenPositionField.setEditable(true);
            companyDepartamentField.setEditable(true);
            companyNameField.setEditable(true);
            numberPositionField.setEditable(true);
            positionTypeField.setEditable(true);
            projectNameField.setEditable(true);
            vacansyNameField.setEditable(true);
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setOpenClose(false);
        }
    }

    private void sendMessage() {
        // по почте
        OpenPosition openPosition = getEditedEntity();

        // нотификация
        if (PersistenceHelper.isNew(getEditedEntity())) {
            // пошлем по почте
            EmailInfo emailInfo = new EmailInfo( getSubscriberMaillist(getEditedEntity()) +
                    ";" + getRecrutiersMaillist(),
                    openPosition.getVacansyName(),
                    null, "com/company/itpearls/templates/create_new_pos.html",
                    Collections.singletonMap("openPosition", openPosition));

            emailInfo.setBodyContentType("text/html; charset=UTF-8");

            emailService.sendEmailAsync(emailInfo);
            // высплывающее сообщение
            events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                    getEditedEntity().getVacansyName()));
        } else {
            if( entityIsChanged ) {
                EmailInfo emailInfo = new EmailInfo(getSubscriberMaillist(getEditedEntity()) +
                        ";" + getRecrutiersMaillist(),
                        openPosition.getVacansyName(),
                        null, "com/company/itpearls/templates/edit_open_pos.html",
                        Collections.singletonMap("openPosition", openPosition));
                emailInfo.setBodyContentType("text/html; charset=UTF-8");

                emailService.sendEmailAsync(emailInfo);
            }
        }
    }

    private Boolean sendOpenCloseMessage() {
        OpenPosition openPosition = getEditedEntity();
        r = false;

        setOK = getEditedEntity().getOpenClose();

        int a = setOK ? 1 : 0;
        int b = booOpenClosePosition ? 1 : 0;


        // если что-то изменилось
        if( PersistenceHelper.isNew( getEditedEntity() ) ) {
            emails = getAllSubscibers();
            // позиция открылась
            events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                    getEditedEntity().getVacansyName()));

            dialogs.createOptionDialog()
                    .withCaption( "Внимание!" )
                    .withMessage( "Разослать email-оповещение по всем сотрудникам об открытии новой позиции?")
                    .withActions( new DialogAction(DialogAction.Type.YES,
                                    Action.Status.PRIMARY).withHandler(e -> {
                                this.emailInfo = new EmailInfo( this.emails,
                                        "Открыта позиция " + openPosition.getVacansyName(),
                                        null, "com/company/itpearls/templates/open_position.html",
                                        Collections.singletonMap( "openPosition", openPosition ) );

                                emailInfo.setBodyContentType("text/html; charset=UTF-8");

                                emailService.sendEmailAsync(emailInfo);

                                notifications.create(Notifications.NotificationType.TRAY)
                                        .withCaption("Рассылка обновлений позиции")
                                        .withDescription("Рассылка по адресам: " + emails)
                                        .show();

                            }),
                            new DialogAction(DialogAction.Type.NO))
                    .show();

            r = true;
        } else {
            if( a != b ) {
               if( !getEditedEntity().getOpenClose() ) {

                   emails = getAllSubscibers();
                   // позиция открылась
                   events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                            getEditedEntity().getVacansyName()));

                   if( getRoleService.isUserRoles( userSession.getUser(), "Manager" ) ) {
                       dialogs.createOptionDialog()
                               .withCaption("Внимание!")
                               .withMessage("Разослать email-оповещение по всем сотрудникам?")
                               .withActions(new DialogAction(DialogAction.Type.YES,
                                               Action.Status.PRIMARY).withHandler(e -> {
                                           this.emailInfo = new EmailInfo(this.emails,
                                                   "Открыта позиция " + openPosition.getVacansyName(),
                                                   null, "com/company/itpearls/templates/open_position.html",
                                                   Collections.singletonMap("openPosition", openPosition));

                                           this.setOK = true;

                                           emailInfo.setBodyContentType("text/html; charset=UTF-8");

                                           emailService.sendEmailAsync(emailInfo);

                                           notifications.create(Notifications.NotificationType.TRAY)
                                                   .withCaption("Рассылка обновлений позиции")
                                                   .withDescription("Рассылка по адресам: " + emails)
                                                   .show();

                                       }),
                                       new DialogAction(DialogAction.Type.NO).withHandler(f -> {
                                           this.setOK = false;
                                       }))
                               .show();
                   }

                   r = true;
               } else {
                   // позиция закрылась
                   events.publish(new UiNotificationEvent(this, "Закрыта позиция: " +
                           getEditedEntity().getVacansyName()));

                   emails = getSubscriberMaillist(getEditedEntity()) +
                           ";" + getRecrutiersMaillist();

                   emailInfo = new EmailInfo( emails,
                           "Закрыта позиция " + openPosition.getVacansyName(),
                            null, "com/company/itpearls/templates/close_position.html",
                            Collections.singletonMap("openPosition", openPosition));

                   emailInfo.setBodyContentType("text/html; charset=UTF-8");

                   emailService.sendEmailAsync(emailInfo);

                   notifications.create(Notifications.NotificationType.TRAY)
                           .withCaption("Рассылка обновлений позиции")
                           .withDescription("Рассылка по адресам: " + emails)
                           .show();

                   setOK = true;
               }

                r = true;
            } else
                r = false;
        }

        return r;
    }


    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        // если не изменился статус открыто/закрыто
        if( !sendOpenCloseMessage() ) {
            // отослать сооьщение об изменении позиции
            if (entityIsChanged) {
                sendMessage();
            }
        }

        if( openClosePositionCheckBox.getValue() == null )
            openClosePositionCheckBox.setValue( false );
        // отправить глобальный мессагу
/*        if( PersistenceHelper.isNew( getEditedEntity() ) ) {
            events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                    getEditedEntity().getVacansyName()));
        } else {
            events.publish(new UiNotificationEvent(this, "Изменено описание пвакансии: " +
                    getEditedEntity().getVacansyName()));
        } */
    }

    private String getAllSubscibers() {
        LoadContext<User> loadContext = LoadContext.create(User.class)
                .setQuery(LoadContext.createQuery("select e from sec$User e" ));

        List<User> listManagers = dataManager.loadList(loadContext );

        String maillist = "";

        for( User user : listManagers ) {
            if( user.getEmail() != null && user.getActive() )
            maillist = maillist + user.getEmail() + ";";
        }

        return maillist;
    }

    private String getSubscriberMaillist(Entity entity)
    {
        List<RecrutiesTasks> listResearchers = dataManager.load(RecrutiesTasks.class)
                .query("select e " +
                    "from itpearls_RecrutiesTasks e " +
                    "where e.endDate  >= :currentDate and " +
                        "e.openPosition = :openPosition" )
                .parameter( "currentDate", new Date() )
                .parameter( "openPosition", entity )
                .view("recrutiesTasks-view")
                .list();



        String maillist = "";
        Boolean subs = false;

        for( RecrutiesTasks address : listResearchers ) {
            String email = address.getReacrutier().getEmail();

            if( address.getSubscribe() == null ? false : address.getSubscribe() )
                // if( address.getSubscribe() )
                if( email != null )
                    maillist = maillist + email + ";";
        }

       return maillist;
    }

    private String getRecrutiersMaillist() {
        /* LoadContext<User> loadContext = LoadContext.create(User.class)
                .setQuery(LoadContext.createQuery("select e from sec$User e where " +
                        "e.userRoles.name = :roleName" )
                .setParameter("roleName", "Headhunter"));

        List<User> listManagers = dataManager.loadList(loadContext );
        String      list = "";

        for( User user : listManagers ) {
            list = list + user.getEmail() + ";";
        } */


        return "alan@itpearls.ru;tmd@itpearls.ru;tdg@itpearls.ru";
    }

    private void setRadioButtons() {
        Map<String, Integer> priorityMap = new LinkedHashMap<>();

        priorityMap.put("Paused", 0);
        priorityMap.put("Low", 1);
        priorityMap.put("Normal", 2);
        priorityMap.put("High", 3);
        priorityMap.put("Critical", 4);

        priorityField.setOptionsMap( priorityMap );

        Map<String, Integer> paymentsType = new LinkedHashMap<>();
        paymentsType.put( "Фиксированная оплата", 0);
        paymentsType.put( "Процент от зарплаты кандидата", 1);
        paymentsType.put( "Процент от зарплаты кандидата + НДФЛ 13%", 2);

        radioButtonGroupPaymentsType.setOptionsMap( paymentsType );

        Map<String, Integer> researcherSalary = new LinkedHashMap<>();
        researcherSalary.put( "Фиксированная комиссия", 0 );
        researcherSalary.put( "Процент комиссии компании", 1 );

        radioButtonGroupResearcherSalary.setOptionsMap( researcherSalary );

        Map<String, Integer> recrutierSalary = new LinkedHashMap<>();
        recrutierSalary.put( "Фиксированная комиссия", 0 );
        recrutierSalary.put( "Процент комиссии компании", 1 );

        radioButtonGroupRecrutierSalary.setOptionsMap( recrutierSalary );
    }

    private void setHiddeField() {
        // скрыть менеджерские пункты
       if( isUserRoles( userSession.getUser(), "Manager" ) ) {
           groupBoxPaymentsDetail.setVisible( true );
           groupBoxPaymentsResearcher.setVisible( true );
           groupBoxPaymentsRecrutier.setVisible( true );
       } else {
           groupBoxPaymentsDetail.setVisible( false );

           if( isUserRoles( userSession.getUser(), "Researcher " ) ) {
               groupBoxPaymentsResearcher.setVisible( true );
               groupBoxPaymentsRecrutier.setVisible( false );
           } else {
               if( isUserRoles( userSession.getUser(), "Recrutier" ) ) {
                   groupBoxPaymentsResearcher.setVisible(false);
                   groupBoxPaymentsRecrutier.setVisible(true);
               }
           }
       }
    }

    @Subscribe
    public void onInit(InitEvent event) {
        setRadioButtons();

        setHiddeField();
    }

    @Install(to = "priorityField", subject = "optionIconProvider")
    private String priorityFieldOptionIconProvider(Integer integer) {

        String icon = null;

        switch ( integer ) {
            case 0: //"Paused"
                icon = "icons/remove.png";
                break;
            case 1: //"Low"
                icon = "icons/traffic-lights_blue.png";
                break;
            case 2: //"Normal"
                icon = "icons/traffic-lights_green.png";
                break;
            case 3: //"High"
                icon = "icons/traffic-lights_yellow.png";
                break;
            case 4: //"Critical"
                icon = "icons/traffic-lights_red.png";
                break;
        }

        return icon;
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onChange(DataContext.ChangeEvent event) {
        entityIsChanged = true;
    }

    public void subscribePosition() {
        Screen opScreen = screenBuilders
                .editor( RecrutiesTasks.class, this )
                .newEntity()
                .withInitializer( data -> {
                    data.setOpenPosition( this.getEditedEntity() );
                })
                .newEntity()
                .withScreenId("itpearls_RecrutiesTasks.edit")
                .withLaunchMode(OpenMode.DIALOG)
                .build();


        opScreen.show();
    }

    public Boolean isUserRoles(User user, String role) {
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        Boolean c = false;
        // установить поле рекрутера
        for (String a : s) {
            if (a.contains(role)) {
                c = true;
                break;
            }
        }
        return c;
    }
}
