package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum StdPictures implements EnumClass<String> {

    NO_CANDIDATE("icons/no-programmer.jpeg"),
    NO_COMPANY("icons/no-company.png"),
    HUNTTECH_ICO("icons/hunttech.ico"),
    HUNTTECH_PNG("icons/HuntTech.png");

    private String id;

    StdPictures(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static StdPictures fromId(String id) {
        for (StdPictures at : StdPictures.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}