package com.haulmont.studio.db.custom

import javax.annotation.Nullable
import java.sql.SQLException

/**
 * This class is used by Studio at design time for working with Custom Database.
 * It defines custom database properties that are needed to configure projects using Custom Database.
 */
// TODO this is an auto-generated sample suitable for MS SQLServer
@SuppressWarnings("GroovyUnusedDeclaration")
class CustomDbProperties {

    /**
     * Database type identifier.
     * <p>
     * It must be unique and differ from the predefined types: hsql, postgres, mssql, oracle, mysql
     * and all other custom types.
     * <p>
     * The identifier is used in cuba.dbmsType property and for generating support classes.
     */
    String getId() {
        return 'custom'
    }

    /**
     * Database type caption.
     * <p>
     * It is used only in Studio to display the database type to the user.
     */
    String getCaption() {
        return 'Custom Database'
    }

    /**
     * Default database user.
     * <p>
     * It is set to the 'Database user' field when a user select this database type.
     */
    String getUser() {
        return 'sa'
    }

    /**
     * Default database password.
     * <p>
     * It is set to the 'Database password' field when a user select this database type.
     */
    String getPassword() {
        return 'saPass1'
    }

    /**
     * Fully qualified name of the JDBC driver.
     */
    String getDriver() {
        return 'net.sourceforge.jtds.jdbc.Driver'
    }

    /**
     * Starting part of JDBC connection URL connection before host.
     */
    String getUrlPrefix() {
        return 'jdbc:jtds:sqlserver://'
    }

    /**
     * JDBC driver coordinates in Maven repository (group:artifact:version) or local JAR file.
     * <p>
     * For instance:
     * <ul>
     *   <li>Postgres - <code>org.postgresql:postgresql:9.4.1212</code>
     *   <li>MySQL - <code>mysql:mysql-connector-java:5.1.46</code>
     *   <li>Oracle - <code>files("$cuba.tomcat.dir/lib/ojdbc6.jar")</code>
     * </ul>
     */
    String getJdbcDriverDependency() {
        return 'net.sourceforge.jtds:jtds:1.3.1'
    }

    /**
     * Whether to wrap jdbcDriverDependency into quotes in build.gradle methods.
     * For example, if your {@link #getJdbcDriverDependency()} method returns <code>files(...)</code>,
     * the quotes are not needed.
     */
    boolean isDependencyInQuotes() {
        return true
    }

    /**
     * Login timeout for java.sql.DriverManager#getConnection(java.lang.String, java.util.Properties).
     * If < 0, this property is not used.
     */
    int getLoginTimeout() {
        return 5
    }

    /**
     * Connection validation query or null if not needed.
     */
    @Nullable
    String getValidationQuery() {
        return 'select 1'
    }

    /**
     * Initialize patterns for changing {@code build.gradle} that are used when Studio switches the project
     * to another DB type.
     *
     * @param patternInfo object containing the following properties (all of them have default implementations):
     * <ul>
     *     <li>prevDriverVarPattern - regex to search for a variable defining JDBC driver dependency in build.gradle.
     *     Default: "def\s+" + id + "\s*=\s*'.*?'"
     *
     *     <li>prevJdbcDepPattern - regex to search for 'jdbc' dependency in build.gradle.
     *     Default: "jdbc\s*?\(\s*?" + id + "\s*?\)"
     *
     *     <li>prevTestRuntimePattern - regex to search for 'testRuntime' dependency in build.gradle.
     *     Default: "testRuntime\s*?\(\s*?" + id + "\s*?\)"
     *
     *     <li>currDriverVar - JDBC driver variable definition.
     *     Default: "def " + id + " = '" + jdbcDriverDependency() + "'"
     *
     *     <li>currJdbcDep - 'jdbc' dependency.
     *     Default: "jdbc(" + id + ")" + currJdbcDepConf
     *
     *     <li>currTestRuntime - 'testRuntime' dependency.
     *     Default: "testRuntime(" + id + ")" + currJdbcDepConf
     * </ul>
     */
    void initBuildScriptChangePatterns(def patternInfo) {
    }

