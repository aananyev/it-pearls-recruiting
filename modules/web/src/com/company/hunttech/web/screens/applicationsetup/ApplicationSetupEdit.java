package com.company.hunttech.web.screens.applicationsetup;

import com.company.hunttech.core.ApplicationSetupService;
import com.company.hunttech.core.TelegramBotService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.ApplicationSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import javax.inject.Inject;
import java.util.function.Supplier;

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
    @Inject
    private Notifications notifications;
    @Inject
    private BackgroundWorker backgroundWorker;
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
        // ОБНОВЛЕНИЕ: кнопка запускает Telegram-операцию в фоне и сразу сообщает пользователю о старте задачи.
        boolean stopBot = telegramBotService.isBotStarted();
        String operationCaption = stopBot
                ? messageBundle.getMessage("msgTelegramBotStopButton")
                : messageBundle.getMessage("msgTelegramBotStartButton");
        runTelegramOperationInBackground(
                operationCaption,
                () -> stopBot ? telegramBotService.telegramBotStop() : telegramBotService.telegramBotStart()
        );
    }

    private void runTelegramOperationInBackground(String operationCaption, Supplier<String> telegramOperation) {
        // ОБНОВЛЕНИЕ: CUBA BackgroundWorker выносит запуск/остановку Telegram long polling из UI-потока редактора.
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption(messageBundle.getMessage("msgTelegramBotStatusCaption"))
                .withDescription(operationCaption + ": " + messageBundle.getMessage("msgTelegramBotTaskStarted"))
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .show();

        BackgroundTask<Integer, String> task = new BackgroundTask<Integer, String>(60, this) {
            @Override
            public String run(TaskLifeCycle<Integer> taskLifeCycle) {
                // ОБНОВЛЕНИЕ: сервис Telegram выполняется в фоне и возвращает текстовый результат для CUBA Notifications.
                return telegramOperation.get();
            }

            @Override
            public void done(String status) {
                showTelegramOperationStatus(status);
            }

            @Override
            public boolean handleException(Exception exception) {
                showTelegramOperationStatus(messageBundle.formatMessage("msgTelegramBotTaskFailed", exception.getMessage()));
                return true;
            }
        };

        BackgroundTaskHandler taskHandler = backgroundWorker.handle(task);
        taskHandler.execute();
    }

    private void showTelegramOperationStatus(String status) {
        // ОБНОВЛЕНИЕ: итог фоновой операции показывается штатным сервисом CUBA Notifications.
        setTelegramBotRestartButton();
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption(messageBundle.getMessage("msgTelegramBotStatusCaption"))
                .withDescription(status)
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .show();
    }
}
