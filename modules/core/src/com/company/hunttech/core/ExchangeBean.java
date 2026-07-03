package com.company.hunttech.core;

import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import org.springframework.stereotype.Component;

@Component(ExchangeBean.NAME)
public class ExchangeBean {
    public static final String NAME = "hunttech_ExchangeBean";

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