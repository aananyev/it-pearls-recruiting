package com.company.hunttech;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.cuba.testsupport.TestContainer;
import org.dom4j.Document;
import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

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
     * Reads the JDBC driver from context.xml and resolves the database profile from
     * environment variables. Integration tests never use PRODUCTION and must clean
     * up entities they create (see {@link TestEntityTracker}).
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
        String profile = System.getenv().getOrDefault("HUNTTECH_DB_PROFILE", "LOCAL").trim().toUpperCase(Locale.ROOT);
        if ("PRODUCTION".equals(profile)) {
            throw new IllegalStateException("Integration tests cannot use the PRODUCTION database profile");
        }
        if ("LOCAL".equals(profile)) {
            String localHost = envOrDefault("HUNTTECH_LOCAL_DB_HOST", "192.168.1.135");
            if (!"192.168.1.135".equals(localHost)) {
                throw new IllegalStateException("LOCAL profile host must be 192.168.1.135");
            }
            dbUrl = jdbcUrl(
                    localHost,
                    validatedPort("HUNTTECH_LOCAL_DB_PORT", envOrDefault("HUNTTECH_LOCAL_DB_PORT", "5432")),
                    validatedToken("HUNTTECH_LOCAL_DB_NAME", envOrDefault("HUNTTECH_LOCAL_DB_NAME", "hunttech")));
            dbUser = validatedToken("HUNTTECH_LOCAL_DB_USER", envOrDefault("HUNTTECH_LOCAL_DB_USER", "cuba"));
            dbPassword = requiredEnv("HUNTTECH_LOCAL_DB_PASSWORD");
        } else if ("TEST".equals(profile)) {
            String testHost = requiredEnv("HUNTTECH_TEST_DB_HOST");
            String normalizedTestHost = testHost.trim().toLowerCase(Locale.ROOT);
            if (normalizedTestHost.endsWith(".")) {
                normalizedTestHost = normalizedTestHost.substring(0, normalizedTestHost.length() - 1);
            }
            String comparisonHost = normalizedTestHost;
            if (comparisonHost.startsWith("[") && comparisonHost.endsWith("]")) {
                comparisonHost = comparisonHost.substring(1, comparisonHost.length() - 1);
            }
            if (comparisonHost.startsWith("0.") || comparisonHost.matches("^0[0-9].*")
                    || comparisonHost.matches(".*\\.0[0-9].*")) {
                throw new IllegalStateException("TEST profile must not use ambiguous leading-zero host notation");
            }
            if (!comparisonHost.matches("[a-z0-9._-]+")) {
                throw new IllegalStateException("TEST profile host contains unsupported characters");
            }
            if (comparisonHost.equals("192.168.1.135") || comparisonHost.equals("127.0.0.1")
                    || comparisonHost.startsWith("127.") || comparisonHost.equals("localhost")
                    || comparisonHost.startsWith("localhost.") || comparisonHost.equals("0.0.0.0")
                    || comparisonHost.matches("[0-9]+")) {
                throw new IllegalStateException("TEST profile must use an explicitly separate database host");
            }
            dbUrl = jdbcUrl(comparisonHost,
                    validatedPort("HUNTTECH_TEST_DB_PORT", requiredEnv("HUNTTECH_TEST_DB_PORT")),
                    validatedToken("HUNTTECH_TEST_DB_NAME", requiredEnv("HUNTTECH_TEST_DB_NAME")));
            dbUser = validatedToken("HUNTTECH_TEST_DB_USER", requiredEnv("HUNTTECH_TEST_DB_USER"));
            dbPassword = requiredEnv("HUNTTECH_TEST_DB_PASSWORD");
        } else {
            throw new IllegalStateException("Unknown HUNTTECH_DB_PROFILE: " + profile);
        }
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty() || value.contains("\n") || value.contains("\r")) {
            throw new IllegalStateException(name + " must be supplied outside Git without newlines");
        }
        return value.trim();
    }

    private static String validatedToken(String name, String value) {
        if (!value.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalStateException(name + " contains unsupported characters");
        }
        return value;
    }

    private static String validatedPort(String name, String value) {
        if (!value.matches("[0-9]{1,5}")) {
            throw new IllegalStateException(name + " must be a TCP port");
        }
        int port = Integer.parseInt(value);
        if (port < 1 || port > 65535) {
            throw new IllegalStateException(name + " must be a TCP port");
        }
        return value;
    }

    private static String jdbcUrl(String host, String port, String database) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
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
            } catch (Exception e) {
                System.err.println("[HunttechTestContainer] Failed to heal system users (anonymous/admin): " + e.getMessage());
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
