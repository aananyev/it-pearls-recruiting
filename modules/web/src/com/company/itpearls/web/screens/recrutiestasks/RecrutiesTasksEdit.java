package com.company.itpearls.web.screens.recrutiestasks;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.EmailInfo;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
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
    private LookupPickerField<OpenPosition> recrutiesTasksField;
    @Inject
    private EmailService emailService;

    @Subscribe("windowExtendAndCloseButton")
    public void onWindowExtendAndCloseButtonClick(Button.ClickEvent event) {
        getEditedEntity().setEndDate( new Date( System.currentTimeMillis() + 1000L * 3600L * 24L * 7L ));

        commitChanges();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        String  role = "Researcher";

        startDateField.setValue( new Date() );

        // если роль - ресерчер, то автоматически вставить себя
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        // установить поле рекрутера
        if( s.contains(role) ) {
            recrutiesTasksFieldUser.setValue(userSession.getUser());

            recrutiesTasksFieldUser.setEnabled(false);
        }
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        String email = userSession.getUser().getEmail();
        EmailInfo   emailInfo;

        emailInfo = new EmailInfo( email,
                "Вы подписаны на вакансию " + recrutiesTasksField.getValue(),
                null, "com/company/itpearls/templates/subscribe_position.html",
                Collections.singletonMap("RescutiesTask", getEditedEntity() ) );

        emailService.sendEmailAsync(emailInfo);
    }
}