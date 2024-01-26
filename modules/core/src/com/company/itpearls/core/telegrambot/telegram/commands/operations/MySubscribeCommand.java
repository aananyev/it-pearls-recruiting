package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.RecrutiesTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MySubscribeCommand extends OperationCommand{
    private Logger logger = LoggerFactory.getLogger(SettingsCommand.class);
    public MySubscribeCommand(String identifier, String description) {
            super(identifier, description);
        }
    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        if (Utils.isInternalUser(user)) {
        try {
                List<RecrutiesTasks> recrutiesTasks =
                        Utils.getMyRecrutiesTasks(Utils.getInternalUser(user.getUserName()));

                int counter = 1;

                if (recrutiesTasks.size() > 0) {
                    sendAnswer(absSender, chat.getId(),
                            this.getCommandIdentifier(),
                            Utils.getUserName(user),
                            new StringBuilder()
                                    .append("<b><u>Бот ")
                                    .append(Utils.getBotName())
                                    .append("</u></b>\n")
                                    .append("<b>МОИ ПОДПИСКИ НА ВАКАНСИИ</b>\n")
                                    .append("<b>ВСЕГО ПОДПИСОК НА ВАКАНСИИ:</b> <i>")
                                    .append(recrutiesTasks.size())
                                    .append("</i>\n\n")
                                    .toString());

                    for (RecrutiesTasks recrutiesTask : recrutiesTasks) {
                        StringBuilder sb = new StringBuilder();

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

                        sb.append(counter++)
                                .append(". ")
                                .append(recrutiesTask.getOpenPosition().getVacansyName())
                                .append("\nC <i>")
                                .append(simpleDateFormat.format(recrutiesTask.getStartDate()))
                                .append("</i> по <i>")
                                .append(simpleDateFormat.format(recrutiesTask.getEndDate()))
                                .append("</i>\n");

                        sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), Utils.getUserName(user), sb.toString());
                    }
                } else {
                    sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(),
                            Utils.getUserName(user),
                            "⛔\uFE0F У ВАС НЕТ АКТИВНЫХ ПОДПИСОК НА ВАКАНСИИ.\n" +
                                    "ПОДПИШИТЕСЬ В РАЗДЕЛЕ /allvacancy или в системе HuntTech: " + Utils.getAppURL());
                }

            } catch(NullPointerException e){
                logger.debug("ОШИБКА - не загрузились список подписок");
                sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), Utils.getUserName(user),
                        "Нет активных подписок на вакансии");
            }
        } else {
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(),
                    Utils.getUserName(user),
                    "⛔\uFE0F ВЫ НЕ ЯВЛЯЕТЕСЬ РЕКРУТЕРОМ КОМПАНИИ. ОБРАТИТЕСЬ К АДМИНИСТРАТОРУ. @AlekseyAnanyev");
        }
    }
}
