package com.company.itpearls.core;

import com.company.itpearls.entity.Iteraction;
import com.haulmont.cuba.security.entity.User;

import java.util.List;

public interface InteractionService {
    String NAME = "itpearls_InteractionService";

    List<Iteraction> getMostPolularIteraction(User user, int maxCount);
}