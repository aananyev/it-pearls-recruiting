package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.entity.OpenPositionPriority;
import com.company.itpearls.entity.Position;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

public class PersonPositionCommand extends OperationCommand {
    public PersonPositionCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        List<Position> positions = Utils.getActivePositionList(chat);
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
}
