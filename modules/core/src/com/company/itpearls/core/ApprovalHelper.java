package com.company.itpearls.core;

import com.company.itpearls.entity.OpenPosition;
import org.springframework.stereotype.Component;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;

import javax.inject.Inject;
import java.util.UUID;

@Component(ApprovalHelper.NAME)
public class ApprovalHelper {
    public static final String NAME = "itpearls_ApprovalHelper";

    @Inject
    private Persistence persistence;

    public void updateState(UUID entityId, Boolean openClose) {
        try (Transaction tx = persistence.getTransaction()) {
            OpenPosition contract = persistence.getEntityManager().find(OpenPosition.class, entityId);
            if (contract != null) {
                contract.setOpenClose(openClose);
            }
            tx.commit();
        }
    }
}