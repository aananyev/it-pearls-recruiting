package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.entity.OpenPositionPriority;
import com.company.itpearls.entity.Position;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

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

        sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                new StringBuilder()
                        .append("<b><u>")
                        .append(Utils.getBotName())
                        .append("</u></b>\n")
                        .append("<b>Приоритет не ниже:</b> ")
                        .append(OpenPositionPriority.fromId(Utils.getPriority(chat)))
                        .append("\n")
                        .append("<b>Всего открыто вакансий:</b> ")
                        .append(positions.size())
                        .append("\n\n")
                        .toString());

        for (Position position : positions) {
            StringBuilder sb = new StringBuilder();

            sb.append(counter++)
                    .append(". ")
                    .append(position.getPositionRuName())
                    .append(" (")
                    .append(Utils.getPositionsVacancyCount(chat, position))
                    .append(" человек)");

            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    sb.toString());
        }
    }
}
