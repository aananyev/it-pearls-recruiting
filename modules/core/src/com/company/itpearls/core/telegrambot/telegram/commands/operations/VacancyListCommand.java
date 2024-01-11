package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.itpearls.entity.OpenPosition;
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
            List<OpenPosition> openPositions = Utils.getOpenPosition();

            int counter = 1;

            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    new StringBuilder()
                            .append("<b><u>")
                            .append(Utils.getBotName())
                            .append("</u><b>\n")
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
                        .append("<b>Аккаунт\\HR на стороне заказчика: </b><i>")
                        .append(openPosition.getProjectName().getProjectOwner().getSecondName())
                        .append(" ")
                        .append(openPosition.getProjectName().getProjectOwner().getFirstName())
                        .append("</i>")
                        .append("\n");

                sendAnswer(absSender, chat.getId(),
                        this.getCommandIdentifier(),
                        userName,
                        sb.toString(),
                        setInline(openPosition));
            }

        } catch (NullPointerException e) {
            logger.debug("ОШИБКА - не загрузились открытые вакансии");
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    "Нет открытых вакансий");
        }

    }

    private InlineKeyboardMarkup setInline(OpenPosition openPosition) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();

        inlineKeyboardButton.setText("View");
        String urlButton = Utils.getOpenPositionEditorURL(openPosition);
        inlineKeyboardButton.setUrl(urlButton);
//        inlineKeyboardButton.setUrl("http://hr.it-pearls.ru:8080/app");

        rowInline.add(inlineKeyboardButton);
        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;
    }

    private InlineKeyboardMarkup setInline(OpenPosition openPosition, SendMessage message) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();

//        inlineKeyboardButton.setSwitchInlineQueryCurrentChat(" ");
        inlineKeyboardButton.setText("View");
        inlineKeyboardButton.setUrl(Utils.getOpenPositionEditorURL(openPosition));
//        inlineKeyboardButton.setCallbackData(" ");

        rowInline.add(inlineKeyboardButton);
        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;
    }
}
