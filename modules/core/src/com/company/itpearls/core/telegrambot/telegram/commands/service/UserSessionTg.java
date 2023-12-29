package com.company.itpearls.core.telegrambot.telegram.commands.service;

import com.company.itpearls.core.telegrambot.Utils;
import com.haulmont.cuba.security.app.UserSessionsAPI;
import com.haulmont.cuba.security.global.UserSession;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import javax.inject.Inject;
import java.util.stream.Stream;

public class UserSessionTg extends ServiceCommand {
    @Inject
    UserSessionsAPI userSessionsAPI;
    public UserSessionTg(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {

        Stream<UserSession> userSessionsStream = userSessionsAPI.getUserSessionsStream(); // TO-DO возвращает инжектирование нулл
        UserSession userSessions[] = userSessionsStream.distinct().toArray(UserSession[]::new);
        StringBuilder sb = new StringBuilder("<b>Список пользователей в системе</b>\n");
        String userName = Utils.getUserName(user);

        int count = 1;

        for (UserSession userSession : userSessions) {
            sb.append(count++)
                    .append(". <i>")
                    .append(userSession.getUser().getName())
                    .append("</i>\n");
        }

        sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName, sb.toString());

    }
}
