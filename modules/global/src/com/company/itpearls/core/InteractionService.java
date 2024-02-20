package com.company.itpearls.core;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.security.entity.User;

import java.util.List;

public interface InteractionService {
    String NAME = "itpearls_InteractionService";

    Iteraction getDefaultInteraction();

    List<Iteraction> getMostPolularIteraction(User user, int maxCount);

    IteractionList getLastIteraction(JobCandidate jobCandidate);

//    BigDecimal getCountInteraction();
}