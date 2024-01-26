package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.OpenPositionPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

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
            List<OpenPosition> openPositions = Utils.getOpenPosition(chat);

            int counter = 1;
            Boolean subscribeFlag = Utils.isInternalUser(user);

            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    new StringBuilder()
                            .append("<b><u>")
                            .append(Utils.getBotName())
                            .append("</u></b>\n")
                            .append("<b>Приоритет не ниже:</b> ")
                            .append(OpenPositionPriority.fromId(Utils.getPriority(chat)))
                            .append("\n")
                            .append("<b>Всего открыто вакансий:</b> ")
                            .append(openPositions.size())
                            .append("\n\n")
                            .toString());

            for (OpenPosition openPosition : openPositions) {
                StringBuilder sb = new StringBuilder();

                sb.append(counter++)
                        .append(". ")
                        .append(new StringBuilder(openPosition.getPositionType().getPositionEnName())
                                .append(" / ")
                                .append(openPosition.getPositionType().getPositionRuName())
                                .append("\n"))
                        .append("<b>Проект:</b> <i>")
                        .append(openPosition.getProjectName().getProjectName())
                        .append("</i>\n")
                        .append("<b>Приоритет:</b> <i>")
                        .append(OpenPositionPriority.fromId(openPosition.getPriority()))
                        .append("</i>\n");

                if (subscribeFlag) {
                    sb.append("<b>Аккаунт\\HR на стороне заказчика: </b><i>")
                            .append(openPosition.getProjectName().getProjectOwner().getSecondName())
                            .append(" ")
                            .append(openPosition.getProjectName().getProjectOwner().getFirstName())
                            .append("</i>")
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

    private InlineKeyboardMarkup setInline(OpenPosition openPosition, Boolean subscribeFlag) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton viewInHuntTechButton = new InlineKeyboardButton();
        viewInHuntTechButton.setText("View");
        String urlButton = Utils.getOpenPositionEditorURL(openPosition);
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
            int subscribers_count = Utils.isOpenPositionSubscribers(CallbackData.SUBSCRIBERS_BUTTON,
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
        inlineKeyboardButton.setUrl(Utils.getOpenPositionEditorURL(openPosition));

        rowInline.add(inlineKeyboardButton);
        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;
    }
}
