package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultString;

@Source(type = SourceType.APP)
public interface HunttechAccountingBotConfig extends Config {

    @Property("hunttech.accountingBot.allowedTelegramUserId")
    @DefaultString("")
    String getAllowedTelegramUserId();

    @Property("hunttech.accountingBot.yandexDiskRootPath")
    @DefaultString("")
    String getYandexDiskRootPath();

    @Property("hunttech.accountingBot.incomingScansPath")
    @DefaultString("")
    String getIncomingScansPath();
}
