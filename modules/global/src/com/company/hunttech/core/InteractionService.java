package com.company.hunttech.core;

import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.security.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface InteractionService {
    String NAME = "hunttech_InteractionService";

    List<Iteraction> getMostPolularIteraction(User user, int maxCount);

    IteractionList getLastIteraction(JobCandidate jobCandidate);

    BigDecimal getCountInteraction();
}