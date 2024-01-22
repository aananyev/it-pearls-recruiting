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
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.View;
import org.jsoup.Jsoup;
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

public class Utils {

    private static final int MAX_TELEGRAM_MESSAGE_LENGTH = 4096;

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

    public static String getOpenPositionEditorURL(OpenPosition openPosition, String text) {
        StringBuilder sb = new StringBuilder();

        String appUrl = AppBeans.get(Configuration.class)
                .getConfig(ServerConfig.class)
                .getUserSessionProviderUrl();

        sb.append("<a href=\"http://hr.it-pearls.ru:8080/app/open?screen=itpearls_OpenPosition.edit-")
                .append(openPosition.getId().toString())
                .append("\">")
                .append(text)
                .append("</a>");

        return sb.toString();
    }

    /**
     * @param openPosition
     * @return
     */

    public static String getOpenPositionEditorURL(OpenPosition openPosition) {
        StringBuilder sb = new StringBuilder();

        String appUrl = AppBeans.get(Configuration.class)
                .getConfig(GlobalConfig.class)
                .getWebAppUrl();

        sb.append(appUrl.replace("http:/", "http://"))
                .append("/open?screen=")
                .append("itpearls_OpenPosition.edit")
                .append("&item=")
                .append("itpearls_OpenPosition-")
                .append(openPosition.getId().toString())
                .append("-openPosition-view")
        ;

        return sb.toString();
    }

    /**
     * @param queryStr
     * @param view
     * @param <T>
     * @return
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

    private static final String QUERY_LIST_OPEN_POSITION =
            "select e from itpearls_OpenPosition e " +
                    "where (e.priority >= %s) and (not e.openClose = true)";

    public static List<OpenPosition> getOpenPosition(Chat chat) {
        int priority = Utils.getPriority(chat);
        String q = String.format(QUERY_LIST_OPEN_POSITION, priority);

        List<OpenPosition> result = queryListResult(String.format(QUERY_LIST_OPEN_POSITION, priority),
                new View(OpenPosition.class)
                        .addProperty("openPositionComments",
                                new View(OpenPositionComment.class)
                                        .addProperty("user",
                                                new View(ExtUser.class)
                                                        .addProperty("name")))
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

        return result;
    }

    public static String getBotName() {
        return queryOneResult("select e.telegramBotName " +
                "from itpearls_ApplicationSetup e " +
                "where e.activeSetup = true");
    }

    public static String getUserSessions() {
        return null;
    }

    public static String getHelloMessage() {
        return new StringBuilder()
                .append("Бот *")
                .append(getBotName())
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
        KeyboardButton setupButton = new KeyboardButton("/settings");
        keyboardSecondRow.add(helpButton);
        keyboardSecondRow.add(setupButton);

        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    public static String getOpenPositionComments(String openPosition, String key) {
        String openPositionId = openPosition.replace(key, "");

        String QUERY_GET_COMMENT = String.format("select e from itpearls_OpenPositionComment e where e.openPosition.id = '%s'",
                openPositionId.substring(1, openPositionId.length()));

        List<OpenPositionComment> openPositionComments = queryListResult(QUERY_GET_COMMENT,
                new View(OpenPositionComment.class)
                        .addProperty("openPosition", new View(OpenPosition.class)
                                .addProperty("vacansyName"))
                        .addProperty("user", new View(ExtUser.class)
                                .addProperty("name")));

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("❗\uFE0F❗\uFE0F❗\uFE0FВАКАНСИЯ: ")
                .append(openPositionComments.get(0).getOpenPosition().getVacansyName())
                .append("\n\n");

        int counter = 1;

        for (OpenPositionComment openPositionComment : openPositionComments) {
            stringBuilder.append(counter).append(". ")
                    .append(openPositionComment.getComment())
                    .append("\n")
                    .append("Автор: ")
                    .append(openPositionComment.getUser().getName())
                    .append("\n\n");
        }

        return stringBuilder.toString();

    }

    public static String getOpenPositionJobDescription(String openPosition, String key) {
        String openPositionId = openPosition.replace(key, "");
        String QUERY_GET_COMMENT = String.format("select e from itpearls_OpenPosition e where e.id = '%s'",
                openPositionId.substring(1, openPositionId.length()));

        List<OpenPosition> openPositionList = queryListResult(QUERY_GET_COMMENT,
                new View(OpenPosition.class));
        OpenPosition op = (OpenPosition) openPositionList.get(0);

        StringBuilder salary = new StringBuilder();
        Boolean salaryFlag = false;

        if (op.getSalaryCandidateRequest() != null ? op.getSalaryCandidateRequest() : false) {
            salary.append("ориентируемся на ожидания кандидатов\n");
            salaryFlag = true;
        }

        if (op.getSalaryMin() != null) {
            salary.append("мин ")
                    .append(op.getSalaryMin());
        }

        if (op.getSalaryMax() != null) {
            salary.append(" макс ")
                    .append(op.getSalaryMax())
                    .append("\n");
        }

        if (op.getSalaryComment() != null) {
            salary.append("Комментарии по заработной плате: ")
                    .append(op.getSalaryComment())
                    .append("\n");
            salaryFlag = true;
        }

        String ret = formattedHtml2text(String.format("<b>НАИМЕНОВАНИЕ ВАКАНСИИ:</b>\n" +
                        "%s\n\n" +
                        "<b>СРОЧНОСТЬ:</b> %s\n" +
                        "<b>ОПИСАНИЕ ВАКАНСИИ:</b>\n%s\n\n" +
                        "<b>ЗАРПЛАТА:</b> %s",
                op.getVacansyName(),
                OpenPositionPriority.fromId(op.getPriority()),
                (op.getComment() != null ?
                        (op.getComment().length() < MAX_TELEGRAM_MESSAGE_LENGTH ?
                                op.getComment() : op.getComment().substring(0, MAX_TELEGRAM_MESSAGE_LENGTH))
                        : ""),
                salary.toString()));

        return "❗\uFE0F❗\uFE0F❗\uFE0F" + ret;
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
}
