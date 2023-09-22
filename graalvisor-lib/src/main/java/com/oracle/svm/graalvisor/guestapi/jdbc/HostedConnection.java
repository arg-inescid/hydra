package com.oracle.svm.graalvisor.guestapi.jdbc;

import com.oracle.svm.graalvisor.guestapi.GuestAPI;

public class HostedConnection implements AutoCloseable {

    private final int id;

    private HostedConnection(String connectionUrl, String username, String password) {
        this.id = GuestAPI.obtainDBConnection(connectionUrl, username, password);
    }

    public static HostedConnection getConnection(String dbUrl, String username, String password) {
        return new HostedConnection(dbUrl, username, password);
    }

    public HostedStatement createStatement() {
        return new HostedStatement(this.id);
    }

    @Override
    public void close() {
        GuestAPI.returnDBConnection(this.id);
    }

    public static class HostedStatement {

        private final int connectionId;

        private HostedStatement(int connectionId) {
            this.connectionId = connectionId;
        }

        public HostedResultSet executeQuery(String sql) {
            int resultSetId = GuestAPI.executeDBQuery(connectionId, sql);
            return new HostedResultSet(resultSetId);
        }
    }

    public static class HostedResultSet {

        private final int id;

        private HostedResultSet(int id) {
            this.id = id;
        }

        public HostedResultSetMetaData getMetaData() {
            return new HostedResultSetMetaData(this.id);
        }

        public boolean next() {
            return GuestAPI.resultSetNext(id);
        }

        public int getInt(int columnIndex) {
            return GuestAPI.resultSetGetInt(id, columnIndex);
        }

        public int getInt(String columnLabel) {
            return GuestAPI.resultSetGetInt(id, columnLabel);
        }

        public String getString(int columnIndex) {
            return GuestAPI.resultSetGetString(id, columnIndex);
        }

        public String getString(String columnLabel) {
            return GuestAPI.resultSetGetString(id, columnLabel);
        }
    }

    public static class HostedResultSetMetaData {

        private final int resultSetId;

        private HostedResultSetMetaData(int resultSetId) {
            this.resultSetId = resultSetId;
        }

        public int getColumnCount() {
            return GuestAPI.resultSetMetaDataGetColumnCount(resultSetId);
        }

        public String getColumnName(int column) {
            return "";
        }

    }
}
