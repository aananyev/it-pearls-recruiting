package com.company.itpearls.service;

import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.security.entity.User;

import java.util.Date;

public interface OpenPositionNewsService {
    String NAME = "itpearls_OpenPositionNewsService";

    public void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                                     String subject,
                                                     String comment,
                                                     Date date,
                                                     JobCandidate jobCandidate,
                                                     User user,
                                                     Boolean priority);
}