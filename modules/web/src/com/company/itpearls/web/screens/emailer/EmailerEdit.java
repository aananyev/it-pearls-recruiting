package com.company.itpearls.web.screens.emailer;

import com.company.itpearls.entity.ExtUser;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.PickerField;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Emailer;
import com.haulmont.cuba.security.app.UserSessionService;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_Emailer.edit")
@UiDescriptor("emailer-edit.xml")
@EditedEntityContainer("emailerDc")
@LoadDataBeforeShow
public class EmailerEdit extends StandardEditor<Emailer> {
    @Inject
    private PickerField<ExtUser> fromEmailField;
    @Inject
    private UserSessionService userSessionService;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox draftEmailField;
    @Inject
    private DateField<Date> dateCreateEmailField;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        fromEmailField.setValue((ExtUser) userSession.getUser());

        if(PersistenceHelper.isNew(getEditedEntity())) {
            draftEmailField.setValue(true);
            dateCreateEmailField.setValue(new Date());
        }
    }
}