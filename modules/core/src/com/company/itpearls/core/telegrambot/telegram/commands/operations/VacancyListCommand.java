package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.ApplicationSetupService;
import com.company.itpearls.core.OpenPositionService;
import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.entity.OpenPosition;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class VacancyListCommand extends OperationCommand {
    @Inject
    private OpenPositionService openPositionService;

    public VacancyListCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        String userName = Utils.getUserName(user);

        Set<String> openPositions = openPositionService.getOpenPositionSet();
        StringBuilder sb = new StringBuilder();
        int counter = 1;

        for (String openPosition : openPositions) {
            sb.append(counter++)
                    .append(". ")
                    .append(openPosition)
                    .append("\n");

            sendAnswer(absSender, chat.getId(), openPosition, sb.toString(), userName, sb.toString());
        }

    }
}
