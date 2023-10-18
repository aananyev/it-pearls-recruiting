package com.company.itpearls.web.screens.internalemailertemplate;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.InternalEmailTemplate;
import com.company.itpearls.entity.InternalEmailer;
import com.company.itpearls.entity.InternalEmailerTemplate;
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

@UiController("itpearls_InternalEmailerTemplate.browse")
@UiDescriptor("internal-emailer-template-browse.xml")
public class InternalEmailerTemplateBrowse extends InternalEmailerBrowse {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private CollectionLoader<InternalEmailer> emailersDl;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UserSession userSession;
    @Inject
    private DataGrid<InternalEmailerTemplate> emailersTable;
    @Inject
    private DataManager dataManager;




    @Install(to = "emailersTable.actionButtonColumn", subject = "columnGenerator")
    private Component emailersTableActionButtonColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setWidthFull();
        retHbox.setHeightFull();

        PopupButton actionButton = uiComponents.create(PopupButton.class);
        actionButton.setIconFromSet(CubaIcon.BARS);
        actionButton.setWidthAuto();
        actionButton.setHeightAuto();
        actionButton.setAlignment(Component.Alignment.MIDDLE_CENTER);

        actionButton.addAction(new BaseAction("replyEmail")
                .withCaption(messageBundle.getMessage("msgReplyEmail"))
                .withHandler(actionPerformedEvent -> resendEmailAction(actionPerformedEvent)));

        retHbox.add(actionButton);

        return retHbox;
    }

    public void setEmailTemplateFilter(InternalEmailTemplate internalEmailTemplate) {
        emailersDl.setParameter("emailTemplate", internalEmailTemplate);
        emailersDl.load();
    }

    public void setCurrentUser() {
        emailersDl.setParameter("user", (ExtUser) userSession.getUser());
        emailersDl.load();
    }

    private void resendEmailAction(Action.ActionPerformedEvent actionPerformedEvent) {
        screenBuilders.editor(InternalEmailer.class, this)
                .withScreenClass(InternalEmailerTemplateEdit.class)
                .newEntity()
                .withInitializer(event -> {
                    event.setToEmail(emailersTable.getSingleSelected().getToEmail());
                    event.setReplyInternalEmailer(emailersTable.getSingleSelected());
                })
                .withAfterCloseListener(afterCloseEvent -> {
                    emailersTable.setSelected(emailersTable.getSingleSelected());
                    emailersTable.repaint();
                })
                .withOpenMode(OpenMode.DIALOG)
                .build()
                .show();
    }

}