package com.company.itpearls.web.screens.jobcandidate;

import com.haulmont.cuba.gui.screen.ScreenOptions;

public class JobCandidateScreenOptions implements ScreenOptions {
    private Boolean noSubscribers;

    JobCandidateScreenOptions(Boolean noSubs) {
        this.noSubscribers = noSubs;
    }

    public Boolean getNoSubscribers() {
        return noSubscribers;
    }
}
