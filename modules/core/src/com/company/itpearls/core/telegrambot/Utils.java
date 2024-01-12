package com.company.itpearls.core.telegrambot;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.View;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Utils {

    /**
     * Формирование имени пользователя
     *
     * @param msg сообщение
     */
    public static String getUserName(Message msg) {
        return getUserName(msg.getFrom());
    }

    /**
     * Формирование имени пользователя. Если заполнен никнейм, используем его. Если нет - используем фамилию и имя
     *
     * @param user пользователь
     */
    public static String getUserName(User user) {
        return (user.getUserName() != null) ? user.getUserName() :
                String.format("%s %s", user.getLastName(), user.getFirstName());
    }

    /**
     *
     * @param queryStr - запрос
     * @param view - вьюшка
     * @param field - поле
     * @param date - дата в запросе
     */
    private static <T> T queryListResult(String queryStr, View view, String field, Date date) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.addView(view);
                query.setParameter(field, date);

                retObj = (T) query.getResultList();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }


    /**
     * @param queryStr - запрос
     * @param field    - поле условия типа Date
     * @param date     - дата
     */

    private static <T> T queryListResult(String queryStr, String field, Date date) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.setParameter(field, date);

                retObj = (T) query.getResultList();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    /**
     * @param queryStr
     * @param startDate
     * @param endDate
     * @param <T>
     * @return
     */
    private static <T> T queryOneResult(String queryStr, Date startDate, Date endDate) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);

                retObj = (T) query.getFirstResult();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    /**
     * @param queryStr
     */

    private static <T> T queryOneResult(String queryStr) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);

                retObj = (T) query.getFirstResult();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    /**
     * @param queryStr
     */
    private static <T> T queryListResult(String queryStr) {
        List<Object> retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);

                retObj = (List<Object>) query.getResultList();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    public static String getOpenPositionEditorURL(OpenPosition openPosition, String text) {
        StringBuilder sb = new StringBuilder();

        sb.append("<a href=\"http://hr.it-pearls.ru:8080/app/open?screen=itpearls_OpenPosition.edit-")
                .append(openPosition.getId().toString())
                .append("\">")
                .append(text)
                .append("</a>");

        return sb.toString();
    }

    /**
     *
     * @param openPosition
     * @return
     */

    public static String getOpenPositionEditorURL(OpenPosition openPosition) {
        StringBuilder sb = new StringBuilder();

        sb.append("http://hr.it-pearls.ru:8080/app/open?screen=")
                .append("itpearls_OpenPosition.edit")
                .append("&item=")
                .append("itpearls_OpenPosition-")
                .append(openPosition.getId().toString())
                .append("-openPosition-view")
        ;

        return sb.toString();
    }

    /**
     *
     * @param queryStr
     * @param view
     * @return
     * @param <T>
     */
    private static <T> T queryListResult(String queryStr, View view) {
        List<Object> retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.addView(view);

                retObj = (List<Object>) query.getResultList();
                tx.commit();
            }

        } finally {
            return (T) retObj;
        }
    }

    public static List<RecrutiesTasks> getRecrutiesTasks() {
        return queryListResult("select e from itpearls_RecrutiesTasks e " +
                        "where :date between e.startDate and e.endDate",
                new View(RecrutiesTasks.class)
                        .addProperty("startDate")
                        .addProperty("endDate")
                        .addProperty("recrutierName")
                        .addProperty("reacrutier",
                                new View(ExtUser.class)
                                        .addProperty("name"))
                        .addProperty("openPosition",
                                new View(OpenPosition.class)
                                        .addProperty("vacansyName")),
                "date", new Date());
    }

    public static List<OpenPosition> getOpenPosition() {
        return queryListResult("select e from itpearls_OpenPosition e " +
                "where not e.openClose = true", new View(OpenPosition.class)
                .addProperty("projectName",
                        new View(Project.class)
                                .addProperty("projectName")
                                .addProperty("projectOwner",
                                        new View(Person.class)
                                                .addProperty("firstName")
                                                .addProperty("secondName")))
                .addProperty("positionType",
                        new View(Position.class)
                                .addProperty("positionEnName")
                                .addProperty("positionRuName")));
    }

    /**
     *
     * @return
     */
    public static String getBotName() {
        return queryOneResult("select e.telegramBotName from itpearls_ApplicationSetup e " +
                "where e.activeSetup = true");
    }

    public static String getUserSessions() {
        return null;
    }

    public static String getHelloMessage() {
        return new StringBuilder()
                .append("Бот *")
                .append(TelegramBotStatus.getDefaultBotName())
                .append("*\n\n")
                .append("Описание команд:\n")
                .append("*/allvacancy* - список открытых вакансий\n")
                .append("*/subscribe* - мои подписки на вакансии\n")
                .append("*/settings* - настройки\n")
                .append("*/help* - получение помощи\n\n")
                .toString();
    }

    public synchronized static void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        // Создаем список строк клавиатуры
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        KeyboardButton helpButton = new KeyboardButton("/help");
        keyboardSecondRow.add(helpButton);

        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
    }
}
