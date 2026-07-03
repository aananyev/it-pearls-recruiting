package com.company.hunttech.web.screens.applicationsetup;

import com.company.hunttech.core.ApplicationSetupService;
import com.company.hunttech.core.TelegramBotService;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.ApplicationSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import javax.inject.Inject;

@UiController("hunttech_ApplicationSetup.edit")
@UiDescriptor("application-setup-edit.xml")
@EditedEntityContainer("applicationSetupDc")
@LoadDataBeforeShow
public class ApplicationSetupEdit extends StandardEditor<ApplicationSetup> {
    @Inject
    private Image applicationDefaultLogoFileImage;
    @Inject
    private Image applicationLogoFileImage;
    @Inject
    private ApplicationSetupService applicationSetupService;
    private Boolean flag = false;
    @Inject
    private CheckBox activeSetupField;
    @Inject
    private Image applicationIconFileImage;
    @Inject
    private Image applicationDefaultIconFileImage;
    @Inject
    private FileUploadField applicationLogoField;
    @Inject
    private FileUploadField applicationIconField;
    @Inject
    private Button telegramBotRestartButton;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private TelegramBotService telegramBotService;
    private Logger logger = LoggerFactory.getLogger(ApplicationSetupEdit.class);

    @Subscribe("applicationLogoField")
    public void onApplicationLogoFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            applicationLogoFileImage.setVisible(true);
            applicationDefaultLogoFileImage.setVisible(false);

            FileDescriptorResource fileDescriptorResource =
                    applicationLogoFileImage.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(
                                    applicationLogoField.getFileDescriptor());

            applicationLogoFileImage.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void setApplicationLogoImage() {
        if (getEditedEntity().getApplicationLogo() == null) {
            applicationDefaultLogoFileImage.setVisible(true);
            applicationLogoFileImage.setVisible(false);
        } else {
            applicationDefaultLogoFileImage.setVisible(false);
            applicationLogoFileImage.setVisible(true);
        }
    }

    @Subscribe("applicationIconField")
    public void onApplicationIconFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            applicationIconFileImage.setVisible(true);
            applicationDefaultIconFileImage.setVisible(false);

            FileDescriptorResource fileDescriptorResource =
                    applicationIconFileImage.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(
                                    applicationIconField.getFileDescriptor());

            applicationIconFileImage.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void setApplicationIconImage() {
        if (getEditedEntity().getApplicationIcon() == null) {
            applicationDefaultIconFileImage.setVisible(true);
            applicationIconFileImage.setVisible(false);
        } else {
            applicationDefaultIconFileImage.setVisible(false);
            applicationIconFileImage.setVisible(true);
        }
    }

    @Subscribe("activeSetupField")
    public void onActiveSetupFieldValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        flag = true;
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (flag && activeSetupField.getValue())
            applicationSetupService.clearActiveApplicationSetup(getEditedEntity());
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setApplicationIconImage();
        setApplicationLogoImage();
        setTelegramBotRestartButton();
    }

    private void setTelegramBotRestartButton() {
        if (telegramBotService.isBotStarted()) {
            telegramBotRestartButton.setCaption(messageBundle.getMessage("msgTelegramBotStopButton"));
        } else {
//            if (telegramBotService.restoreTelegramBotApi() != null) {
                telegramBotRestartButton.setCaption(messageBundle.getMessage("msgTelegramBotStartButton"));
//            }
        }
    }

    public void telegramBotRestartButtonInvoke() {
        telegramBotService.telegramBotRestart();
        setTelegramBotRestartButton();
    }
}