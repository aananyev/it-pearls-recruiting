package com.company.hunttech.dto.yandex;

public enum YandexCalendarType {
    PERSONAL("Личный календарь"),
    CLIENT_INTERVIEW("Календарь собеседований с заказчиками");

    private final String caption;

    YandexCalendarType(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }
}
