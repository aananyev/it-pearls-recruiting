package com.company.hunttech.core;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.security.entity.User;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface OpenPositionService {
    String NAME = "hunttech_OpenPositionService";

    List<String> getOpenPositionSet();

    List<OpenPosition> getOpenPositionList();

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

    String getOpenPositionCloseShortMessage(OpenPosition entity, User user);

    String getOpenPositionOpenShortMessage(OpenPosition entity, User user);

    String getOpenPositionOpenLongMessage(OpenPosition entity, User user);

    String getOpenPositionCloseLongMessage(OpenPosition entity, User user);
}