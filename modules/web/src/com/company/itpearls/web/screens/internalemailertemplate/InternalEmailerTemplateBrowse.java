package com.company.itpearls.web.screens.internalemailertemplate;

import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.internalemailer.InternalEmailerEdit;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.web.screens.internalemailer.InternalEmailerBrowse;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_InternalEmailerTemplate.browse")
@UiDescriptor("internal-emailer-template-browse.xml")
public class InternalEmailerTemplateBrowse extends InternalEmailerBrowse {
    @Inject
    private CollectionLoader<InternalEmailer> emailersDl;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataGrid<InternalEmailerTemplate> emailersTable;

    public void setEmailTemplateFilter(InternalEmailTemplate internalEmailTemplate) {
        emailersDl.setParameter("emailTemplate", internalEmailTemplate);
        emailersDl.load();
    }

    @Override
    protected void addNewInteractionAction(InternalEmailer internalEmailer) {
        emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
        screenBuilders.editor(IteractionList.class, this)
                .withInitializer(event -> {
                    event.setCandidate(internalEmailer.getToEmail());
                    event.setRecrutier(internalEmailer.getFromEmail());
                    event.setRecrutierName(internalEmailer.getFromEmail().getName());
                    event.setDateIteraction(new Date());
                    event.setVacancy(((InternalEmailerTemplate)internalEmailer).getEmailTemplate().getTemplateOpenPosition());
                })
                .newEntity()
                .build()
                .show();
    }

    @Override
    protected void resendEmailAction(InternalEmailer internalEmailer) {
        screenBuilders.editor(InternalEmailerTemplate.class, this)
                .newEntity()
                .withInitializer(emailer -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    emailer.setReplyInternalEmailer(internalEmailer);
                    emailer.setToEmail(internalEmailer.getToEmail());
                })
                .withOpenMode(OpenMode.DIALOG)
                .build()
                .show();
    }
}