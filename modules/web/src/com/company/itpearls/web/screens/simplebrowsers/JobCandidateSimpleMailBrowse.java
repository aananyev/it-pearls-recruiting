package com.company.itpearls.web.screens.simplebrowsers;

import com.company.itpearls.core.InteractionService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.internalemailertemplate.InternalEmailerTemplateEdit;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_JobCandidateSimpleMail.browse")
@UiDescriptor("job-candidate-simple-mail-browse.xml")
public class JobCandidateSimpleMailBrowse extends JobCandidateSimpleBrowse {
    @Inject
    private DataGrid<JobCandidate> iteractionListsTable;
    @Inject
    private Button sendEmailTemplateButton;
    @Inject
    private Dialogs dialogs;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private CollectionLoader<InternalEmailTemplate> internalEmailTemplateDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionContainer<InternalEmailTemplate> internalEmailTemplateDc;
    @Inject
    private Screens screens;
    @Inject
    private Metadata metadata;
    @Inject
    private InteractionService interactionService;

    @Subscribe
    public void onInit(InitEvent event) {
        iteractionListsTable
                .addSelectionListener(customerSelectionEvent ->
                        sendEmailTemplateButton.setEnabled(iteractionListsTable.getSelected().size() != 0));
    }

    public void sendEmailTemplateButtonInvoke() {
        dialogs.createInputDialog(this)
                .withCaption(messageBundle.getMessage("msgSelectTemplate"))
                .withParameters(
                        InputParameter.parameter("emailTemplate")
                                .withField(() -> {
                                    internalEmailTemplateDl.setParameter("author", userSession.getUser());
                                    internalEmailTemplateDl.setParameter("positionType", getOpenPosition().getPositionType());
                                    internalEmailTemplateDl.load();

                                    LookupPickerField<InternalEmailTemplate> internalEmailTemplatePickerField = uiComponents.create(LookupPickerField.class);
                                    internalEmailTemplatePickerField.setCaption(messageBundle.getMessage("msgTemplate"));
                                    internalEmailTemplatePickerField.setWidthFull();
                                    internalEmailTemplatePickerField.setHeightAuto();
                                    internalEmailTemplatePickerField.setRequired(true);
                                    internalEmailTemplatePickerField.setIcon(CubaIcon.ENVELOPE_SQUARE.source());
                                    internalEmailTemplatePickerField.setOptionsList(internalEmailTemplateDc.getItems());
                                    internalEmailTemplatePickerField.setOptionImageProvider(this::emailTemplateImageProvider);

                                    return internalEmailTemplatePickerField;
                                }))
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        InternalEmailTemplate internalEmailTemplate = closeEvent.getValue("emailTemplate");
                        sendSelectedJobCandidatesWithMessages(internalEmailTemplate);
                    }
                })
                .show();
    }

    private void sendSelectedJobCandidatesWithMessages(InternalEmailTemplate internalEmailTemplate) {
        for (JobCandidate jobCandidate : iteractionListsTable.getSelected()) {
            InternalEmailerTemplate internalEmailerTemplate = metadata.create(InternalEmailerTemplate.class);
            internalEmailerTemplate.setFromEmail((ExtUser) userSession.getUser());

            InternalEmailerTemplateEdit emailerTemplateEdit = screens.create(InternalEmailerTemplateEdit.class);
            emailerTemplateEdit.setEntityToEdit(internalEmailerTemplate);
            emailerTemplateEdit.setEmailTemplate(internalEmailTemplate);
            emailerTemplateEdit.setJobCandidate(jobCandidate);
            emailerTemplateEdit.show();
        }
    }

    protected Resource emailTemplateImageProvider(InternalEmailTemplate internalEmailTemplate) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (internalEmailTemplate.getTemplateOpenPosition() != null) {
            if (internalEmailTemplate.getTemplateOpenPosition().getProjectName() != null) {
                if (internalEmailTemplate.getTemplateOpenPosition().getProjectName().getProjectLogo() != null) {
                    return retImage.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(
                                    internalEmailTemplate
                                            .getTemplateOpenPosition()
                                            .getProjectName()
                                            .getProjectLogo());
                } else {
                    return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
                }
            } else {
                return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
            }
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    @Install(to = "iteractionListsTable.mailColumn", subject = "columnGenerator")
    private Component iteractionListsTableMailColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        LinkButton retLabel = uiComponents.create(LinkButton.class);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (event.getItem().getEmail() != null) {
            if (!event.getItem().getEmail().equals("")) {
                retLabel.setIcon(CubaIcon.ENVELOPE_O.source());
                if (interactionService.getLastIteraction(event.getItem()).getIteractionType() != null) {
                    if (interactionService.getLastIteraction(event.getItem()).getIteractionType().getSignEmailSend() != null) {
                        if (interactionService.getLastIteraction(event.getItem()).getIteractionType().getSignEmailSend()) {
                            retLabel.addStyleName("label_button_gray");
                        } else {
                            retLabel.addStyleName("label_button_green");
                        }
                    } else {
                        retLabel.addStyleName("label_button_green");
                    }
                } else {
                    retLabel.addStyleName("label_button_green");
                }

                retLabel.setDescription(event.getItem().getEmail());
                retLabel.addClickListener(event1 -> {
                    InternalEmailerTemplate internalEmailerTemplate = metadata.create(InternalEmailerTemplate.class);
                    internalEmailerTemplate.setFromEmail((ExtUser) userSession.getUser());

                    InternalEmailerTemplateEdit emailerTemplateEdit = screens.create(InternalEmailerTemplateEdit.class);
                    emailerTemplateEdit.setEntityToEdit(internalEmailerTemplate);
                    emailerTemplateEdit.setJobCandidate(event.getItem());
                    emailerTemplateEdit.show();
                });
            } else {
                retLabel.setIcon(CubaIcon.MINUS_CIRCLE.source());
                retLabel.addStyleName("label_button_red");
            }
        }

        retHBox.add(retLabel);

        return retHBox;
    }


}