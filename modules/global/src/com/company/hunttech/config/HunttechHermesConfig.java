package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;

/**
 * Конфигурация подключения к Hermes Agent (профиль hrm-viewer в Docker на hr.hunttech.ru).
 */
@Source(type = SourceType.APP)
public interface HunttechHermesConfig extends Config {

    @Property("hunttech.hermes.ssh.enabled")
    @DefaultBoolean(true)
    boolean getSshEnabled();

    @Property("hunttech.hermes.ssh.host")
    @DefaultString("hr.hunttech.ru")
    String getSshHost();

    @Property("hunttech.hermes.ssh.user")
    @DefaultString("root")
    String getSshUser();

    @Property("hunttech.hermes.ssh.port")
    @DefaultInt(22)
    int getSshPort();

    @Property("hunttech.hermes.containerName")
    @DefaultString("hermes-hrm-viewer")
    String getContainerName();

    @Property("hunttech.hermes.profile")
    @DefaultString("hrm-viewer")
    String getProfile();

    @Property("hunttech.hermes.timeoutSeconds")
    @DefaultInt(90)
    int getTimeoutSeconds();
}
