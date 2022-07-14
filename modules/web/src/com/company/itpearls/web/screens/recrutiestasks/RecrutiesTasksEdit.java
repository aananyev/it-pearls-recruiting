package com.company.itpearls.web.screens.recrutiestasks;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.OpenPositionNews;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.service.GetUserRoleService;
import com.company.itpearls.service.SubscribeDateService;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.config.ConfigStorageCommon;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.security.entity.User;
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
    private LookupPickerField<User> recrutiesTasksFieldUser;
    @Inject
    private UserSession userSession;
    @Inject
    private EmailService emailService;
    @Inject
    private InstanceContainer<RecrutiesTasks> recrutiesTasksDc;
    @Inject
    private DateField<Date> endDateField;

    private OpenPosition openPosition = null;
    private Boolean fromOpenPosition = false;
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
    private SubscribeDateService subscribeDateService;
    @Inject
    private GetRoleService getRoleService;

    private String MANAGER = "Manager";
    @Inject
    private CheckBox allSubscribeCheckBox;
    @Inject
    private ConfigStorageCommon configStorageCommon;
    @Inject
    private GetUserRoleService getUserRoleService;
    @Inject
    private CollectionContainer<User> usersDc;
    @Inject
    private Events events;
    @Inject
    private Metadata metadata;

    private String ROLE_MANAGER = "Manager";
    private String ROLE_RESEARCHER = "Researcher";
    private String startDateSunscribe = "";
    private String endDateSubscribe = "";

    @Subscribe("windowExtendAndCloseButton")
    public void onWindowExtendAndCloseButtonClick(Button.ClickEvent event) {
        getEditedEntity().setEndDate(subscribeDateService.dateOfNextMonday());

        commitChanges();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        String role = "Researcher";

        if (openPosition != null) {
            openPositionField.setValue(openPosition);
        }

        startDateField.setValue(new Date());

        // если роль - ресерчер, то автоматически вставить себя
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        // установить поле рекрутера
        if (s.contains(role) || !fromOpenPosition) {
            recrutiesTasksFieldUser.setValue(userSession.getUser());

            if (s.contains(role))
                recrutiesTasksFieldUser.setEnabled(false);
            else
                recrutiesTasksFieldUser.setEnabled(true);
            // поставить следующий понедельник
            LocalDate currentDate = LocalDate.now();
            LocalDate nextMonday = currentDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));

            Instant instant = nextMonday
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            Date legacyDate = Date.from(instant);

            endDateField.setValue(legacyDate);
        }

        allSubscribeCheckBoxSet();
    }

    private boolean isAllreadySubscribe() {
        String QUERY_GET_SUBSCRIBE = "select e from itpearls_RecrutiesTasks e " +
                "where e.reacrutier = :reacrutier " +
                "and e.openPosition = :openPosition " +
                "and current_date between e.startDate and e.endDate";
        List<RecrutiesTasks> subscribes = dataManager.load(RecrutiesTasks.class)
                .query(QUERY_GET_SUBSCRIBE)
                .parameter("reacrutier", recrutiesTasksFieldUser.getValue())
                .parameter("openPosition", openPositionField.getValue())
                .list();

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        if (subscribes.size() > 0) {
            for (RecrutiesTasks e : subscribes) {
                startDateSunscribe = sdf.format(e.getStartDate());
                endDateSubscribe = sdf.format(e.getEndDate());
            }

            return true;
        } else {
            return false;
        }

    }

    private String GROUP_RESEARCHING_NAME = "Ресерчинг";
    private String GROUP_RECRUTING_NAME = "Рекрутинг";
    private String GROUP_MANAGEMENT_NAME = "Менеджмент";
    private String GROUP_ACCOUNTING_NAME = "Аккаунтинг";

    private void allSubscribeCheckBoxSet() {
        if (userSession.getUser().getGroup().getName().equals(GROUP_ACCOUNTING_NAME) ||
                userSession.getUser().getGroup().getName().equals(GROUP_MANAGEMENT_NAME)) {
            allSubscribeCheckBox.setEnabled(true);

            allSubscribeCheckBox.addValueChangeListener(e -> {
                if (e.getValue() != null) {
                    recrutiesTasksFieldUser.setEnabled(!e.getValue());
                }
            });
        } else {
            allSubscribeCheckBox.setEnabled(false);
        }

        allSubscribeCheckBox.setValue(false);
    }

    @Subscribe
    public void onBeforeCommitChanges1(BeforeCommitChangesEvent event) {
        if (allSubscribeCheckBox.getValue()) {
            //подписать всех

            for (User user : usersDc.getItems()) {
                if (!user.equals(userSession.getUser())
                        && user.getActive()
                        && (user.getGroup().getName().equals(GROUP_RESEARCHING_NAME) ||
                        user.getGroup().getName().equals(GROUP_RECRUTING_NAME))) {
                    CommitContext commitContext = new CommitContext();

                    RecrutiesTasks recrutiesTasks = new RecrutiesTasks();

                    recrutiesTasks.setStartDate(startDateField.getValue());
                    recrutiesTasks.setEndDate(endDateField.getValue());
                    recrutiesTasks.setReacrutier(user);
                    recrutiesTasks.setSubscribe(false);
                    recrutiesTasks.setOpenPosition(openPositionField.getValue());

                    commitContext.addInstanceToCommit(recrutiesTasks);
                    dataManager.commit(commitContext);

                    events.publish(new UiNotificationEvent(this, "Подписан рекрутер "
                            + user.getName()
                            + " на вакансию "
                            + openPositionField.getValue().getVacansyName()));
                }
            }
        }
    }

    Boolean deleteTwiceEvent = true;

    @Subscribe
    public void onAfterCommitChanges1(AfterCommitChangesEvent event) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        if (deleteTwiceEvent) {
            setOpenPositionNewsAutomatedMessage(openPositionField.getValue(),
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
        }

    }


    private void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                                     String subject,
                                                     String comment,
                                                     Date date,
                                                     JobCandidate jobCandidate,
                                                     User user,
                                                     Boolean priority) {

        try {
            OpenPositionNews openPositionNews = metadata.create(OpenPositionNews.class);

            openPositionNews.setOpenPosition(editedEntity);
            openPositionNews.setAuthor(user);
            openPositionNews.setDateNews(date);
            openPositionNews.setSubject(subject);
            openPositionNews.setComment(comment);
            openPositionNews.setCandidates(jobCandidate);
            openPositionNews.setPriorityNews(priority != null ? priority : false);

            CommitContext commitContext = new CommitContext();
            commitContext.addInstanceToCommit(openPositionNews);
            dataManager.commit(commitContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        if (!(openPositionField.getValue().getInternalProject()
                && getRoleService.isUserRoles(userSession.getUser(), ROLE_RESEARCHER))) {
            event.preventWindowClose();
        }
    }

    @Subscribe
    public void onBeforeCommitChanges(AfterCommitChangesEvent event) {
        if (!isAllreadySubscribe()) {
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
        } else {
            dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                    .withMessage("Вы уже подписаны на эту вакансию \nс "
                            + startDateSunscribe
                            + " по "
                            + endDateSubscribe
                            + "\nПродлить подписку на 1 неделю?")
                    .withType(Dialogs.MessageType.WARNING)
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {

                    }), new DialogAction(DialogAction.Type.NO))
                    .show();
        }
    }

    private static DateFormatSymbols ruDateFormatSymbols = new DateFormatSymbols() {

        @Override
        public String[] getMonths() {
            return new String[]{"января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        }

    };

    private Boolean checkSubscribePosition() {
        Integer countSubscrine = dataManager
                .loadValue("select count(e.reacrutier) from itpearls_RecrutiesTasks e " +
                        "where e.reacrutier = :recrutier and " +
                        "e.openPosition = :openPosition and " +
                        ":nowDate between e.startDate and e.endDate", Integer.class)
//                .parameter( "recrutier", recrutiesTasksFieldUser.getValue() )
                .parameter("recrutier", getEditedEntity().getReacrutier())
                .parameter("openPosition", getEditedEntity().getOpenPosition())
//                .parameter( "openPosition", openPositionField.getValue() )
                .parameter("nowDate", new Date())
                .one();

        // если нет соответствия, значит нет еще подписки, значит можно подписаться,
        // если уже есть, то не надо подписываться
        return countSubscrine > 0 ? true : false;
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Подписка")
                .withDescription(recrutiesTasksFieldUser.getValue().getName() +
                        " подписан на позицию: \n" +
                        openPositionField.getValue().getVacansyName())
                .show();

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
/*
    @Subscribe("okButton")
    private void onOkButtonClick(Button.ClickEvent event) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM YYYY",
                ruDateFormatSymbols);

        // проверить, а вдруг вы уже подписаны на эту вакансию?
        if (checkSubscribePosition() && !getRoleService.isUserRoles(userSession.getUser(), MANAGER)) {
            dialogs.createMessageDialog()
                    .withCaption("Внимание")
                    .withModal(true)
                    .withMessage("Вы уже подписаны на вакансию " + openPositionField.getValue().getVacansyName() +
                            "\n c " + dateFormat.format(startDateField.getValue()) +
                            " по " + dateFormat.format(endDateField.getValue()))
                    .show();

            close(WINDOW_DISCARD_AND_CLOSE_ACTION);
        } else {
            closeWithCommit();
        }
    } */
}