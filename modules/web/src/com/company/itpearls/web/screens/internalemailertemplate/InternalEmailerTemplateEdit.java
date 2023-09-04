package com.company.itpearls.web.screens.internalemailertemplate;

import com.company.itpearls.core.EmailGenerationService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.internalemailer.InternalEmailerEdit;
import com.company.itpearls.web.screens.openposition.OpenPositionEdit;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_InternalEmailerTemplate.edit")
@UiDescriptor("internal-emailer-template-edit.xml")
@EditedEntityContainer("internalEmailerTemplateDc")
@LoadDataBeforeShow
public class InternalEmailerTemplateEdit extends InternalEmailerEdit<InternalEmailer> {
    @Inject
    private Dialogs dialogs;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private TextField<String> subjectEmailField;
    @Inject
    private RichTextArea bodyEmailField;
    @Inject
    private EmailGenerationService emailGenerationService;
    @Inject
    private LookupPickerField<InternalEmailTemplate> emailTemplateField;
    @Inject
    private SuggestionPickerField<JobCandidate> toEmailField;
    @Inject
    private UserSession userSession;
    @Inject
    private ScreenBuilders screenBuilders;

    private OpenPosition currentOpenPosition = null;

    @Subscribe("emailTemplateField")
    public void onEmailTemplateFieldValueChange(HasValue.ValueChangeEvent<InternalEmailTemplate> event) {
        if (event.getValue() != null) {
            Boolean flag = false;
            if (bodyEmailField.getValue() != null) {
                if (!bodyEmailField.getValue().equals("")) {
                    createTextMessage();

                    flag = true;
                }
            }

            if (!flag) {
                dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withMessage(messageBundle.getMessage("msgReplaceTextMessage"))
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                        .withHandler(e -> createTextMessage()),
                                new DialogAction(DialogAction.Type.NO))
                        .show();
            }
        } else {
            dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage(messageBundle.getMessage("msgClearAllFields"))
                    .withActions(
                            new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                    .withHandler(e -> clearAllFields()),
                            new DialogAction(DialogAction.Type.NO))
                    .show();
        }
    }

    private void createTextMessage() {
        bodyEmailField.setValue(emailGenerationService
                .preparingMessage(emailTemplateField.getValue().getTemplateText(),
                        toEmailField.getValue(),
                        (ExtUser) userSession.getUser()
                ));

        subjectEmailField.setValue(emailGenerationService
                .preparingMessage(emailTemplateField.getValue().getTemplateSubj(),
                        toEmailField.getValue(),
                        emailTemplateField.getValue().getTemplateOpenPosition(),
                        (ExtUser) userSession.getUser()
                ));
    }

    private void clearAllFields() {
        subjectEmailField.setValue(null);
        bodyEmailField.setValue(null);
    }

    public void selectOpenPositionButton() {
        screenBuilders.lookup(OpenPosition.class, this)
                .withLaunchMode(OpenMode.DIALOG)
                .withSelectHandler(selectHandler -> {
                    OpenPosition openPosition = selectHandler.iterator().next();
                    if (currentOpenPosition == null) {
                        if (openPosition != null) {
                            bodyEmailField.setValue(emailGenerationService
                                    .preparingMessage(bodyEmailField.getValue(), openPosition));

                            subjectEmailField.setValue(emailGenerationService
                                    .preparingMessage(subjectEmailField.getValue(), openPosition));
                        }

                        currentOpenPosition = openPosition;
                    } else {
                        dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                                .withType(Dialogs.MessageType.CONFIRMATION)
                                .withCaption(messageBundle.getMessage("msgWarning"))
                                .withMessage(messageBundle.getMessage("msgRebuildTemplateWithNewOpenPosition"))
                                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                        .withHandler(event -> {
                                            clearAllFields();
                                            createTextMessage();
                                            currentOpenPosition = openPosition;

                                            if (openPosition != null) {
                                                bodyEmailField.setValue(emailGenerationService
                                                        .preparingMessage(bodyEmailField.getValue(), openPosition));

                                                subjectEmailField.setValue(emailGenerationService
                                                        .preparingMessage(subjectEmailField.getValue(), openPosition));
                                            }
                                        }),
                                        new DialogAction((DialogAction.Type.NO)))
                                .show();
                    }
                })
                .build()
                .show();
    }
}