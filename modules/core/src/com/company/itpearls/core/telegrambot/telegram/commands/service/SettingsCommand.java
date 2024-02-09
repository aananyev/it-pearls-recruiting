package com.company.itpearls.core.telegrambot.telegram.commands.service;


import com.company.itpearls.core.StdPriority;
import com.company.itpearls.core.telegrambot.TelegramBotStatus;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.entity.OpenPositionPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.Settings;

import java.util.ArrayList;
import java.util.List;

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
        TelegramBotStatus.setChatId(chat.getId());

            logger.debug(String.format("Пользователь %s. Начато выполнение команды %s", userName,
                this.getCommandIdentifier()));

        Settings settings = Bot.getUserSettings(chat.getId());
        int strings_length = strings != null ? strings.length : 0;

        if (strings_length == 0) {
            InlineKeyboardMarkup keyboardMarkup = settingsInlineKeyboard(settings);

            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    String.format("<b>%s</b>\n" +
                                    "⚙\uFE0F<b>ТЕКУЩИЕ НАСТРОЙКИ</b>\n"
                                    + " - приоритет вакансии не ниже <b>%s (%s)</b>\n"
                                    + " - публикация новых вакансий <b>%s</b>\n\n" +
                                    "Для изменения настроек наберите команду <b>/settings</b>, " +
                                    "а затем ключи [[приоритет <b>от 0 до 4</b> (0 - минимальный приоритет)]] " +
                                    "и [[публикация новых вакансий <b>true/false</b>]]\n" +
                                    "Например /settings 3 true",
                            Utils.getBotName(),
                            OpenPositionPriority.fromId(settings.getPriorityNotLower()),
                            settings.getPriorityNotLower(),
                            settings.getPublishNewVacancies()), keyboardMarkup);
        } else {
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName, "⚙\uFE0FИзменение настроек");

            settings.setPriorityNotLower(Integer.parseInt(strings[0]));
            settings.setPublishNewVacancies(strings[1].equals("true") ? true : false);

            Bot.setUserSettings(chat.getId(), settings);

            execute(absSender, user, chat, null);

        }

        logger.debug(String.format("Пользователь %s. Завершено выполнение команды %s", userName,
                this.getCommandIdentifier()));
    }

    private InlineKeyboardButton getInlineButton(String textButton, String callbackData, Settings settings) {
        StringBuilder sb = new StringBuilder(textButton);
        String s = OpenPositionPriority.fromId(settings.getPriorityNotLower()).name();
        StringBuilder b = new StringBuilder(s);
        StringBuilder c = new StringBuilder(textButton);
        sb.append(b.toString().equalsIgnoreCase(c.toString()) ? "(*)" : "");
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText(sb.toString());
        inlineKeyboardButton.setCallbackData(callbackData);

        return inlineKeyboardButton;
    }

    private InlineKeyboardMarkup settingsInlineKeyboard(Settings settings) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> priorityRowInline = new ArrayList<>();
        List<InlineKeyboardButton> publishVacancyRowInline = new ArrayList<>();

        InlineKeyboardButton pausePriority = getInlineButton(StdPriority.PRIORITY_PAUSED_NAME,
                CallbackData.PAUSE_PRIORITY_BUTTON, settings);
        InlineKeyboardButton lowPriority = getInlineButton(StdPriority.PRIORITY_LOW_NAME,
                CallbackData.LOW_PRIORITY_BUTTON, settings);
        InlineKeyboardButton normalPriority = getInlineButton(StdPriority.PRIORITY_NORMAL_NAME,
                CallbackData.NORMAL_PRIORITY_BUTTON, settings);
        InlineKeyboardButton highPriority = getInlineButton(StdPriority.PRIORITY_HIGH_NAME,
                CallbackData.HIHG_PRIORITY_BUTTON, settings);
        InlineKeyboardButton criticalPriority = getInlineButton(StdPriority.PRIORITY_CRITICAL_NAME,
                CallbackData.CRITICAL_PRIORITY_BUTTON, settings);

        InlineKeyboardButton publishTrue = new InlineKeyboardButton();
        publishTrue.setText("Да" + (settings.getPublishNewVacancies() ? "(*)" : ""));
        publishTrue.setCallbackData(CallbackData.TRUE_CV_PUBLISH_BUTTON);

        InlineKeyboardButton publishFalse = new InlineKeyboardButton();
        publishFalse.setText("Нет" + (!settings.getPublishNewVacancies() ? "(*)" : ""));
        publishFalse.setCallbackData(CallbackData.FALSE_CV_PUBLISH_BUTTON);

        priorityRowInline.add(pausePriority);
        priorityRowInline.add(lowPriority);
        priorityRowInline.add(normalPriority);
        priorityRowInline.add(highPriority);
        priorityRowInline.add(criticalPriority);

        publishVacancyRowInline.add(publishTrue);
        publishVacancyRowInline.add(publishFalse);

        buttons.add(priorityRowInline);
        buttons.add(publishVacancyRowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;
    }
}