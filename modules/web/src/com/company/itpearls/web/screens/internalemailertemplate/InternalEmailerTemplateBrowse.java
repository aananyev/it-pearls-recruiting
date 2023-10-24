package com.company.itpearls.web.screens.internalemailertemplate;

import com.company.itpearls.entity.*;
import com.company.itpearls.service.OpenPositionNewsService;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.web.screens.internalemailer.InternalEmailerBrowse;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import javax.persistence.NoResultException;
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
    @Inject
    private UserSession userSession;

    public void setEmailTemplateFilter(InternalEmailTemplate internalEmailTemplate) {
        emailersDl.setParameter("emailTemplate", internalEmailTemplate);
        emailersDl.load();
    }

    @Override
    protected void putCandidatesToPersonelReserve(InternalEmailer singleSelected) {

        if (((InternalEmailerTemplate) singleSelected)
                .getEmailTemplate().getTemplateOpenPosition() == null) {
            putCandidatesToPersonelReserve(singleSelected, getDefaultOpenPosition());
        } else {
            putCandidatesToPersonelReserve(singleSelected,
                    ((InternalEmailerTemplate)singleSelected)
                            .getEmailTemplate().getTemplateOpenPosition());
        }
    }

    @Override
    protected PersonelReserve setToPersonelReserve(PersonelReserve event) {
        event.setRecruter((ExtUser) userSession.getUser());
        event.setJobCandidate(emailersTable.getSingleSelected().getToEmail());
        event.setSelectedForAction(emailersTable
                .getSingleSelected()
                .getSelectedForAction() != null ?
                emailersTable
                        .getSingleSelected()
                        .getSelectedForAction() : false);
        event.setSelectionSymbolForActions(emailersTable
                .getSingleSelected()
                .getSelectionSymbolForActions() != null ? emailersTable
                .getSingleSelected()
                .getSelectionSymbolForActions() : 0);
        if (emailersTable.getSingleSelected().getEmailTemplate() != null) {
            if (emailersTable.getSingleSelected().getEmailTemplate().getTemplateOpenPosition() != null) {
                event.setOpenPosition(emailersTable
                        .getSingleSelected()
                        .getEmailTemplate()
                        .getTemplateOpenPosition());
            }
        }

        if (emailersTable.getSingleSelected().getToEmail().getPersonPosition() != null) {
            event.setPersonPosition(emailersTable
                    .getSingleSelected()
                    .getToEmail()
                    .getPersonPosition());
        }

        event.setRemovedFromReserve(false);
        event.setInProcess(true);
        return event;
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