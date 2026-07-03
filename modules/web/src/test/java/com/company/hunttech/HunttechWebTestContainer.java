package com.company.hunttech;

import com.haulmont.cuba.web.testsupport.TestContainer;

import java.util.Arrays;

/**
 * Web-tier test container for HRM HuntTech UI integration tests.
 */
public class HunttechWebTestContainer extends TestContainer {

    public HunttechWebTestContainer() {
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
                "com.haulmont.bpm");
        appPropertiesFiles = Arrays.asList(
                "com/company/hunttech/web-app.properties",
                "com/haulmont/cuba/web/testsupport/test-web-app.properties");
    }

    public static class Common extends HunttechWebTestContainer {

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
        }
    }
}
