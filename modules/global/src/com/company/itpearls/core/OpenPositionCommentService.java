package com.company.itpearls.core;

import com.company.itpearls.entity.OpenPositionComment;
import com.haulmont.cuba.security.entity.User;

public interface OpenPositionCommentService {
    String NAME = "itpearls_OpenPositionCommentService";

    String getOpenPositionCommentMessage(OpenPositionComment entity, User user);

    void publishOpenPositionComment(OpenPositionComment openPositionComment);

    void publishOpenPositionComment(OpenPositionComment openPositionComment, String message);
}