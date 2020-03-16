package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.screen.ScreenOptions;

public class ExchangeData implements ScreenOptions {

    JobCandidate candidate;
    OpenPosition openPosition;

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

}
