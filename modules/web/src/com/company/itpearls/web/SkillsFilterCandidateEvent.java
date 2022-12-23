package com.company.itpearls.web;

import com.haulmont.cuba.gui.events.UiEvent;
import com.haulmont.cuba.security.entity.User;
import org.springframework.context.ApplicationEvent;

public class SkillsFilterCandidateEvent extends ApplicationEvent implements UiEvent {
    private User user;

    public SkillsFilterCandidateEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
