package com.company.itpearls.core;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;

import java.util.Date;

public interface OpenPositionService {
    String NAME = "itpearls_OpenPositionService";

    void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                             String subject,
                                             String comment,
                                             Date date,
                                             JobCandidate jobCandidate,
                                             ExtUser user,
                                             Boolean priority);

    void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                             String subject,
                                             String comment,
                                             Date date,
                                             ExtUser user);

    OpenPosition createOpenPositionDefault();

    OpenPosition getOpenPositionDefault();
}