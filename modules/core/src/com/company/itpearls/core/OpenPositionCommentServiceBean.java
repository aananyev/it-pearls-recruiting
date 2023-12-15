package com.company.itpearls.core;

import com.company.itpearls.entity.OpenPositionComment;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

@Service(OpenPositionCommentService.NAME)
public class OpenPositionCommentServiceBean implements OpenPositionCommentService {

    @Override
    public String getOpenPositionCommentMessage(OpenPositionComment entity, User user) {
        StringBuilder sb = new StringBuilder();

        sb.append("Опубликован комментарий к вакансии ")
                .append(entity.getOpenPosition().getVacansyName())
                .append("<br><br>")
                .append("\n\n")
                .append(entity.getComment())
                .append("<br><br>")
                .append("\n\n")
                .append("<br><svg align=\"right\" width=\"100%\"><i>")
                .append(user.getName())
                .append("</i></svg>");

        return sb.toString();
    }

}