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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Bot.class);

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
        } catch (Exception e) {
            logger.error(new StringBuilder("Ошибка запроса \'").append(queryStr).append("\'").toString());
        } finally {
            return (T) retObj;
        }
    }

    private static <T> T queryListResult(String queryStr, View view, UUID uuid) {
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

    public static String getAppURL() {
        String appUrl = AppBeans.get(Configuration.class)
                .getConfig(ServerConfig.class)
                .getUserSessionProviderUrl();

        return appUrl;
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

    public static List<RecrutiesTasks> getMyRecrutiesTasks(ExtUser user) {
        return Utils.getRecrutiesTasks(user);
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
    private static final String QUERY_LIST_OPEN_POSITION_ID =
            "select e from itpearls_OpenPosition e " +
                    "where e.id = :uuid";

    private static final String QUERY_LIST_POSITION_OPEN_POSITION_ID =
            "select e from itpearls_OpenPosition e " +
                    "where e.priority >= %s and not(e.openClose = true) and e.positionType.id = :uuid";

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

    public static UUID getUUID(String callBack, String key) {
        return UUID.fromString(callBack.substring(key.length() + 1));
    }

    public static List<OpenPosition> getOpenPosition(String openPositionCallBack, String openPositionKey) {

        List<OpenPosition> result = queryListResult(QUERY_LIST_OPEN_POSITION_ID,
                new View(OpenPosition.class)
                        .addProperty("vacansyName")
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
                                        .addProperty("positionRuName")),
                Utils.getUUID(openPositionCallBack, openPositionKey));

        return result;
    }

    public static List<OpenPosition> getPositonOpenPosition(Long chatId, String positionCallBack, String positionKey) {
        Settings settings = Bot.getUserSettings(chatId);

        List<OpenPosition> result = queryListResult(String.format(QUERY_LIST_POSITION_OPEN_POSITION_ID,
                        settings.getPriorityNotLower()),
                new View(OpenPosition.class)
                        .addProperty("openPositionComments", new View(OpenPositionComment.class)
                                .addProperty("comment"))
                        .addProperty("vacansyName")
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
                                        .addProperty("positionRuName")),
//                Utils.getUUID(positionCallBack, positionKey));
                UUID.fromString(positionKey));

        return result;
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

    public static String getHelloMessage() {
        return new StringBuilder()
                .append(Utils.getBotName())
                .append("\n")
                .append("ОПИСАНИЕ КОМАНД БОТА:\n")
                .append("/allvacancy - список открытых вакансий\n")
                .append("/positions - вакансии по должностям\n")
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

    public static String getOpenPositionComments(String openPosition, String key) {
        String openPositionId = openPosition.replace(key, "");
        SimpleDateFormat sdf = new SimpleDateFormat("dd EEEE yyyy H:m");

        String QUERY_GET_COMMENT = String.format("select e from itpearls_OpenPositionComment e where e.openPosition.id = '%s'",
                openPositionId.substring(1, openPositionId.length()));

        List<OpenPositionComment> openPositionComments = queryListResult(QUERY_GET_COMMENT,
                new View(OpenPositionComment.class)
                        .addProperty("dateComment")
                        .addProperty("openPosition", new View(OpenPosition.class)
                                .addProperty("vacansyName"))
                        .addProperty("user", new View(ExtUser.class)
                                .addProperty("name")));

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append(Utils.getBotName())
                .append("\n")
                .append("❗\uFE0FВАКАНСИЯ: ")
                .append(openPositionComments.get(0).getOpenPosition().getVacansyName())
                .append("\n\n");

        int counter = 1;

        for (OpenPositionComment openPositionComment : openPositionComments) {
            stringBuilder.append(counter++)
                    .append(". <b>Дата:</b> ")
                    .append(sdf.format(openPositionComment.getDateComment()))
                    .append("\n<i>")
                    .append(openPositionComment.getComment())
                    .append("</i>\n")
                    .append("<b>Автор:</b> ")
                    .append(openPositionComment.getUser().getName())
                    .append("\n\n");
        }

        return stringBuilder.toString();

    }

    public static String getOpenPositionJobDescription(User user, String openPosition, String key) {
        String openPositionId = openPosition.replace(key, "");
        String QUERY_GET_COMMENT = String.format("select e from itpearls_OpenPosition e where e.id = '%s'",
                openPositionId.substring(1, openPositionId.length()));

        List<OpenPosition> openPositionList = queryListResult(QUERY_GET_COMMENT,
                new View(OpenPosition.class)
                        .addProperty("positionType", new View(Position.class)
                                .addProperty("positionRuName")
                                .addProperty("positionEnName"))
                        .addProperty("projectName", new View(Project.class)
                                .addProperty("projectName"))
                        .addProperty("numberPosition")
                        .addProperty("openClose")
                        .addProperty("priority")
                        .addProperty("vacansyName")
                        .addProperty("salaryCandidateRequest")
                        .addProperty("salaryComment")
                        .addProperty("salaryMax")
                        .addProperty("salaryMin"));
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

        if (Utils.isInternalUser(user)) {
            if (op.getSalaryComment() != null) {
                salary.append("Комментарии по заработной плате: ")
                        .append(op.getSalaryComment())
                        .append("\n");
                salaryFlag = true;
            }
        }

        String ret = String.format("%s\n" +
                        "НАИМЕНОВАНИЕ ВАКАНСИИ:\n" +
                        "<b>%s</b>\n\n" +
                        "КОЛИЧЕСТВО ПЕРСОНАЛА: <b>%s</b>\n" +
                        "СРОЧНОСТЬ: <b>%s</b>\n\n" +
                        "ОПИСАНИЕ ВАКАНСИИ:\n<i>%s</i>\n\n" +
                        "ЗАРПЛАТА: <b>%s</b>",
                Utils.getBotName(),
                op.getVacansyName(),
                op.getNumberPosition(),
                OpenPositionPriority.fromId(op.getPriority()),
                (op.getComment() != null ?
                        (formattedHtml2text(op.getComment()).length() < MAX_TELEGRAM_MESSAGE_LENGTH ?
                                formattedHtml2text(op.getComment()) :
                                formattedHtml2text(op.getComment()).substring(0, MAX_TELEGRAM_MESSAGE_LENGTH))
                        : ""),
                salary.toString());

        return ret;
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

    public static Boolean isInternalUser(User user) {
        String query = String.format("select e from itpearls_ExtUser e where e.telegram like '%%%s'", user.getUserName());

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

    public static List<Position> getActivePositionList(Chat chat) {
        List<Position> positions = new ArrayList<>();
        Settings settings = Bot.getUserSettings(chat.getId());

        final String QUERY_ACTIVE_POSITION_LIST
                = "select e from itpearls_Position e where e in " +
                "(select f.positionType from itpearls_OpenPosition f " +
                "where not (f.openClose = true) and f.priority >= :priority)";

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(QUERY_ACTIVE_POSITION_LIST);
                query.addView(new View(Position.class)
                        .addProperty("positionRuName")
                        .addProperty("positionEnName"));
                query.setParameter("priority", settings.getPriorityNotLower());

                positions = (List<Position>) query.getResultList();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            return positions;
        }
    }

    public static int getPositionsVacancyCount(Chat chat, Position position) {
        final String QUERY_POSITIONVACANCY_COUNT = "select sum(e.numberPosition) from itpearls_OpenPosition e " +
                "where e.positionType = :position and e.priority >= :priority and not (e.openClose = true)";
        Long count = null;
        Settings settings = Bot.getUserSettings(chat.getId());


        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(QUERY_POSITIONVACANCY_COUNT);
                query.setParameter("priority", settings.getPriorityNotLower());
                query.setParameter("position", position);

                count = (Long) query.getSingleResult();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            return Math.toIntExact(count);
        }
    }

    public static String getPositionUUID(String positionID) {
        final String QUERY_GET_POSITION_NAME = "select e from itpearls_Position e where e.id = :uuid";
        Position position = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(QUERY_GET_POSITION_NAME);
                query.setParameter("uuid", UUID.fromString(positionID));

                position = (Position) query.getSingleResult();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            return new StringBuilder()
                    .append(position.getPositionEnName() != null ? position.getPositionEnName() : "")
                    .append(" / ")
                    .append(position.getPositionRuName() != null ? position.getPositionRuName() : "")
                    .toString();
        }


    }
}
