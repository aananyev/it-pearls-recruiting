package com.company.itpearls.listeners;

import com.company.itpearls.core.InteractionListService;
import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.IteractionList;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.haulmont.cuba.core.TransactionalDataManager;
import com.haulmont.cuba.core.app.events.EntityChangedEvent;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.inject.Inject;

@Component("itpearls_IteractionListChangedListener")
public class IteractionListChangedListener {
    @Inject
    private TransactionalDataManager txDm;
    @Inject
    private InteractionListService interactionListService;

    static private BigDecimal interactionListNumber = null;
    @Inject
    private UserSessionSource userSessionSource;

    @EventListener
    public void beforeCommit(EntityChangedEvent<IteractionList, UUID> event) {
        IteractionList iteractionList;
        BigDecimal iteractionListCount = interactionListService.getCountInteraction();

        if (event.getType() == EntityChangedEvent.Type.CREATED) {
            iteractionList = txDm.load(event.getEntityId())
                    .view("iteractionList-view")
                    .one();

            if (iteractionList.getDateIteraction() == null) {
                iteractionList.setDateIteraction(new Date());
            }

            if (iteractionList.getCurrentOpenClose() == null) {
                if (iteractionList.getVacancy() != null) {
                    iteractionList.setCurrentOpenClose(iteractionList.getVacancy().getOpenClose() != null
                            ? iteractionList.getVacancy().getOpenClose() : false);
                }
            }

            if (iteractionList.getRecrutier() == null) {
                iteractionList.setRecrutier((ExtUser) userSessionSource.getUserSession().getUser());
            }

            if (iteractionList.getRecrutierName() == null) {
                iteractionList.setRecrutierName(userSessionSource.getUserSession().getUser().getName());
            }

            if (iteractionList.getNumberIteraction() == null) {
                if (interactionListNumber != null) {
                    if (iteractionListCount.add(BigDecimal.ONE)
                            .equals(interactionListNumber)) {
                        interactionListNumber = iteractionListCount.add(BigDecimal.ONE).add(BigDecimal.ONE);
                        iteractionList.setNumberIteraction(interactionListNumber);
                    }
                } else {
                    interactionListNumber = iteractionListCount.add(BigDecimal.ONE);
                    iteractionList.setNumberIteraction(interactionListNumber);
                }
            } else {
                if (!iteractionList.getNumberIteraction().equals(interactionListService.getCountInteraction().add(BigDecimal.ONE))) {
                    interactionListNumber = BigDecimal.ONE;
                    iteractionList.setNumberIteraction(interactionListNumber);
                }
            }

            txDm.save(iteractionList);
        }
    }

    @TransactionalEventListener
    public void afterCommit(EntityChangedEvent<IteractionList, UUID> event) {
        interactionListNumber = null;
    }
}