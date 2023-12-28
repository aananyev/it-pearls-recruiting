package com.company.itpearls.core.telegrambot.telegram.commands.service;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.entity.OpenPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

public class VacancyListCommand extends ServiceCommand {

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
                            .append("*")
                            .append(Utils.getBotName())
                            .append("*\n")
                            .append("**Всего открыто вакансий: **")
                            .append(openPositions.size())
                            .append("\n\n")
                            .toString());

            for (OpenPosition openPosition : openPositions) {
                StringBuilder sb = new StringBuilder();

                sb.append(counter++)
                        .append(". ")
                        .append(openPosition.getVacansyName())
                        .append("\n");

                sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName, sb.toString());
            }

        } catch (NullPointerException e) {
            logger.debug("ОШИБКА - не загрузились открытые вакансии");
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    "Нет открытых вакансий");
        }

    }
}
