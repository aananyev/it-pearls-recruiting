package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
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
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private CollectionLoader<RecrutiesTasks> recrutiesTasksesDl;
    @Inject
    private EmailService emailService;
    @Inject
    private Notifications notifications;

    private Boolean booOpenClosePosition;
    private Boolean entityIsChanged = false;
    @Inject
    private LookupField<Integer> priorityField;

    @Subscribe(id = "openPositionDc", target = Target.DATA_CONTAINER)
    public void onOpenPositionDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<OpenPosition> event) {
        entityIsChanged = false;
    }

    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            jobCandidatesDl.setParameter("candidatePersonPosition", getEditedEntity().getPositionType().getPositionRuName());
            recrutiesTasksesDl.setParameter("openPosition", getEditedEntity().getVacansyName());
        } else {
            jobCandidatesDl.removeParameter("candidatePersonPosition");
            recrutiesTasksesDl.removeParameter("openPosition");
        }

        jobCandidatesDl.load();

        booOpenClosePosition = getEditedEntity().getOpenClose();
    }

    @Subscribe("companyDepartamentField")
    public void onCompanyDepartamentFieldValueChange(HasValue.ValueChangeEvent<CompanyDepartament> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getCompanyDepartament().getDepartamentRuName() != null)
                getEditedEntity().setCompanyName(getEditedEntity().getCompanyDepartament().getCompanyName());
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
                    null, "com/company/itpearls/templates/create_new_pos.txt",
                    Collections.singletonMap("openPosition", openPosition));

            emailService.sendEmailAsync(emailInfo);
            // высплывающее сообщение
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Открыта новая позиции:" )
                    .withDescription( getEditedEntity().getVacansyName() )
                    .show();
        } else {
            EmailInfo emailInfo = new EmailInfo( getSubscriberMaillist(getEditedEntity()) +
                    ";" + getRecrutiersMaillist(),
                    openPosition.getVacansyName(),
                    null, "com/company/itpearls/templates/edit_open_pos.html",
                    Collections.singletonMap("openPosition", openPosition));
            emailInfo.setBodyContentType( "text/html; charset=UTF-8" );

//                    Collections.singletonMap("bodyMessage", bodyMessage ));

            emailService.sendEmailAsync(emailInfo);

            // всплывающее сообщение
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Изменение описания позиции:" )
                    .withDescription( getEditedEntity().getVacansyName() )
                    .show();
        }
    }

    private Boolean sendOpenCloseMessage() {
        EmailInfo emailInfo;
        OpenPosition openPosition = getEditedEntity();

        Boolean setOK = getEditedEntity().getOpenClose();
        int a = setOK ? 1 : 0;
        int b = booOpenClosePosition ? 1 : 0;

        // если что-то изменилось
       if( a != b ) {

           if( !getEditedEntity().getOpenClose() ) {
               // позиция открылась
               notifications.create(Notifications.NotificationType.TRAY)
                       .withCaption("Открыта позиция" )
                       .withDescription( getEditedEntity().getVacansyName() )
                       .show();

               emailInfo = new EmailInfo( getSubscriberMaillist(getEditedEntity()) +
                       ";" + getRecrutiersMaillist(),
                       "Открыта позиция " + openPosition.getVacansyName(),
                       null, "com/company/itpearls/templates/open_position.html",
                       Collections.singletonMap("openPosition", openPosition));

           } else {
               // позиция закрылась
               notifications.create(Notifications.NotificationType.TRAY)
                       .withCaption("Закрыта позиция" )
                       .withDescription( getEditedEntity().getVacansyName() )
                       .show();

               emailInfo = new EmailInfo( getSubscriberMaillist(getEditedEntity()) +
                       ";" + getRecrutiersMaillist(),
                       "Закрыта позиция " + openPosition.getVacansyName(),
                       null, "com/company/itpearls/templates/close_position.html",
                       Collections.singletonMap("openPosition", openPosition));
           }

           emailInfo.setBodyContentType( "text/html; charset=UTF-8" );

           emailService.sendEmailAsync(emailInfo);

           notifications.create(Notifications.NotificationType.TRAY)
               .withCaption("Рассылка обновлений позиции")
               .withDescription("Рассылка по адресам: " + getSubscriberMaillist(getEditedEntity()))
               .show();

           return true;
       } else
           return false;
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
        // отправить глобальный мессагу
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


        return "alan@itpearls.ru;tdgitpearls.ru;tmd@itpearls.ru";
    }

    @Subscribe
    public void onInit(InitEvent event) {
        Map<String, Integer> priorityMap = new LinkedHashMap<>();

        priorityMap.put("Paused", 0);
        priorityMap.put("Low", 1);
        priorityMap.put("Normal", 2);
        priorityMap.put("High", 3);
        priorityMap.put("Critical", 4);

        priorityField.setOptionsMap( priorityMap );
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
}
