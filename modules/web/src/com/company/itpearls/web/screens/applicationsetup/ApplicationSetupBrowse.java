package com.company.itpearls.web.screens.applicationsetup;

import com.company.itpearls.core.TelegramBotService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.export.FileDataProvider;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.ApplicationSetup;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;

@UiController("itpearls_ApplicationSetup.browse")
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

    ApplicationSetup applicationSetup = null;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private TelegramBotService telegramBotService;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;

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
                .withAfterCloseListener(applicationSetupEditAfterScreenCloseEvent -> {
                    applicationSetupsDl.load();
                    applicationSetupsTable.scrollTo(applicationSetup);
                    applicationSetupsTable.setSelected(applicationSetup);

                    flag = true;
                })
                .build()
                .show();
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

    public Component telegramBotStartedColumnGenerator(ApplicationSetup entity) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        ApplicationSetup as = telegramBotService.restoreApplicationSetup();

        if (telegramBotService.isBotStarted() && entity.equals(telegramBotService.restoreApplicationSetup())) {
            retLabel.setIcon(CubaIcon.OK.source());
            retLabel.setStyleName("h1-green");
        } else {
            retLabel.setIcon(CubaIcon.CANCEL.source());
            retLabel.setStyleName("h1-red");
        }

        retHBox.add(retLabel);

        return retHBox;
    }

    public Component telegramActionColumnGenerator(ApplicationSetup entity) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setHeightFull();
        retHbox.setWidthFull();

        PopupButton popupButton = uiComponents.create(PopupButton.class);
        popupButton.setWidthAuto();
        popupButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        popupButton.setIcon(CubaIcon.BARS.source());

        popupButton.addAction(new BaseAction("restartTelegramBotAction")
                .withCaption(messageBundle.getMessage("msgTelegramBotRestartButton")).withHandler(e -> {
                    if (telegramBotService.isBotStarted()) {
                        telegramBotService.telegramBotStop();
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withHideDelayMs(5000)
                                .withCaption(messageBundle.getMessage("msgTelegramBotStopButton")
                                        + ": "
                                        + telegramBotService.getApplicationSetup().getTelegramBotName())
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withHideDelayMs(5000)
                                .withCaption(messageBundle.getMessage("msgTelegramBotNotStartedOnlyStart")
                                        + ": "
                                        + telegramBotService.getApplicationSetup().getTelegramBotName())
                                .show();
                    }

                    telegramBotService.telegramBotStart(entity);

                    entity.setTelegramBotStarted(true);
                    dataManager.commit(entity);

                    applicationSetupsDl.load();
                    applicationSetupsTable.repaint();
                    applicationSetupsTable.setSelected(entity);

                    if (telegramBotService.isBotStarted()) {
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withHideDelayMs(15000)
                                .withCaption(messageBundle.getMessage("msgTelegramBotRestartButton")
                                        + ": "
                                        + telegramBotService.getApplicationSetup().getTelegramBotName())
                                .show();
                    }
                }));

        popupButton.addAction(new BaseAction("stopTelegramBotAction")
                .withCaption(messageBundle.getMessage("msgTelegramBotStopButton"))
                .withHandler(e -> {
                    if (telegramBotService.isBotStarted()) {
                        telegramBotService.telegramBotStop();

                        entity.setTelegramBotStarted(false);
                        dataManager.commit(entity);

                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption(messageBundle.getMessage("msgTelegramBotNotStopped"))
                                .withHideDelayMs(15000)
                                .show();
                    } else {
                        notifications.create(Notifications.NotificationType.TRAY)
                                .withCaption(messageBundle.getMessage("msgTelegramBotNotStarted"))
                                .withHideDelayMs(15000)
                                .show();
                    }

                    applicationSetupsDl.load();
                    applicationSetupsTable.repaint();
                    applicationSetupsTable.setSelected(entity);
                }));

        retHbox.add(popupButton);

        return retHbox;
    }
}