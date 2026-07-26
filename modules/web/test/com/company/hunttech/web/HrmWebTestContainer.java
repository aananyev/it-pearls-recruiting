package com.company.hunttech.web;

import com.haulmont.cuba.web.testsupport.TestContainer;

import java.util.Arrays;

/**
 * Общий Spring-контейнер web integration тестов HRM HuntTech.
 * Состав app components повторяет modules/web/web/WEB-INF/web.xml.
 */
public class HrmWebTestContainer extends TestContainer {

    public HrmWebTestContainer() {
        appComponents = Arrays.asList(
                "com.haulmont.cuba",
                "com.haulmont.addon.globalevents",
                "com.haulmont.addon.emailtemplates",
                "de.diedavids.cuba.dataimport",
                "com.haulmont.addon.dashboard",
                "com.haulmont.addon.helium",
                "com.haulmont.fts",
                "com.haulmont.charts",
                "com.haulmont.reports",
                "com.haulmont.bpm"
        );
        appPropertiesFiles = Arrays.asList(
                "com/company/hunttech/web-app.properties",
                "com/haulmont/cuba/web/testsupport/test-web-app.properties"
        );
    }

    public static class Common extends HrmWebTestContainer {

        public static final Common INSTANCE = new Common();
        private static volatile boolean initialized;

        private Common() {
        }

        @Override
        public void before() throws Throwable {
            if (!initialized) {
                super.before();
                initialized = true;
            }
            setupContext();
        }

        @Override
        public void after() {
            cleanupContext();
            // Общий контейнер не останавливается между тестами, чтобы не пересоздавать Spring context.
        }
    }
}
