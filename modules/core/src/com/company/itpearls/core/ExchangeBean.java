package com.company.itpearls.core;

import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import org.springframework.stereotype.Component;

@Component(ExchangeBean.NAME)
public class ExchangeBean {
    public static final String NAME = "itpearls_ExchangeBean";

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