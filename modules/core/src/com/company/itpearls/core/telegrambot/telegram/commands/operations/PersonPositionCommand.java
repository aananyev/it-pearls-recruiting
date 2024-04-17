package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.Settings;
import com.company.itpearls.entity.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PersonPositionCommand extends OperationCommand {
    private static Logger logger = LoggerFactory.getLogger(Bot.class);

    public PersonPositionCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        List<Position> positions = getActivePositionList(chat);
        String userName = Utils.getUserName(user);
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
                        .append(positions.size())
                        .append("</b>\n\n")
                        .toString());

        for (Position position : positions) {
            StringBuilder sb = new StringBuilder();

            sb.append(counter++)
                    .append(". <b>")
                    .append(position.getPositionRuName())
                    .append(" / ")
                    .append(position.getPositionEnName())
                    .append("</b> (<b>")
                    .append(Utils.getPositionsVacancyCount(chat, position))
                    .append("</b> человек)");

            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    sb.toString(), setInline(position, subscribeFlag));
        }
    }

    private InlineKeyboardMarkup setInline(Position position, Boolean subscribeFlag) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton viewDetailsButton = new InlineKeyboardButton();
        viewDetailsButton.setText("View");
        viewDetailsButton.setCallbackData(CallbackData.VIEW_VACANSIES_BUTTON
                + CallbackData.CALLBACK_SEPARATOR
                + position.getId().toString());

        rowInline.add(viewDetailsButton);

        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;

    }

    private static final String QUERY_LIST_POSITION_OPEN_POSITION_ID =
            "select e from itpearls_OpenPosition e " +
                    "where e.priority >= %s and not(e.openClose = true) and e.positionType.id = :uuid";

    public static List<OpenPosition> getPositonOpenPosition(Long chatId, String positionCallBack, String positionKey) {
        Settings settings = Bot.getUserSettings(chatId);

        List<OpenPosition> result = Utils.queryListResult(String.format(QUERY_LIST_POSITION_OPEN_POSITION_ID,
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

}
