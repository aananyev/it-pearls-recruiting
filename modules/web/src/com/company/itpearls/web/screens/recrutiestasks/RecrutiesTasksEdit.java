package com.company.itpearls.web.screens.recrutiestasks;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.EmailInfo;
import com.haulmont.cuba.core.global.PersistenceHelper;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

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

    @Subscribe("windowExtendAndCloseButton")
    public void onWindowExtendAndCloseButtonClick(Button.ClickEvent event) {
        getEditedEntity().setEndDate( new Date( System.currentTimeMillis() + 1000L * 3600L * 24L * 7L ));

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
            LocalDate nextMonday = currentDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));

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
        String emailSubscriber = userSession.getUser().getEmail();
        String openPos = openPositionField.getValue().getVacansyName();

        EmailInfo   emailInfo;

        emailInfo = new EmailInfo( email,
                "Вы подписаны на вакансию " + openPositionField.getValue().getVacansyName(),
                null, "com/company/itpearls/templates/subscribe_to_position.html",
                Collections.singletonMap( "RecrutierTask", getEditedEntity() ) );

        emailInfo.setBodyContentType( "text/html; charset=UTF-8" );

//        emailInfo.setTemplateParameters( Collections.singletonMap("startDate", startDateField.getValue() ) );
//        emailInfo.setTemplateParameters( Collections.singletonMap("reacrutier", emailSubscriber ) );

        emailService.sendEmailAsync(emailInfo);

    }

    public void setOpenPosition( OpenPosition op ) {
      this.openPosition = op;
      fromOpenPosition = true;
    }
}