package com.company.itpearls.core;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.OpenPositionComment;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(OpenPositionCommentService.NAME)
public class OpenPositionCommentServiceBean implements OpenPositionCommentService {

    @Inject
    private Events events;
    @Inject
    private ApplicationSetupService applicationSetupService;
    @Inject
    private TelegramService telegramService;
    @Inject
    private TelegramBotService telegramBotService;

    @Override
    public String getOpenPositionCommentMessage(OpenPositionComment entity, User user) {
        StringBuilder sb = new StringBuilder();

        sb.append("Опубликован комментарий к вакансии ")
                .append(entity.getOpenPosition().getVacansyName())
                .append("<br><br>")
                .append("\n\n")
                .append(entity.getComment())
                .append("<br><br>")
                .append("\n\n")
                .append("<br><svg align=\"right\" width=\"100%\"><i>")
                .append(user.getName())
                .append("</i></svg>");

        return sb.toString();
    }

    @Override
    public void publishOpenPositionComment(OpenPositionComment openPositionComment) {
        events.publish(new UiNotificationEvent(this,
                        openPositionComment.getOpenPosition().getVacansyName()));

        if (applicationSetupService.getTelegramBotStart() != null ? applicationSetupService.getTelegramBotStart() : false) {
            telegramService.sendMessageToChat(telegramBotService.getApplicationSetup().getTelegramChatOpenPosition(),
                    new StringBuilder("*")
                            .append(openPositionComment.getOpenPosition().getVacansyName())
                            .append("*\n")
                            .append(openPositionComment.getComment() != null ? openPositionComment.getComment() : "")
                            .append("\n")
                            .append(openPositionComment.getUser().getName())
                            .toString());
        }
    }

    @Override
    public void publishOpenPositionComment(OpenPositionComment openPositionComment, String message) {
        events.publish(new UiNotificationEvent(this,
                message
                        + ":"
                        + openPositionComment.getOpenPosition().getVacansyName()));

        if (applicationSetupService.getTelegramBotStart() != null ? applicationSetupService.getTelegramBotStart() : false) {
            telegramService.sendMessageToChat(new StringBuilder("")
                            .append(message)
                            .append(": ")
                            .append(openPositionComment.getOpenPosition().getVacansyName())
                            .append("\n")
                            .append(openPositionComment.getComment() != null ? openPositionComment.getComment() : "")
                            .append("\n")
                            .append(openPositionComment.getUser().getName())
                            .toString());
        }
    }

}