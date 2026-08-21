package com.company.hunttech;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.cuba.testsupport.TestContainer;
import org.dom4j.Document;
import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class HunttechTestContainer extends TestContainer {

    public HunttechTestContainer() {
        super();
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
                "com/company/hunttech/app.properties",
                "com/haulmont/cuba/testsupport/test-app.properties",
                "com/company/hunttech/test-app.properties");
        initDbProperties();
    }

    /**
     * Reads JDBC settings from Tomcat context.xml — the same {@code HuntTech} database as local dev.
     * Integration tests that commit entities must clean up after themselves (see {@link TestEntityTracker}).
     */
    private void initDbProperties() {
        File contextXmlFile = new File("modules/core/web/META-INF/context.xml");
        if (!contextXmlFile.exists()) {
            contextXmlFile = new File("web/META-INF/context.xml");
        }
        if (!contextXmlFile.exists()) {
            throw new RuntimeException("Cannot find 'context.xml' file to read database connection properties. " +
                    "You can set them explicitly in this method.");
        }
        Document contextXmlDoc = Dom4j.readDocument(contextXmlFile);
        Element resourceElem = contextXmlDoc.getRootElement().element("Resource");

        dbDriver = resourceElem.attributeValue("driverClassName");
        dbUrl = resourceElem.attributeValue("url");
        dbUser = resourceElem.attributeValue("username");
        dbPassword = resourceElem.attributeValue("password");
    }

    public static class Common extends HunttechTestContainer {

        public static final HunttechTestContainer.Common INSTANCE = new HunttechTestContainer.Common();

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
            healSystemUsersAfterTests();
            // never stops - do not call super
        }

        private void healSystemUsersAfterTests() {
            try {
                if (INSTANCE.dbUrl != null && INSTANCE.dbUser != null && INSTANCE.dbPassword != null) {
                    try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                            INSTANCE.dbUrl, INSTANCE.dbUser, INSTANCE.dbPassword)) {
                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                "UPDATE sec_user SET delete_ts = NULL, deleted_by = NULL, active = true " +
                                "WHERE login_lc IN ('anonymous', 'admin') AND (delete_ts IS NOT NULL OR active = false)")) {
                            ps.executeUpdate();
                        }
                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                "UPDATE sec_user_role SET delete_ts = NULL, deleted_by = NULL " +
                                "WHERE user_id IN (SELECT id FROM sec_user WHERE login_lc IN ('anonymous', 'admin')) " +
                                "AND delete_ts IS NOT NULL")) {
                            ps.executeUpdate();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Exposes protected {@link #setupContext()} for web-tier performance tests.
     */
    public void activateContext() {
        setupContext();
    }

    /**
     * Exposes protected {@link #cleanupContext()} for web-tier performance tests.
     */
    public void deactivateContext() {
        cleanupContext();
    }
}