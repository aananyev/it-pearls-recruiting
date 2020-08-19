package com.company.itpearls.web.screens.iteractionlist;

import com.haulmont.cuba.gui.screen.ScreenOptions;

public class IteracionListScreenOptions implements ScreenOptions {
    private Boolean noSubscribers;

    IteracionListScreenOptions(Boolean noSubs) {
        this.noSubscribers = noSubs;
    }

    public Boolean getNoSubscribers() {
        return noSubscribers;
    }
}
