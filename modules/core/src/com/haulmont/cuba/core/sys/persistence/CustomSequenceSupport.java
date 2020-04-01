package com.haulmont.cuba.core.sys.persistence;

/**
 * SequenceSupport implementation for Custom Database.
 */
// TODO this is an auto-generated sample suitable for MS SQLServer
@SuppressWarnings("unused")
public class CustomSequenceSupport implements SequenceSupport {

    @Override
    public String sequenceExistsSql(String sequenceName) {
        return "select NAME from SYS.SEQUENCES where NAME = '" + sequenceName.toUpperCase() + "'";
    }

    @Override
    public String createSequenceSql(String sequenceName, long startValue, long increment) {
        return "create sequence " + sequenceName.toUpperCase()
                + " as bigint increment by " + increment + " start with " + startValue + " minvalue 0";
    }

    @Override
    public String modifySequenceSql(String sequenceName, long startWith) {
        return "alter sequence " + sequenceName.toUpperCase()
                + " restart with " + startWith;
    }

    @Override
    public String deleteSequenceSql(String sequenceName) {
        return "drop sequence " + sequenceName.toUpperCase();
    }

    @Override
    public String getNextValueSql(String sequenceName) {
        return "select next value for " + sequenceName.toUpperCase();
    }

    @Override
    public String getCurrentValueSql(String sequenceName) {
        return "select cast(CURRENT_VALUE as bigint) from SYS.SEQUENCES where NAME = '" + sequenceName.toUpperCase() + "'";
    }
}