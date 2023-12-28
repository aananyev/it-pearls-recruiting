package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.itpearls.entity.RecrutiesTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.SimpleDateFormat;
import java.util.List;

public class SubscribeCommand extends OperationCommand{
    private Logger logger = LoggerFactory.getLogger(SettingsCommand.class);
    public SubscribeCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        try {
            List<RecrutiesTasks> recrutiesTasks = Utils.getRecrutiesTasks();

            int counter = 1;

            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), Utils.getUserName(user),
                    new StringBuilder()
                            .append("*")
                            .append(Utils.getBotName())
                            .append("*\n")
                            .append("**Всего подписок на вакансии: **")
                            .append(recrutiesTasks.size())
                            .append("\n\n")
                            .toString());

            for (RecrutiesTasks recrutiesTask : recrutiesTasks) {
                StringBuilder sb = new StringBuilder();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

                sb.append(counter++)
                        .append(". ")
                        .append(recrutiesTask.getOpenPosition().getVacansyName())
                        .append("\nC **")
                        .append(simpleDateFormat.format(recrutiesTask.getStartDate()))
                        .append("** по **")
                        .append(simpleDateFormat.format(recrutiesTask.getEndDate()))
                        .append("\n** Рекрутер: **")
                        .append(recrutiesTask.getReacrutier().getName())
                        .append("**\n");

                sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), Utils.getUserName(user), sb.toString());
            }

        } catch (NullPointerException e) {
            logger.debug("ОШИБКА - не загрузились список подписок");
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), Utils.getUserName(user),
                    "Нет активных подписок на вакансии");
        }

    }
}
