package com.company.itpearls.listeners;

import com.company.itpearls.core.ApplicationSetupService;
import com.company.itpearls.core.OpenPositionCommentService;
import com.company.itpearls.core.TelegramService;
import com.company.itpearls.entity.OpenPositionComment;

import java.util.UUID;

import com.haulmont.cuba.core.TransactionalDataManager;
import com.haulmont.cuba.core.app.events.EntityChangedEvent;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.inject.Inject;

@Component("itpearls_OpenPositionCommentChangedListener")
public class OpenPositionCommentChangedListener {

    @Inject
    private TelegramService telegramService;
    @Inject
    private ApplicationSetupService applicationSetupService;
    @Inject
    private OpenPositionCommentService openPositionCommentService;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private TransactionalDataManager txDm;

    @EventListener
    public void beforeCommit(EntityChangedEvent<OpenPositionComment, UUID> event) {
        OpenPositionComment openPositionComment;

        if (event.getType() == EntityChangedEvent.Type.CREATED) {
            openPositionComment = txDm.load(event.getEntityId())
                    .view("openPositionComment-view")
                    .one();

            telegramService.sendMessageToChat(applicationSetupService.getTelegramChatOpenPosition(),
                    openPositionCommentService
                            .getOpenPositionCommentMessage(openPositionComment,
                                    userSessionSource.getUserSession().getUser()));
        }
    }
}