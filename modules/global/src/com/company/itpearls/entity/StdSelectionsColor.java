package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum StdSelectionsColor implements EnumClass<String> {

    STAR_RED("star-center-large-red"),
    STAR_YELLOW("star-center-large-orange"),
    STAR_GREEN("star-center-large-green"),
    FLAG_RED("flag-center-large-red"),
    FLAG_YELLOW("flag-center-large-orange"),
    FLAG_GREEN("flag-center-large-green");

    private String id;

    StdSelectionsColor(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static StdSelectionsColor fromId(String id) {
        for (StdSelectionsColor at : StdSelectionsColor.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}