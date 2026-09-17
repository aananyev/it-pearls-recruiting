package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;

/**
 * Конфигурация подключения к Hermes Agent (профиль hrm-operator в Docker на hr.hunttech.ru).
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
    @DefaultString("hermes-hrm-operator")
    String getContainerName();

    @Property("hunttech.hermes.profile")
    @DefaultString("hrm-operator")
    String getProfile();

    @Property("hunttech.hermes.timeoutSeconds")
    @DefaultInt(90)
    int getTimeoutSeconds();

    @Property("hunttech.hermes.viewer.containerName")
    @DefaultString("hermes-hrm-viewer")
    String getViewerContainerName();

    @Property("hunttech.hermes.viewer.profile")
    @DefaultString("hrm-viewer")
    String getViewerProfile();

    @Property("hunttech.hermes.viewer.timeoutSeconds")
    @DefaultInt(120)
    int getViewerTimeoutSeconds();
}
