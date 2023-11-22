package com.company.itpearls.web.screens.internalemailertemplate;

import com.company.itpearls.core.EmailGenerationService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.internalemailer.InternalEmailerEdit;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
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
    @Inject
    private Label<String> openPositionLabel;
    @Inject
    private CollectionLoader<InternalEmailTemplate> emailTemplatesDl;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private CheckBox onlyMySubscribeCheckBox;
    @Inject
    private CollectionContainer<InternalEmailTemplate> emailTemplatesDc;
    @Inject
    private Notifications notifications;
    @Inject
    private CheckBox showSharedTemplatesCheckBox;

    private OpenPosition currentOpenPosition = null;
    private InternalEmailTemplate emailTemplate = null;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        setAuthorTemplate();
    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        initEmailTemplateField();
        setOnlyMySubscribeCheckBox();
        setShowSharedTemplatesCheckBox();
        saveEmailTemplate();
    }

    private void saveEmailTemplate() {
        if (emailTemplateField.getValue() != null)
            this.emailTemplate = emailTemplateField.getValue();
    }

    private void setShowSharedTemplatesCheckBox() {
        showSharedTemplatesCheckBox.addValueChangeListener(booleanValueChangeEvent -> {
            if (booleanValueChangeEvent.getValue()) {
                emailTemplatesDl.setParameter("shareTemplate", true);
                emailTemplatesDl.removeParameter("templateAuthor");
            } else {
                emailTemplatesDl.removeParameter("shareTemplate");
                emailTemplatesDl.setParameter("templateAuthor", userSession.getUser());
            }

            emailTemplatesDl.load();
        });
    }

    private void initEmailTemplateField() {
        emailTemplateField.setOptionImageProvider(this::emailTemplateImageProvider);

        emailTemplateField.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (event.getValue().getTemplateOpenPosition() != null) {
                    openPositionLabel.setValue(event.getValue().getTemplateOpenPosition().getVacansyName());
                    openPositionLabel.setDescription(event.getValue().getTemplateOpenPosition().getComment());
                }
            }
        });

        if (getEmailTemplate() != null) {
            emailTemplateField.setValue(getEmailTemplate());
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

    private void setAuthorTemplate() {
        emailTemplatesDl.setParameter("templateAuthor", userSession.getUser());
        emailTemplatesDl.load();
    }

    private Boolean closeScreen = false;

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        closeScreen = true;
    }

    @Subscribe("emailTemplateField")
    public void onEmailTemplateFieldValueChange(HasValue.ValueChangeEvent<InternalEmailTemplate> event) {
        if (event.getValue() != null) {
            if (!closeScreen) {
                reloadTemplate();
            }
        }
    }

    private void reloadTemplate() {

        if (bodyEmailField.getValue() == null) {
            createTextMessage();
        } else {
            if (bodyEmailField.getValue().equals("")) {
                createTextMessage();
            } else {
                dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withMessage(messageBundle.getMessage("msgReplaceTextMessage"))
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                        .withHandler(e -> createTextMessage()),
                                new DialogAction(DialogAction.Type.NO))
                        .show();
            }
        }
    }

    private void createTextMessage(OpenPosition openPosition) {
        bodyEmailField.setValue(emailGenerationService
                .preparingMessage(emailTemplateField.getValue().getTemplateText(),
                        toEmailField.getValue(),
                        (ExtUser) userSession.getUser()
                ));

        subjectEmailField.setValue(emailGenerationService
                .preparingMessage(emailTemplateField.getValue().getTemplateSubj(),
                        toEmailField.getValue(),
                        openPosition,
                        (ExtUser) userSession.getUser()
                ));

        if (openPosition != null) {
            currentOpenPosition = openPosition;
            openPositionLabel.setValue(openPosition.getVacansyName());
            openPositionLabel.setDescription(openPosition.getComment());
            bodyEmailField.setValue(emailGenerationService.preparingMessage(bodyEmailField.getValue(),
                    currentOpenPosition));
        }

        this.emailTemplate = emailTemplateField.getValue();
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

        if (emailTemplateField.getValue().getTemplateOpenPosition() != null) {
            currentOpenPosition = emailTemplateField.getValue().getTemplateOpenPosition();
            openPositionLabel.setValue(emailTemplateField.getValue().getTemplateOpenPosition().getVacansyName());
            openPositionLabel.setDescription(emailTemplateField.getValue().getTemplateOpenPosition().getComment());
            bodyEmailField.setValue(emailGenerationService.preparingMessage(bodyEmailField.getValue(),
                    currentOpenPosition));
        }

        this.emailTemplate = emailTemplateField.getValue();
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

                        openPositionLabel.setValue(currentOpenPosition.getVacansyName());
                        openPositionLabel.setDescription(currentOpenPosition.getComment());
                    } else {
                        dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                                .withType(Dialogs.MessageType.CONFIRMATION)
                                .withCaption(messageBundle.getMessage("msgWarning"))
                                .withMessage(messageBundle.getMessage("msgRebuildTemplateWithNewOpenPosition"))
                                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                                .withHandler(event -> {
                                                    clearAllFields();
                                                    createTextMessage(openPosition);
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

    @Override
    protected IteractionList createIteraction() {
        IteractionList iteractionList = super.createIteraction();

        if (currentOpenPosition != null) {
            iteractionList.setVacancy(currentOpenPosition);
        }

        return iteractionList;
    }


    private void setOnlyMySubscribeCheckBox() {
        onlyMySubscribeCheckBox.setValue(true);
        emailTemplatesDl.setParameter("subscriber", userSession.getUser());
        emailTemplatesDl.load();

        if (emailTemplatesDc.getItems().size() == 0) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgNoSubscribeVacansies"))
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withHideDelayMs(10000)
                    .withType(Notifications.NotificationType.WARNING)
                    .show();
        }

        onlyMySubscribeCheckBox.addValueChangeListener(e -> {
            if (e.getValue()) {
                emailTemplatesDl.setParameter("subscriber", userSession.getUser());
                emailTemplatesDl.load();

                if (emailTemplatesDc.getItems().size() == 0) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption(messageBundle.getMessage("msgWarning"))
                            .withDescription(messageBundle.getMessage("msgNoSubscribeVacansies"))
                            .withPosition(Notifications.Position.BOTTOM_RIGHT)
                            .withHideDelayMs(10000)
                            .withType(Notifications.NotificationType.WARNING)
                            .show();
                }
            } else {
                emailTemplatesDl.removeParameter("subscriber");
                emailTemplatesDl.load();
            }

        });
    }

    public void setToEmailField(JobCandidate jobCandidate) {
        toEmailField.setValue(jobCandidate);
    }

    public InternalEmailerTemplate getInternalEmailerTemplate() {
        return (InternalEmailerTemplate) getEditedEntity();
    }

    @Subscribe("showTemplateClosedVacancyCheckBox")
    public void onShowTemplateClosedVacancyCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            emailTemplatesDl.setParameter("openClose", true);
        } else {
            emailTemplatesDl.removeParameter("openClose");
        }
    }

    public InternalEmailTemplate getEmailTemplate() {
        return emailTemplate;
    }

    public void setEmailTemplate(InternalEmailTemplate emailTemplate) {
        this.emailTemplate = emailTemplate;

        emailTemplateField.setValue(emailTemplate);
    }
}