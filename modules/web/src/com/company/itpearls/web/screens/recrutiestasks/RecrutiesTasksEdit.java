package com.company.itpearls.web.screens.recrutiestasks;

import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.service.SubscribeDateService;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.EmailInfo;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

@UiController("itpearls_RecrutiesTasks.edit")
@UiDescriptor("recruties-tasks-edit.xml")
@EditedEntityContainer("recrutiesTasksDc")
@LoadDataBeforeShow
public class RecrutiesTasksEdit extends StandardEditor<RecrutiesTasks> {
    @Inject
    private DateField<Date> startDateField;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private LookupPickerField<User> recrutiesTasksFieldUser;
    @Inject
    private UserSession userSession;
    @Inject
    private EmailService emailService;
    @Inject
    private InstanceContainer<RecrutiesTasks> recrutiesTasksDc;
    @Inject
    private DateField<Date> endDateField;

    private OpenPosition openPosition = null;
    private Boolean fromOpenPosition = false;
    @Inject
    private LookupPickerField<OpenPosition> openPositionField;
    @Inject
    private CheckBox recrutiesTasksSubscribeCheckBox;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
    @Inject
    private SubscribeDateService subscribeDateService;
    @Inject
    private GetRoleService getRoleService;

    private String MANAGER = "Manager";

    @Subscribe("windowExtendAndCloseButton")
    public void onWindowExtendAndCloseButtonClick(Button.ClickEvent event) {
        getEditedEntity().setEndDate( subscribeDateService.dateOfNextMonday() );

        commitChanges();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        String  role = "Researcher";

        if( openPosition != null ) {
            openPositionField.setValue( openPosition );
        }

        startDateField.setValue( new Date() );

        // если роль - ресерчер, то автоматически вставить себя
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        // установить поле рекрутера
        if( s.contains(role) || !fromOpenPosition ) {
            recrutiesTasksFieldUser.setValue(userSession.getUser());

            if( s.contains(role) )
                recrutiesTasksFieldUser.setEnabled(false);
            else
                recrutiesTasksFieldUser.setEnabled(true);
            // поставить следующий понедельник
            LocalDate currentDate = LocalDate.now();
            LocalDate nextMonday = currentDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));

            Instant instant = nextMonday
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            Date legacyDate = Date.from( instant );

            endDateField.setValue( legacyDate );
        }
    }

    @Subscribe
    public void onBeforeCommitChanges(AfterCommitChangesEvent event) {
        getEditedEntity().setRecrutierName( userSession.getUser().getName() );

        // если были изменения, то послать оповещение по почте
        sendMessage();

        // если не установлен флаг подписки, то установить его в false
        if( recrutiesTasksSubscribeCheckBox.getValue() == null ) {
            dialogs.createOptionDialog()
                    .withCaption("Warning")
                    .withMessage("Подписатся на изменение вакансии?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES,
                                    Action.Status.PRIMARY).withHandler(e -> {
                                this.recrutiesTasksSubscribeCheckBox.setValue(true);
                            }),
                            new DialogAction(DialogAction.Type.NO).withHandler(f -> {
                                this.recrutiesTasksSubscribeCheckBox.setValue(false);
                            })
                    )
                    .show();
        }
    }

    private static DateFormatSymbols ruDateFormatSymbols = new DateFormatSymbols(){

        @Override
        public String[] getMonths() {
            return new String[]{"января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        }

    };

    private Boolean checkSubscribePosition() {
        Integer countSubscrine = dataManager
                .loadValue( "select count(e.reacrutier) from itpearls_RecrutiesTasks e " +
                    "where e.reacrutier = :recrutier and " +
                    "e.openPosition = :openPosition and " +
                    ":nowDate between e.startDate and e.endDate", Integer.class )
//                .parameter( "recrutier", recrutiesTasksFieldUser.getValue() )
                .parameter( "recrutier", getEditedEntity().getReacrutier() )
                .parameter( "openPosition", getEditedEntity().getOpenPosition() )
//                .parameter( "openPosition", openPositionField.getValue() )
                .parameter( "nowDate", new Date() )
                .one();

        // если нет соответствия, значит нет еще подписки, значит можно подписаться,
        // если уже есть, то не надо подписываться
        return countSubscrine > 0 ? true : false;
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Подписка" )
                .withDescription( recrutiesTasksFieldUser.getValue().getName() +
                        " подписан на позицию: \n" +
                        openPositionField.getValue().getVacansyName() )
                .show();
        
    }

    public void sendMessage() {
        String email = recrutiesTasksDc.getItem().getReacrutier().getEmail();

        EmailInfo   emailInfo;

        if(email != null) {
            if (!email.isEmpty()) {
                emailInfo = new EmailInfo(email,
                        "Вы подписаны на вакансию " + openPositionField.getValue().getVacansyName(),
                        null, "com/company/itpearls/templates/subscribe_to_position.html",
                        Collections.singletonMap("RecrutierTask", getEditedEntity()));

                emailInfo.setBodyContentType("text/html; charset=UTF-8");

                emailService.sendEmailAsync(emailInfo);
            }
        } else {
            notifications.create()
                    .withCaption("У Вас не зарегистрирован в профиле адрес электронной почты!")
                    .withDescription("Потдписка невозможна!")
                    .withPosition(Notifications.Position.MIDDLE_CENTER)
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    public void setOpenPosition( OpenPosition op ) {
      this.openPosition = op;
      fromOpenPosition = true;
    }

    @Subscribe("okButton")
    private void onOkButtonClick(Button.ClickEvent event) {

        SimpleDateFormat dateFormat = new SimpleDateFormat( "dd MMMM YYYY",
                ruDateFormatSymbols );

        // проверить, а вдруг вы уже подписаны на эту вакансию?
        if( checkSubscribePosition() && !getRoleService.isUserRoles( userSession.getUser(), MANAGER)) {
            dialogs.createMessageDialog()
                    .withCaption( "Внимание" )
                    .withModal( true )
                    .withMessage( "Вы уже подписаны на вакансию " + openPositionField.getValue().getVacansyName() +
                            "\n c " + dateFormat.format( startDateField.getValue() ) +
                            " по " + dateFormat.format( endDateField.getValue() ) )
                   .show();

            close(WINDOW_DISCARD_AND_CLOSE_ACTION);
        } else
            closeWithCommit();
    }

}