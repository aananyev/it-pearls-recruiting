package com.company.itpearls.core;

import org.springframework.stereotype.Component;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;

import javax.inject.Inject;
import java.util.UUID;

@Component(ApprovalHelper.NAME)
public class ApprovalHelper {
    public static final String NAME = "demo_ApprovalHelper";

    @Inject
    private Persistence persistence;

    public void updateState(UUID entityId, String state) {
/*        try (Transaction tx = persistence.getTransaction()) {
            Contract contract = persistence.getEntityManager().find(Contract.class, entityId);
            if (contract != null) {
                contract.setState(state);
            }
            tx.commit();
        } */
    }
}