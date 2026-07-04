package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultString;

@Source(type = SourceType.APP)
public interface HunttechTelegramConfig extends Config {

    // ОБНОВЛЕНИЕ: имя Telegram-бота читается из app.properties через CUBA Config вместо передачи hardcode в LongPollingBot.
    @Property("hunttech.telegram.botName")
    @DefaultString("")
    String getBotName();

    // ОБНОВЛЕНИЕ: токен Telegram-бота читается из app.properties через CUBA Config вместо хранения значения в коде.
    @Property("hunttech.telegram.botToken")
    @DefaultString("")
    String getBotToken();
}
