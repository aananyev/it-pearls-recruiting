package com.haulmont.studio.db.custom

import javax.annotation.Nullable
import java.sql.DatabaseMetaData
import java.sql.SQLException

/**
 * Contains helper methods and default implementations.
 * <p>
 * An object of this type is available in each *DdlGenerator class.
 * You don't have to implement this interface, it has an implementation in Studio.
 */
interface DdlGeneratorDelegate {

    /*******************************************************************************************************************
                                                   Helper methods
     ******************************************************************************************************************/

    /**
     * Returns project namespace.
     */
    String getProjectNamespace()

    /**
     * Returns the database name of the main data store.
     */
    String getMainDataStoreDatabaseName()

    /**
     * Returns database schema of the main data store.
     */
    String getDefaultSchema()

    /**
     * Returns the JDBC connection to the main data store.
     */
    def getConnection()

    /**
     * Returns the given column name wrapped in quotes if it must be used in quotes due to spaces, mixed case, etc.
     *
     * @param column column name
     * @return  the same or wrapped name
     */
    String wrapColumnInQuotesIfNeeded(String column)

    /**
     * Returns a column definition with column name, type, parameters and 'not null' clause.
     *
     * @param entity    entity object
     * @param attribute attribute object
     * @param column    column name
     * @param notNull   true if the column is 'not null'
     * @return column definition
     */
    String getColumnDefinition(def entity, def attribute, String column, boolean notNull)

    /**
     * Processes DDL script adding a "not null" column.
     *
     * @param script        script
     * @param entity        entity object
     * @param attribute     attribute object
     * @param columnName    column name
     */
    void processScriptAddingMandatoryColumn(StringBuilder script, def entity, def attribute, String columnName)

    /**
     * Returns "add column" statements for embeddable attributes. Adds all attributes (including nested) except those
     * contained in "added" parameter.
     *
     * @param entity      root entity, which table must be altered
     * @param attribute   embeddable attribute
     * @param added       attributes that have already been added
     * @return alter table statements
     */
    String getAddColumnsForEmbeddedAttrStatements(def entity, def attribute, Collection added)

    /**
     * Returns column length by parsing {@code length} attribute of the {@code @Column} annotation.
     * Searches all static variables in the class and its base classes.
     *
     * @param entity    entity object
     * @param attribute attribute object
     * @return  length or null if there is no {@code length} attribute
     */
    Integer getColumnLength(def entity, def attribute)

    /**
     * Returns the column name in uppercase or get it from entity attribute.
     *
     * @param attribute     attribute object. It is used only if the second parameter is null.
     * @param columnName    column name
     * @return  column name
     */
    String getColumnName(def attribute, String columnName)

    /**
     * Returns string with parameters for creating a decimal column.
     *
     * @param entity    entity object
     * @param attribute attribute object
     * @return parameters string
     */
    String getDecimalParams(def entity, def attribute)

    /**
     * Returns column name of id attribute.
     * If the attribute represents a composite PK then returns column names separated by commas.
     *
     * @param entity entity object
     * @return column name
     */
    String getPrimaryKeyColumn(def entity)

    /**
     * Returns type of ID attribute:
     * <ul>
     * <li> UUID
     * <li> String
     * <li> Integer
     * <li> Long
     * <li> LongIdentity
     * <li> IntegerIdentity
     * <li> Embedded
     * <li> Unknown
     * <li> null
     * </ul>
     * @param entity entity object
     * @return type string
     */
    String getIdType(def entity)

    /**
     * Returns ID attribute of the given entity.
     *
     * @param entity entity object
     * @return attribute object
     */
    Object getIdAttribute(def entity)

    /**
     * Returns column length for enum attributes.
     * Default value is 50, it can be overridden in Studio settings.
     *
     * @return column length
     */
    String getDefaultEnumColumnLength()

    /**
     * Adds precision and scale params for BigDecimal attribute.
     *
     * @param attribute attribute object
     * @param params    params string
     */
    void processBigDecimalParams(def attribute, StringBuilder params)


    /*******************************************************************************************************************
                                                  Default implementations
     ******************************************************************************************************************/

    /**
     * Returns names of unique and non-unique indexes for a given column.
     *
     * @param metaData      java.sql.DatabaseMetaData object
     * @param schema        database schema
     * @param tableName     table name
     * @param columnName    column name
     * @return collection of index names
     */
    Collection<String> defaultGetColumnIndexNames(DatabaseMetaData metaData, String schema,
                                                  String tableName, String columnName)

    /**
     * Returns names of constraints for a given column
     * obtaining them from {@link java.sql.DatabaseMetaData#getImportedKeys}
     *
     * @param metaData      java.sql.DatabaseMetaData object
     * @param schema        database schema
     * @param tableName     table name
     * @param columnName    column name
     * @return collection of constraint names
     */
    Collection<String> defaultGetColumnConstraintNames(def metaData, String schema,
                                                       String tableName, String columnName)

