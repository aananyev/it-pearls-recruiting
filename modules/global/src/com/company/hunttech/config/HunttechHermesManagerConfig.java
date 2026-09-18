package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;

/**
 * Конфигурация подключения к Hermes Agent для менеджеров и директоров
 * (профиль hrm-operator в Docker на hr.hunttech.ru).
 */
@Source(type = SourceType.APP)
public interface HunttechHermesManagerConfig extends Config {

    @Property("hunttech.hermes.manager.ssh.enabled")
    @DefaultBoolean(true)
    boolean getSshEnabled();

    @Property("hunttech.hermes.manager.ssh.host")
    @DefaultString("hr.hunttech.ru")
    String getSshHost();

    @Property("hunttech.hermes.manager.ssh.user")
    @DefaultString("root")
    String getSshUser();

    @Property("hunttech.hermes.manager.ssh.port")
    @DefaultInt(22)
    int getSshPort();

    @Property("hunttech.hermes.manager.containerName")
    @DefaultString("hermes-hrm-operator")
    String getContainerName();

    @Property("hunttech.hermes.manager.profile")
    @DefaultString("hrm-operator")
    String getProfile();

    @Property("hunttech.hermes.manager.timeoutSeconds")
    @DefaultInt(240)
    int getTimeoutSeconds();
}
