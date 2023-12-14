package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "itpearls_SomeFilesOpenPosition")
@NamePattern("%s %s|openPosition,fileDescription")
public class SomeFilesOpenPosition extends SomeFiles {
    private static final long serialVersionUID = 4885488578805926985L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    private OpenPosition openPosition;

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }
}