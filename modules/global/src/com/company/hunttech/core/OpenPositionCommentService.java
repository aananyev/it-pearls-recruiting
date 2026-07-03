package com.company.hunttech.core;

import com.company.hunttech.entity.OpenPositionComment;
import com.haulmont.cuba.security.entity.User;

public interface OpenPositionCommentService {
    String NAME = "hunttech_OpenPositionCommentService";

    String getOpenPositionCommentMessage(OpenPositionComment entity, User user);
}