    /**
     * Returns the list of statements for dropping all constraints for a column.
     * Uses {@code getColumnIndexNames} and {@code getColumnConstraintNames} methods.
     *
     * @param tableName     table name
     * @param columnName    column name
     * @return list of statements
     */
    List<String> defaultGetDropColumnConstraintStatements(String tableName, String columnName)

    /**
     * Returns column name in upper case if it does not contain quotes.
     *
     * @param columnName
     * @return column name
     */
    String defaultPrepareColumnForAddScript(String columnName)

    /**
     * Returns column type for a given attribute from {@code types} collection.
     *
     * @param attributeType attribute type name
     * @return column type
     */
    String defaultGetColumnType(String attributeType)

    /**
     * For a BigDecimal attribute, returns true if the scale of the attribute differs from the scale of the
     * database column:
     * <pre>columnScale != attributeScale && (columnScale > -84 || attributeScale == 0)</pre>
     *
     * @param attributeScale    attribute scale
     * @param columnScale       column scale
     * @param columnType        column type
     * @return true/false
     */
    boolean defaultIsScaleDifferent(int attributeScale, int columnScale, String columnType)

    /**
     * Returns a statement for primary key definition:
     * <pre>primary key (\${primaryKeyColumn})</pre>
     *
     * @param entity entity object
     * @return a statement
     */
    String defaultGetPrimaryKeyStatement(def entity)


    /**
     * Returns a statement for making an existing column a primary key like
     * <pre>alter table \${tableName} add constraint PK_\${tableName} primary key (\${idColumnName}) ^</pre>
     *
     * @param entity   entity object
     * @param notNull  whether to add "not null" to the column
     * @return a statement
     */
    String defaultGetPrimaryKeyConstraintStatement(def entity, boolean notNull)

    /**
     * Returns true if the current column type is different and not compatible with the old one.
     * The default implementation searches in {@code typeSynonyms} collection:
     * <pre>
     * return !getColumnTypeSynonyms(oldType).contains(currentType)
     * </pre>
     *
     * @param attribute     attribute object
     * @param currentType   current column type
     * @param oldType       old column type
     * @param oldLength     old column length
     * @return true/false
     */
    boolean defaultIsTypeDifferent(def attribute, String currentType, String oldType, int oldLength)

    /**
     * Returns the part of a primary or foreign key column definition containing parameters
     * and (optional) "not null" clause.
     * Parameters are enclosed in parentheses.
     *
     * @param attribute attribute object
     * @return parameters string
     */
    String defaultGetPrimaryOrForeignKeyParams(def attribute)

    /**
     * For the given column type, returns 'DATE', 'TIME' or 'TIMESTAMP' strings indicating what attribute
     * type should be used for the column.
     * Default implementation searches in the {@code temporalTypes} collection by columnType or by it's first
     * synonym from {@code typeSynonyms} collection.
     *
     * @param columnType    column type
     * @return 'DATE', 'TIME', 'TIMESTAMP' or null
     */
    String defaultGetTemporalType(String columnType)

    /**
     * Returns column type for a given attribute if it is a primary or foreign key.
     * Default implementation finds column type in {@code types} collection.
     *
     * @param attributeTypeName name of the attribute type, e.g. "String", "Integer", "UUID", etc.
     * @return column type for a primary or foreign key
     */
    String defaultGetPrimaryOrForeignKeyType(def attribute)

    /**
     * Replace delimiter '^' and '^^' in ddl/sql script.
     * Default implementation: Replace ^ -> ; ^^ -> ^
     *
     * @param script ddl/sql script
     * @return ddl/sql script with replaced delimiters
     */
    String defaultReplaceDelimiter(String script)

    /**
     * Returns names of constraints for a given table
     * obtaining them from {@link java.sql.DatabaseMetaData#getExportedKeys}
     *
     * @param metaData      java.sql.DatabaseMetaData object
     * @param schema        database schema
     * @param tableName     table name
     * @return collection of constraint names
     * @throws SQLException
     */
    Collection<String> defaultGetDropTableConstraintNames(
            DatabaseMetaData metaDatam, String schema, String tableName) throws SQLException

    /**
     * @return max length of constraints and indexes identifier. Return -1 if length is unlimited
     */
    Integer getIdentifierMaxLength()

    /**
     * Given notification will be shown as tray notification and printed in the console.
     * If you want part of description text not to be printed in console, wrap it in
     * <noconsole>Text that should be shown only in tray notification</noconsole> custom tag
     *
     * @param caption The message caption
     * @param description The message description
     */
    void showNotification(String caption, @Nullable String description)
}