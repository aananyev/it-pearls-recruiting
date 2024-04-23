package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.BotInfo;
import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class VacancyListCommand extends OperationCommand {
    private Logger logger = LoggerFactory.getLogger(SettingsCommand.class);
    public VacancyListCommand(String identifier, String description) {
        super(identifier, description);
    }
    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        String userName = Utils.getUserName(user);

        try {
            List<OpenPosition> openPositions = VacancyListCommand.getOpenPosition(chat);

            int counter = 1;
            Boolean subscribeFlag = Utils.isInternalUser(user);

            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    new StringBuilder()
                            .append(Utils.getBotName())
                            .append("\n")
                            .append("Приоритет не ниже: <b>")
                            .append(OpenPositionPriority.fromId(Utils.getPriority(chat)))
                            .append("</b>\n")
                            .append("Всего открыто вакансий: <b>")
                            .append(openPositions.size())
                            .append("</b>\n\n")
                            .toString());

            for (OpenPosition openPosition : openPositions) {
                StringBuilder sb = new StringBuilder();
                StringBuilder openPositionCounter = new StringBuilder();
                StringBuilder projectNameSB = new StringBuilder();

                if (subscribeFlag) {
                    openPositionCounter.append(" (человек - <b>")
                            .append(openPosition.getNumberPosition())
                            .append("</b>)");

                    projectNameSB.append(openPosition.getProjectName().getProjectName());
                } else {
                    projectNameSB.append(openPosition.getProjectName().getProjectNameForCandidate() != null ?
                            openPosition.getProjectName().getProjectNameForCandidate() : openPosition.getProjectName().getProjectName());
                }

                sb.append(counter++)
                        .append(". <b>")
                        .append(new StringBuilder(openPosition.getPositionType().getPositionEnName())
                                .append(" / ")
                                .append(openPosition.getPositionType().getPositionRuName())
                                .append("</b>")
                                .append(openPositionCounter)
                                .append("\n")
                        .append("Проект: <b>")
                        .append(projectNameSB)
                        .append("</b>\n")
                        .append("Приоритет: <b>")
                        .append(OpenPositionPriority.fromId(openPosition.getPriority()))
                        .append("</b>\n"));

                if (subscribeFlag) {
                    sb.append("Аккаунт\\HR на стороне заказчика: <b>")
                            .append(openPosition.getProjectName().getProjectOwner().getSecondName())
                            .append(" ")
                            .append(openPosition.getProjectName().getProjectOwner().getFirstName())
                            .append("</b>")
                            .append("\n");
                }

                sendAnswer(absSender, chat.getId(),
                        this.getCommandIdentifier(),
                        userName,
                        sb.toString(),
                        setInline(openPosition, subscribeFlag));
            }

        } catch (NullPointerException e) {
            logger.debug("ОШИБКА - не загрузились открытые вакансии");
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    "Нет открытых вакансий");
        }
    }

    public static InlineKeyboardMarkup setInline(OpenPosition openPosition, Boolean subscribeFlag) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton viewInHuntTechButton = new InlineKeyboardButton();
        viewInHuntTechButton.setText("View");
        String urlButton = getOpenPositionEditorURL(openPosition);
        viewInHuntTechButton.setUrl(urlButton);

        InlineKeyboardButton viewDetailsButton = new InlineKeyboardButton();
        viewDetailsButton.setText("Details");
        viewDetailsButton.setCallbackData(CallbackData.VIEW_DETAIL_BUTTON
                + CallbackData.CALLBACK_SEPARATOR
                + openPosition.getId().toString());

        int count_comments = Utils.countComments(openPosition);
        InlineKeyboardButton commentButton = new InlineKeyboardButton();

            if (count_comments > 0) {
                commentButton.setText("Comment (" + Utils.countComments(openPosition) + ")");
                commentButton.setCallbackData(CallbackData.COMMENT_VIEW_BUTTON
                        + CallbackData.CALLBACK_SEPARATOR
                        + openPosition.getId().toString());
        }

        InlineKeyboardButton subscribeButton = new InlineKeyboardButton();
        subscribeButton.setText("Subscribe");
        subscribeButton.setCallbackData(CallbackData.SUBSCRIBE_BUTTON
                + CallbackData.CALLBACK_SEPARATOR
                + openPosition.getId().toString());

        InlineKeyboardButton subscribersRecruterButton = new InlineKeyboardButton();
        subscribersRecruterButton.setCallbackData(CallbackData.SUBSCRIBERS_BUTTON
                + CallbackData.CALLBACK_SEPARATOR
                + openPosition.getId().toString());

        rowInline.add(viewDetailsButton);


        if (subscribeFlag) {
            rowInline.add(viewInHuntTechButton);
            if (count_comments > 0) {
                rowInline.add(commentButton);
            }
            rowInline.add(subscribeButton);
        } else {
            int subscribers_count = SubscribeCommand.isOpenPositionSubscribers(CallbackData.SUBSCRIBERS_BUTTON,
                    CallbackData.SUBSCRIBERS_BUTTON
                            + CallbackData.CALLBACK_SEPARATOR
                            + openPosition.getId().toString()).size();

            if (subscribers_count > 0) {
                subscribersRecruterButton.setText(new StringBuilder("Subscribers (")
                        .append(subscribers_count)
                        .append(")")
                        .toString());
                rowInline.add(subscribersRecruterButton);
            }
        }

        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;
    }

    private InlineKeyboardMarkup setInline(OpenPosition openPosition, SendMessage message) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();

        inlineKeyboardButton.setText("View");
        inlineKeyboardButton.setUrl(getOpenPositionEditorURL(openPosition));

        rowInline.add(inlineKeyboardButton);
        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;
    }

    public static String getOpenPositionJobDescription(User user, String openPosition, String key) {
        String openPositionId = openPosition.replace(key, "");
        String QUERY_GET_COMMENT = String.format("select e from itpearls_OpenPosition e where e.id = '%s'",
                openPositionId.substring(1, openPositionId.length()));

        List<OpenPosition> openPositionList = Utils.queryListResult(QUERY_GET_COMMENT,
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
//                op.getVacansyName(),
                BotInfo.getVacancyName(op, user),
                op.getNumberPosition(),
                OpenPositionPriority.fromId(op.getPriority()),
                (op.getComment() != null ?
                        (Utils.formattedHtml2text(op.getComment()).length() < Utils.MAX_TELEGRAM_MESSAGE_LENGTH ?
                                Utils.formattedHtml2text(op.getComment()) :
                                Utils.formattedHtml2text(op.getComment()).substring(0, Utils.MAX_TELEGRAM_MESSAGE_LENGTH))
                        : ""),
                salary.toString());

        return ret;
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


    private static final String QUERY_LIST_OPEN_POSITION =
            "select e from itpearls_OpenPosition e " +
                    "where (e.priority >= %s) and (not e.openClose = true)";
    private static final String QUERY_LIST_OPEN_POSITION_ID =
            "select e from itpearls_OpenPosition e " +
                    "where e.id = :uuid";


    public static List<OpenPosition> getOpenPosition(String openPositionCallBack, String openPositionKey) {

        List<OpenPosition> result = Utils.queryListResult(QUERY_LIST_OPEN_POSITION_ID,
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

    public static List<OpenPosition> getOpenPosition(Chat chat) {
        int priority = Utils.getPriority(chat);
        String q = String.format(QUERY_LIST_OPEN_POSITION, priority);

        List<OpenPosition> result = Utils.queryListResult(String.format(QUERY_LIST_OPEN_POSITION, priority),
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

    public static String getOpenPositionComments(String openPosition, String key) {
        String openPositionId = openPosition.replace(key, "");
        SimpleDateFormat sdf = new SimpleDateFormat("dd EEEE yyyy H:m");

        String QUERY_GET_COMMENT = String.format("select e from itpearls_OpenPositionComment e where e.openPosition.id = '%s'",
                openPositionId.substring(1, openPositionId.length()));

        List<OpenPositionComment> openPositionComments = Utils.queryListResult(QUERY_GET_COMMENT,
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
}
