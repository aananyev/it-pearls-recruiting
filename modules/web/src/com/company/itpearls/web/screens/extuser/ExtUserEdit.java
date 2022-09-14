package com.company.itpearls.web.screens.extuser;

import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.screen.Install;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_ExtUserEdit")
@UiDescriptor("ext-user-edit.xml")
public class ExtUserEdit extends Screen {
    @Inject
    private Field smtpPassword;

    @Install(to = "emailFieldPasswordRequired.smtpPasswordRequired", subject = "validator")
    private void emailFieldPasswordRequiredSmtpPasswordRequiredValidator(Boolean aBoolean) {
        smtpPassword.setRequired(aBoolean);
    }

}