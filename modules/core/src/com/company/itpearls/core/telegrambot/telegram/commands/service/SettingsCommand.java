package com.company.itpearls.core.telegrambot.telegram.commands.service;


import com.company.itpearls.entity.OpenPositionPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.Settings;

/**
 * Команда получения текущих настроек
 */
public class SettingsCommand extends ServiceCommand {
    private Logger logger = LoggerFactory.getLogger(SettingsCommand.class);

    public SettingsCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        String userName = Utils.getUserName(user);

            logger.debug(String.format("Пользователь %s. Начато выполнение команды %s", userName,
                this.getCommandIdentifier()));

        Settings settings = Bot.getUserSettings(chat.getId());

        if (strings.length == 0) {
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    String.format("*ТЕКУЩИЕ НАСТРОЙКИ*\n"
                                    + " - приоритет вакансии не ниже *%s (%s)*\n"
                                    + " - публикация новых вакансий *%s*\n\n" +
                                    "Для изменения настроек наберите команду */settings*, " +
                                    "а затем ключи [[приоритет *от 0 до 4* (0 - минимальный приоритет)]] " +
                                    "и [[публикация новых вакансий *true/false*]]\n" +
                                    "Например /settings 3 true",
                            OpenPositionPriority.fromId(settings.getPriorityNotLower()),
                            settings.getPriorityNotLower(),
                            settings.getPublishNewVacancies()));
        } else {
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName, "Изменение настроек");

            settings.setPriorityNotLower(Integer.parseInt(strings[0]));
            settings.setPublishNewVacancies(strings.equals("true") ? true : false);

            Bot.setUserSettings(chat.getId(), settings);

        }

        logger.debug(String.format("Пользователь %s. Завершено выполнение команды %s", userName,
                this.getCommandIdentifier()));
    }
}