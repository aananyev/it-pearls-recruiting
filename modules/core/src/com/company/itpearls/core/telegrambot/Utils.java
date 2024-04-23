package com.company.itpearls.core.telegrambot;

import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.Settings;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.View;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Bot.class);

    public static final int MAX_TELEGRAM_MESSAGE_LENGTH = 4096;

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
     * @param queryStr - запрос
     * @param view     - вьюшка
     * @param field    - поле
     * @param date     - дата в запросе
     */
    public static <T> T queryListResult(String queryStr, View view, String field, Date date) {
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
        } catch (Exception e) {
            logger.error(new StringBuilder("Ошибка запроса \'").append(queryStr).append("\'").toString());
        } finally {
            return (T) retObj;
        }
    }

    public static <T> T queryListResult(String queryStr, View view, UUID uuid) {
        Object retObj = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(queryStr);
                query.addView(view);
                query.setParameter("uuid", uuid);

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

    public static <T> T queryListResult(String queryStr, String field, Date date) {
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
    public static <T> T queryOneResult(String queryStr, Date startDate, Date endDate) {
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
    public static <T> T queryListResult(String queryStr) {
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

    public static String getAppURL() {
        String appUrl = AppBeans.get(Configuration.class)
                .getConfig(ServerConfig.class)
                .getUserSessionProviderUrl();

        return appUrl;
    }

    /**
     * @param queryStr
     * @param view
     * @param <T>
     * @return
     */
    public static <T> T queryListResult(String queryStr, View view) {
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

    public static int getPriority() {
        int priority = 3;
        long chatid = Long.parseLong(TelegramBotStatus.getApplicationSetup().getTelegramChatOpenPosition());
        Settings settings = Bot.getUserSettings(chatid);
        priority = settings.getPriorityNotLower();

        return priority;
    }

    public static int getPriority(Chat chat) {
        int priority = 3;
        Settings settings = Bot.getUserSettings(chat.getId());
        priority = settings.getPriorityNotLower();

        return priority;
    }

    public static UUID getUUID(String callBack, String key) {
        return UUID.fromString(callBack.substring(key.length() + 1));
    }

    public static String getBotName() {
        String botName = queryOneResult("select e.telegramBotName " +
                "from itpearls_ApplicationSetup e " +
                "where e.activeSetup = true");
        return new StringBuilder("\uD83E\uDD16БОТ <b><u>")
                .append(botName)
                .append("</u></b>")
                .toString();
    }

    public static String getUserSessions() {
        return null;
    }


    public static String getHelloMessage(User user) {
        return new StringBuilder()
//                .append(Utils.getBotName())
                .append("\n")
                .append(BotInfo.botShortDescription(user))
                .append("\n")
                .append("ОПИСАНИЕ КОМАНД БОТА:\n")
                .append("/allvacancy - список открытых вакансий\n")
                .append("/positions - вакансии по должностям\n")
                .append("/projects - описание проектов и вакансии к ним\n")
                .append("/mysubscribe - мои подписки на вакансии\n")
                .append("/subscribers - подписчики на вакансии\n")
//                .append("/settings - настройки\n")
                .append("/help - получение помощи\n\n")
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
        KeyboardButton setupButton = new KeyboardButton("/settings");
        keyboardSecondRow.add(helpButton);
        keyboardSecondRow.add(setupButton);

        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    static public String formattedHtml2text(String inputHtml) {
        return String.valueOf(Jsoup.parse(inputHtml
                        .replace("<br>", "\n")
                        .replace("<ol>", "<ul>")
                        .replace("</ol>", "</ul>")
                        .replace("<li>", "- ")
                        .replace("</li>", "\n"))
                .body().wholeText());
    }

    public static int countComments(OpenPosition openPosition) {
        return openPosition.getOpenPositionComments().size();
    }


    public static Boolean isInternalUser(User user) {
        String query = String.format("select e from itpearls_ExtUser e where e.telegram like '%%%s'", user.getUserName());

        List<ExtUser> extUsers = Utils.queryListResult(query);

        if (extUsers != null)
            return extUsers.size() > 0;
        else
            return false;
    }

    public static Boolean isInternalUser(String user) {
        String query = String.format("select e from itpearls_ExtUser e where e.telegram like '%%%s'", user);

        List<ExtUser> extUsers = Utils.queryListResult(query);

        if (extUsers != null)
            return extUsers.size() > 0;
        else
            return false;
    }

    public static ExtUser getInternalUser(String userName) {
        String query = String.format("select e from itpearls_ExtUser e where e.telegram like '%%%s'", userName);

        List<ExtUser> extUsers = Utils.queryListResult(query);

        if (extUsers != null)
            return extUsers.get(0);
        else
            return null;
    }

    public static String getAdministratorTelegram() {
        return TelegramBotStatus.getApplicationSetup().getAdministrator().getTelegram();
    }

    public static String getCoordinatorTelegram() {
        return TelegramBotStatus.getApplicationSetup().getCoordinator().getTelegram();
    }

    public static int getPositionsVacancyCount(Chat chat, Position position) {
        final String QUERY = String.format("select sum(e.numberPosition) from itpearls_OpenPosition e where e.positionType.id = %s and not (e.openClose = true)", position.getId());
        int retInt = queryListResult(QUERY);
        return retInt;
    }
}
