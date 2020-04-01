package com.haulmont.cuba.core.sys.persistence;

import java.sql.*;
import java.util.Date;
import java.util.UUID;

/**
 * DbTypeConverter implementation for Custom Database.
 */
// TODO this is an auto-generated sample suitable for MS SQLServer
@SuppressWarnings("unused")
public class CustomDbTypeConverter implements DbTypeConverter {

    @Override
    public Object getJavaObject(ResultSet resultSet, int columnIndex) {
        Object value;

        try {
            ResultSetMetaData metaData = resultSet.getMetaData();

            if ((columnIndex > metaData.getColumnCount()) || (columnIndex <= 0))
                throw new IndexOutOfBoundsException("Column index out of bound");

            int sqlType = metaData.getColumnType(columnIndex);
            String typeName = metaData.getColumnTypeName(columnIndex);

            switch (sqlType) {
                case Types.CHAR:
                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                case Types.CLOB:
                    if ("uniqueidentifier".equals(typeName)) {
                        String stringValue = resultSet.getString(columnIndex);
                        value = stringValue != null ? UUID.fromString(stringValue) : null;
                    } else {
                        value = resultSet.getObject(columnIndex);
                    }
                    break;

                default:
                    value = resultSet.getObject(columnIndex);
                    break;
            }

            return value;
        } catch (SQLException e) {
            throw new RuntimeException("Error converting database value", e);
        }
    }

    @Override
    public Object getSqlObject(Object value) {
        if (value instanceof Date)
            return new Timestamp(((Date) value).getTime());
        if (value instanceof UUID)
            return value.toString();
        if (value instanceof Boolean)
            return ((Boolean) value) ? 1 : 0;
        return value;
    }

    @Override
    public int getSqlType(Class<?> javaClass) {
        if (javaClass == Date.class)
            return Types.TIMESTAMP;
        else if (javaClass == UUID.class)
            return Types.VARCHAR;
        else if (javaClass == Boolean.class)
            return Types.BIT;
        else if (javaClass == String.class)
            return Types.VARCHAR;
        else if (javaClass == Integer.class)
            return Types.INTEGER;
        else if (javaClass == Long.class)
            return Types.BIGINT;
        return Types.OTHER;
    }
}