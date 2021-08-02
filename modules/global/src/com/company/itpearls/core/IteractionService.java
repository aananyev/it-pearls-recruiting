package com.company.itpearls.core;

import com.company.itpearls.entity.Iteraction;
import com.haulmont.cuba.security.entity.User;

import java.util.List;

public interface IteractionService {
    String NAME = "itpearls_IteractionService";

    List<Iteraction> getMostPolularIteraction(User user, int maxCount);
}