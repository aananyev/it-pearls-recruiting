package com.company.itpearls.web.screens.recrutiestasks;

import com.company.itpearls.core.OpenPositionService;
import com.company.itpearls.core.TimeAndDateService;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.service.RecrutiesTasksService;
import com.company.itpearls.web.StandartRoles;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import javax.inject.Inject;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@UiController("itpearls_RecrutiesTasks.edit")
@UiDescriptor("recruties-tasks-edit.xml")
@EditedEntityContainer("recrutiesTasksDc")
@LoadDataBeforeShow
public class RecrutiesTasksEdit extends StandardEditor<RecrutiesTasks> {
    @Inject
    private DateField<Date> startDateField;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private LookupPickerField<ExtUser> recrutiesTasksFieldUser;
    @Inject
    private UserSession userSession;
    @Inject
    private EmailService emailService;
    @Inject
    private InstanceContainer<RecrutiesTasks> recrutiesTasksDc;
    @Inject
    private DateField<Date> endDateField;
    @Inject
    private LookupPickerField<OpenPosition> openPositionField;
    @Inject
    private CheckBox recrutiesTasksSubscribeCheckBox;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private OpenPositionService openPositionService;

    private OpenPosition openPosition = null;
    private Boolean fromOpenPosition = false;
    private final String ROLE_RESEARCHER = StandartRoles.RESEARCHER;
    private Boolean deleteTwiceEvent = true;
    @Inject
    private RecrutiesTasksService recrutiesTasksService;
    @Inject
    private TimeAndDateService timeAndDateService;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setClosed(false);
        }

        if (openPosition != null) {
            openPositionField.setValue(openPosition);
        }

        startDateField.setValue(new Date());

        // если роль - ресерчер, то автоматически вставить себя
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        // установить поле рекрутера
        if (s.contains(ROLE_RESEARCHER) || !fromOpenPosition) {
            recrutiesTasksFieldUser.setValue((ExtUser) userSession.getUser());

            recrutiesTasksFieldUser.setEnabled(!s.contains(ROLE_RESEARCHER));
            // поставить следующий понедельник
            LocalDate currentDate = LocalDate.now();
            LocalDate nextMonday = currentDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));

            Instant instant = nextMonday
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            Date legacyDate = Date.from(instant);

            endDateField.setValue(legacyDate);
        }

        setClosedIsNewEntity();
    }

    private void setClosedIsNewEntity() {
        if( PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setClosed(
                    getEditedEntity().getClosed() == null ? false : getEditedEntity().getClosed());
        }
    }

    @Subscribe("windowCommitAndCloseButton")
    public void onOkBtnClick(Button.ClickEvent event) {

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        Date startDate = timeAndDateService.setStartDay(startDateField.getValue());
        Date endDate = timeAndDateService.setEndDay(endDateField.getValue());

        startDateField.setValue(startDate);
        endDateField.setValue(endDate);

        if (deleteTwiceEvent) {
            if (recrutiesTasksService.isSubscribed(openPositionField.getValue(), recrutiesTasksFieldUser.getValue(), startDate, endDate)) {
                openPositionService.setOpenPositionNewsAutomatedMessage(openPositionField.getValue(),
                        recrutiesTasksFieldUser.getValue().getName()
                                + " подписался на вакансию c "
                                + sdf.format(startDateField.getValue())
                                + " по "
                                + sdf.format(endDateField.getValue()),
                        "",
                        new Date(),
                        null,
                        recrutiesTasksFieldUser.getValue(),
                        true);
                deleteTwiceEvent = false;
            } else {
                dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                        .withType(Dialogs.MessageType.WARNING)
                        .withContentMode(ContentMode.HTML)
                        .withMessage("Выбранный Вами интервал подписики на вакансию '"
                                + openPositionField.getValue().getVacansyName()
                                + "' в интервале дат с "
                                + sdf.format(startDateField.getValue())
                                + "' по "
                                + sdf.format(endDateField.getValue())
                                + " уже совпадает с одной из ваших подписок на эту вакансию. \n\nХотите продолжить ?")
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                    close(StandardOutcome.COMMIT);
                                }),
                                new DialogAction(DialogAction.Type.NO, Action.Status.NORMAL).withHandler(g -> {
                                }))
                        .withCaption("ВНИМАНИЕ")
                        .show();
            }
        }
    }

    @Subscribe("windowCommitAndCloseButton")
    public void onWindowCommitAndCloseButtonClick(Button.ClickEvent event) {
        if (!(openPositionField.getValue().getInternalProject()
                && getRoleService.isUserRoles(userSession.getUser(), ROLE_RESEARCHER))) {
            getEditedEntity().setRecrutierName(userSession.getUser().getName());

            // если были изменения, то послать оповещение по почте
            sendMessage();

            // если не установлен флаг подписки, то установить его в false
            if (recrutiesTasksSubscribeCheckBox.getValue() == null) {
                dialogs.createOptionDialog()
                        .withCaption("Warning")
                        .withMessage("Подписатся на изменение вакансии?")
                        .withActions(
                                new DialogAction(DialogAction.Type.YES,
                                        Action.Status.PRIMARY).withHandler(e -> {
                                    this.recrutiesTasksSubscribeCheckBox.setValue(true);
                                }),
                                new DialogAction(DialogAction.Type.NO).withHandler(f -> {
                                    this.recrutiesTasksSubscribeCheckBox.setValue(false);
                                })
                        )
                        .show();
            }
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("ОШИБКА")
                    .withDescription("Для подписки на эксклюзивную вакансию обратитесь к Team Lead")
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    private static final DateFormatSymbols ruDateFormatSymbols = new DateFormatSymbols() {
        @Override
        public String[] getMonths() {
            return new String[]{"января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        }
    };

    Boolean notificationFlag = true;

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if (getEditedEntity() != null) {
            if (notificationFlag) {
                StringBuilder sb = new StringBuilder();

                sb.append(recrutiesTasksFieldUser.getValue().getName())
                                .append(" подписан на вакансию: ")
                                        .append(getEditedEntity().getOpenPosition().getVacansyName());

                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Подписка")
                        .withDescription(sb.toString())
                        .show();

                openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity().getOpenPosition(),
                        "Подписка на вакансию",
                        sb.toString(),
                        new Date(),
                        (ExtUser) userSession.getUser());

                notificationFlag = false;
            }
        }
    }

    public void sendMessage() {
        String email = recrutiesTasksDc.getItem().getReacrutier().getEmail();

        EmailInfo emailInfo;

        if (email != null) {
            if (!email.isEmpty()) {
                emailInfo = new EmailInfo(email,
                        "Вы подписаны на вакансию " + openPositionField.getValue().getVacansyName(),
                        null, "com/company/itpearls/templates/subscribe_to_position.html",
                        Collections.singletonMap("RecrutierTask", getEditedEntity()));

                emailInfo.setBodyContentType("text/html; charset=UTF-8");

                emailService.sendEmailAsync(emailInfo);
            }
        } else {
            notifications.create()
                    .withCaption("У Вас не зарегистрирован в профиле адрес электронной почты!")
                    .withDescription("Потдписка невозможна!")
                    .withPosition(Notifications.Position.MIDDLE_CENTER)
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    public void setOpenPosition(OpenPosition op) {
        this.openPosition = op;
        fromOpenPosition = true;
    }
}