package com.company.hunttech.core;

import com.company.hunttech.entity.OpenPosition;
import org.springframework.stereotype.Component;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;

import javax.inject.Inject;
import java.util.UUID;

@Component(OpenPositionApprovalBean.NAME)
public class OpenPositionApprovalBean {
    public static final String NAME = "hunttech_OpenPositionApprovalBean";

    @Inject
    private Persistence persistence;

    public void updateState(UUID entityId, Integer state) {
        try (Transaction tx = persistence.getTransaction()) {
            OpenPosition openPosition = persistence.getEntityManager().find(OpenPosition.class, entityId);
            if (openPosition != null) {
                openPosition.setPriority(state);
            }
            tx.commit();
        }
    }
}