    /**
     * 'jdbc' dependency config
     */
    String getJdbcDriverDependencyConfig() {
        return ''
    }

    /**
     * Database schema.
     *
     * @param connectionParams an object providing connection parameters by name
     */
    String getSchema(def connectionParams) {
        String currentSchema = connectionParams.getConnectionParam("currentSchema")
        return currentSchema?: "DBO"
    }

    /**
     * Create a JDBC connection URL.
     *
     * @param host database host
     * @param dbName database name
     * @param connectionParams connection parameters
     * @return connection URL
     */
    String createDbUrl(String host, String dbName, String connectionParams) {
        return "${getUrlPrefix()}${host}/${dbName}${connectionParams}"
    }

    /**
     * Return true if the exception thrown on connection attempt contains information that the database does not exist.
     *
     * @param sqlException connection exception
     * @return true if the exception is caused due to the database does not exist
     */
    boolean isNonexistentDatabaseException(SQLException sqlException) {
        return "3D000".equals(sqlException.getSQLState()) || sqlException.getErrorCode() == 4060
    }

    /**
     * Create a URL to connect to master database.
     *
     * @param host database host
     * @param dbName database name
     * @param connectionParams connection parameters
     * @return connection URL
     */
    String getMasterUrl(String host, String dbName, String connectionParams) {
        return "jdbc:jtds:sqlserver://$host/master$connectionParams"
    }

    /**
     * This method is used to determine what database is used in the project.
     *
     * @param url JDBC connection URL from context.xml
     * @return true if the URL corresponds to this database type
     */
    boolean isSuitableUrl(String url) {
        return url.startsWith("jdbc:jtds:sqlserver:")
    }

    /**
     * Convert table and column names to the case in which they are stored in database metadata.
     */
    String convertDbIdentifierCase(String identifier) {
        return identifier
    }

    /**
     * Extracts the part of connection URL containing connection parameters.
     */
    @Nullable
    String getConnectionParams(String url) {
        if (!url)
            return null
        String subsLastSlash = substringWithLastSlash(url)
        if (!subsLastSlash) {
            return ''
        }
        def m = subsLastSlash =~ /[^?;,\\]*([?;,\\].*)/
        if (m.find()) {
            return m.group(1)
        }
        return ''
    }

    /**
     * Get database host from the given URL.
     * @param url database URL
     * @return database host
     */
    @Nullable
    String getHost(String url) {
        if (!url)
            return null
        int slash = url.lastIndexOf('/')
        if (slash <= 0)
            return ""
        String beforeName = url.substring(0, slash)
        slash = beforeName.lastIndexOf('/')
        if (slash <= 0)
            return ''
        return beforeName.substring(slash + 1)
    }

    /**
     * Get database name from the given URL.
     * @param url database URL
     * @return database name
     */
    String getDatabaseName(String url) {
        String subsLastSlash = substringWithLastSlash(url)
        if (!subsLastSlash) {
            return ''
        }
        String[] splitDbName = subsLastSlash.split(/[?;,\\]/)
        return splitDbName.length == 0 ? '' : splitDbName[0]
    }

    /**
     * Returns "drop database" statement.
     *
     * @param dbName    database name
     * @return "drop database" statement
     */
    String getDropDatabaseStatement(def dbName) {
        return "drop database $dbName;"
    }

    /**
     * Returns "create database" statement.
     *
     * @param dbName    database name
     * @return "create database" statement
     */
    String getCreateDatabaseStatement(def dbName) {
        return "create database $dbName;"
    }

    /**
     * Returns column type corresponding to {@link javax.persistence.TemporalType#TIMESTAMP}.
     *
     * @return column type
     */
    String getTimestampType() {
        return 'DATETIME'
    }

    private String substringWithLastSlash(String url) {
        if (!url) {
            return null
        }
        int slash = url.lastIndexOf('/')
        if (slash <= 0) {
            return null
        }
        return url.substring(slash + 1)
    }
}
