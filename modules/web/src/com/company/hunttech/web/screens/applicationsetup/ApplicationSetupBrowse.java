package com.company.hunttech.web.screens.applicationsetup;

import com.company.hunttech.core.ApplicationSetupService;
import com.company.hunttech.core.TelegramBotService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.export.FileDataProvider;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.ApplicationSetup;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.web.App;

import javax.inject.Inject;
import java.util.function.Supplier;

@UiController("hunttech_ApplicationSetup.browse")
@UiDescriptor("application-setup-browse.xml")
@LookupComponent("applicationSetupsTable")
@LoadDataBeforeShow
public class ApplicationSetupBrowse extends StandardLookup<ApplicationSetup> {
    @Inject
    private CollectionLoader<ApplicationSetup> applicationSetupsDl;

    private Boolean flag = false;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private GroupTable<ApplicationSetup> applicationSetupsTable;
    @Inject
    private Filter filter;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private TelegramBotService telegramBotService;
    @Inject
    private Notifications notifications;
    @Inject
    private BackgroundWorker backgroundWorker;

    ApplicationSetup applicationSetup = null;
    @Inject
    private UiComponents uiComponents;

    @Subscribe(id = "applicationSetupsDc", target = Target.DATA_CONTAINER)
    public void onApplicationSetupsDcItemChange(InstanceContainer.ItemChangeEvent<ApplicationSetup> event) {
        if (flag) {
            flag = false;
            applicationSetupsDl.load();
            applicationSetupsTable.repaint();
        }
    }
    public void editActionHandler() {
        applicationSetup = applicationSetupsTable.getSingleSelected();
        screenBuilders.editor(applicationSetupsTable)
                .withScreenClass(ApplicationSetupEdit.class)
                .editEntity(applicationSetup)
                .build()
                .show();

        applicationSetupsDl.load();
        applicationSetupsTable.scrollTo(applicationSetup);
        applicationSetupsTable.setSelected(applicationSetup);

        flag = true;
    }

    public void createActionHandler() {
        screenBuilders.editor(applicationSetupsTable)
                .newEntity()
                .build()
                .show();
        if (applicationSetup != null) {
            applicationSetupsTable.scrollTo(applicationSetup);
            applicationSetupsTable.setSelected(applicationSetup);
        }

        flag = true;
    }

    private Component retColumnGeneratorImage(FileDescriptor fileDescriptor) {
        HBoxLayout hBoxLayout = uiComponents.create(HBoxLayout.class);
        hBoxLayout.setWidthFull();
        hBoxLayout.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setWidth("50px");
        image.setHeight("50px");
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (fileDescriptor != null) {
            image.setSource(StreamResource.class)
                    .setStreamSupplier(() -> new FileDataProvider(fileDescriptor).provide());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        hBoxLayout.add(image);
        return hBoxLayout;

    }
    public Component applicationLogoGenerator(ApplicationSetup entity) {
        return retColumnGeneratorImage(entity.getApplicationLogo());
    }

    public Component applicationIconGenerator(ApplicationSetup entity) {
        return retColumnGeneratorImage(entity.getApplicationIcon());
    }

    public Component applicationSetupActionsGenerator(ApplicationSetup entity) {
        HBoxLayout actionsBox = uiComponents.create(HBoxLayout.class);
        actionsBox.setWidthFull();
        actionsBox.setHeightFull();

        PopupButton actionMenuButton = uiComponents.create(PopupButton.class);
        actionMenuButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        actionMenuButton.setWidthAuto();
        actionMenuButton.setHeightAuto();
        actionMenuButton.setIconFromSet(CubaIcon.BARS);
        actionMenuButton.setShowActionIcons(true);

        // ОБНОВЛЕНИЕ: возвращён пункт меню строки "Запуск телеграм-бота" со статусом выполнения.
        actionMenuButton.addAction(new BaseAction("telegramBotStartAction")
                .withCaption(messageBundle.getMessage("msgTelegramBotStartButton"))
                .withHandler(actionPerformedEvent -> {
                    applicationSetupsTable.setSelected(entity);
                    runTelegramOperationInBackground(
                            messageBundle.getMessage("msgTelegramBotStartButton"),
                            telegramBotService::telegramBotStart
                    );
                }));
        // ОБНОВЛЕНИЕ: возвращён пункт меню "Перезапуск телеграм-бота" со статусом выполнения.
        actionMenuButton.addAction(new BaseAction("telegramBotRestartAction")
                .withCaption(messageBundle.getMessage("msgTelegramBotRestartButton"))
                .withHandler(actionPerformedEvent -> {
                    applicationSetupsTable.setSelected(entity);
                    runTelegramOperationInBackground(
                            messageBundle.getMessage("msgTelegramBotRestartButton"),
                            telegramBotService::telegramBotRestart
                    );
                }));
        // ОБНОВЛЕНИЕ: возвращён пункт меню "Остановка телеграм-бота" со статусом выполнения.
        actionMenuButton.addAction(new BaseAction("telegramBotStopAction")
                .withCaption(messageBundle.getMessage("msgTelegramBotStopButton"))
                .withHandler(actionPerformedEvent -> {
                    applicationSetupsTable.setSelected(entity);
                    runTelegramOperationInBackground(
                            messageBundle.getMessage("msgTelegramBotStopButton"),
                            telegramBotService::telegramBotStop
                    );
                }));

        actionsBox.add(actionMenuButton);
        return actionsBox;
    }

    private void runTelegramOperationInBackground(String operationCaption, Supplier<String> telegramOperation) {
        // ОБНОВЛЕНИЕ: Telegram-команда уходит в CUBA BackgroundWorker, чтобы запуск/остановка long polling не блокировали экран.
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption(messageBundle.getMessage("msgTelegramBotStatusCaption"))
                .withDescription(operationCaption + ": " + messageBundle.getMessage("msgTelegramBotTaskStarted"))
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .show();

        BackgroundTask<Integer, String> task = new BackgroundTask<Integer, String>(60, this) {
            @Override
            public String run(TaskLifeCycle<Integer> taskLifeCycle) {
                // ОБНОВЛЕНИЕ: сервис Telegram выполняется в фоне и возвращает готовый статус для уведомления пользователя.
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
        // ОБНОВЛЕНИЕ: CUBA Notifications показывает итоговый статус фоновой Telegram-операции пользователю.
        applicationSetupsDl.load();
        applicationSetupsTable.repaint();
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption(messageBundle.getMessage("msgTelegramBotStatusCaption"))
                .withDescription(status)
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .show();
    }
}
