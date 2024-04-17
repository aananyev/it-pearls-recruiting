package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SubscribeCommand extends OperationCommand {
    private static Logger logger = LoggerFactory.getLogger(SettingsCommand.class);

    public SubscribeCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        try {
            List<RecrutiesTasks> recrutiesTasks = SubscribeCommand.getRecrutiesTasks();

            int counter = 1;

            sendAnswer(absSender, chat.getId(),
                    this.getCommandIdentifier(),
                    Utils.getUserName(user),
                    new StringBuilder()
                            .append(Utils.getBotName())
                            .append("\n")
                            .append("<b>ПОДПИСЧИКОВ НА ВАКАНСИИ</b>\n")
                            .append("ВСЕГО ПОДПИСОК НА ВАКАНСИИ: <b>")
                            .append(recrutiesTasks.size())
                            .append("</b>\n\n")
                            .toString());

            for (RecrutiesTasks recrutiesTask : recrutiesTasks) {
                StringBuilder sb = new StringBuilder();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

                sb.append(counter++)
                        .append(". ")
                        .append(recrutiesTask.getOpenPosition().getVacansyName())
                        .append("\nC <b>")
                        .append(simpleDateFormat.format(recrutiesTask.getStartDate()))
                        .append("</b> по <b>")
                        .append(simpleDateFormat.format(recrutiesTask.getEndDate()))
                        .append("\n</b>\nРекрутер: <b>")
                        .append(recrutiesTask.getReacrutier().getName())
                        .append("</b>\nСвязаться: ")
                        .append(recrutiesTask.getReacrutier().getEmail() != null ? recrutiesTask.getReacrutier().getEmail() : "")
                        .append("");

                if (recrutiesTask.getReacrutier().getEmail() != null && recrutiesTask.getReacrutier().getTelegram() != null) {
                    sb.append(", ");
                }

                sb.append(recrutiesTask.getReacrutier().getTelegram() != null ? recrutiesTask.getReacrutier().getTelegram() : "");

                sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), Utils.getUserName(user), sb.toString());
            }

        } catch (NullPointerException e) {
            logger.debug("ОШИБКА - не загрузились список подписок");
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), Utils.getUserName(user),
                    new StringBuilder()
                            .append(Utils.getBotName())
                            .append("\n")
                            .append("Нет активных подписок на вакансии")
                            .toString());
        }

    }

    public static List<RecrutiesTasks> getMyRecrutiesTasks(ExtUser user) {
        return getRecrutiesTasks(user);
    }

    public static List<RecrutiesTasks> isOpenPositionSubscribers(String openPositionKey, String openPositionCallBack) {
        String queryStr = "select e from itpearls_RecrutiesTasks e where e.openPosition.id = :openPositionId and :currentDate between e.startDate and e.endDate";
        List<RecrutiesTasks> recrutiesTasks = new ArrayList<>();
        String openPositionId = openPositionCallBack.substring(openPositionKey.length() + 1);

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.addView(new View(RecrutiesTasks.class)
                        .addProperty("reacrutier", new View(ExtUser.class)
                                .addProperty("name")
                                .addProperty("telegram")
                                .addProperty("email"))
                        .addProperty("openPosition", new View(OpenPosition.class)
                                .addProperty("id")
                                .addProperty("vacansyName")));
                query.setParameter("currentDate", new Date());
                query.setParameter("openPositionId", UUID.fromString(openPositionId));

                recrutiesTasks = (List<RecrutiesTasks>) query.getResultList();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            return recrutiesTasks;
        }
    }

    public static boolean isRecrutiesTaskValide(RecrutiesTasks recrutiesTasks, User user) {
        List<RecrutiesTasks> retObj = null;
        String QUERY_RECTUTIERS_TASKS_IS = "select e from itpearls_RecrutiesTasks e " +
                "where (:currentDate between e.startDate and e.endDate) " +
                "and e.reacrutier = :reacrutier " +
                "and e.openPosition = :openPosition";

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(QUERY_RECTUTIERS_TASKS_IS)
                        .setParameter("openPosition", recrutiesTasks.getOpenPosition())
                        .setParameter("reacrutier", recrutiesTasks.getReacrutier())
                        .setParameter("currentDate", new Date());

                retObj = (List<RecrutiesTasks>) query.getResultList();
                tx.commit();
            }

        } finally {
            if (retObj != null) {
                return retObj.size() > 0 ? true : false;
            } else {
                return false;
            }
        }
    }

    public static List<RecrutiesTasks> getRecrutiesTasks(ExtUser user) {
        List<RecrutiesTasks> recrutiesTasksUser = new ArrayList<>();
        String QUERY_GETRECRUTIES_TASK_USER = "select e from itpearls_RecrutiesTasks e " +
                "where (:date between e.startDate and e.endDate) and e.reacrutier = :reacrutier";

        Persistence persistence = AppBeans.get(Persistence.class);
        try (Transaction tx = persistence.createTransaction()) {
            View view = new View(RecrutiesTasks.class)
                    .addProperty("startDate")
                    .addProperty("endDate")
                    .addProperty("recrutierName")
                    .addProperty("reacrutier",
                            new View(ExtUser.class)
                                    .addProperty("name"))
                    .addProperty("openPosition",
                            new View(OpenPosition.class)
                                    .addProperty("vacansyName"));

            // get EntityManager for the current transaction
            EntityManager em = persistence.getEntityManager();
            Query query = em.createQuery(QUERY_GETRECRUTIES_TASK_USER)
                    .setParameter("date", new Date())
                    .setParameter("reacrutier", user);
            query.addView(view);

            recrutiesTasksUser = query.getResultList();
            tx.commit();
        } catch (Exception e) {
            logger.error("Ошибка получения списка заданий рекрутеру RecriturTasks");
        } finally {
            return recrutiesTasksUser;
        }
    }

    public static List<RecrutiesTasks> getRecrutiesTasks() {
        return Utils.queryListResult("select e from itpearls_RecrutiesTasks e " +
                        "where :date between e.startDate and e.endDate",
                new View(RecrutiesTasks.class)
                        .addProperty("reacrutier", new View(ExtUser.class)
                                .addProperty("name")
                                .addProperty("email")
                                .addProperty("telegram"))
                        .addProperty("startDate")
                        .addProperty("endDate")
                        .addProperty("recrutierName")
                        .addProperty("openPosition",
                                new View(OpenPosition.class)
                                        .addProperty("vacansyName")),
                "date", new Date());
    }

    public static InlineKeyboardMarkup getSendSubscribersKeyboard(String openPositionId) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton viewDetailsButton = new InlineKeyboardButton();
        viewDetailsButton.setText("Send CV");
        StringBuilder callBackDataSB = new StringBuilder()
                .append(CallbackData.SEND_CV_TO_COORDINATOR)
                .append(CallbackData.CALLBACK_SEPARATOR)
                .append(openPositionId);

        viewDetailsButton.setCallbackData(callBackDataSB.toString());

        rowInline.add(viewDetailsButton);

        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;
    }
}
