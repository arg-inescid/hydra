package com.oracle.svm.graalvisor.guestapi.jdbc;

public class HostedConnection implements AutoCloseable {

    private final int id;

    private HostedConnection(String url, String user, String password) {
        this.id = JdbcGraalvisorAdapter.connection_getConnection(url, user, password);
    }

    public static HostedConnection getConnection(String url, String user, String password) {
        return new HostedConnection(url, user, password);
    }

    public HostedStatement createStatement() {
        int statementId = JdbcGraalvisorAdapter.connection_createStatement(this.id);
        return new HostedStatement(statementId);
    }

    @Override
    public void close() {
        JdbcGraalvisorAdapter.connection_close(this.id);
    }

    /**
     * Release the connection, but do not close it. The connection can be reused later.
     * This method is custom, i.e., it does not appear in the original java.sql.Connection class.
     */
    public void release() {
        JdbcGraalvisorAdapter.custom_connection_release(this.id);
    }

    @Override
    protected void finalize() {
        JdbcGraalvisorAdapter.custom_connection_release(this.id);
    }

    public static class HostedStatement implements AutoCloseable {

        private final int id;

        private HostedStatement(int connectionId) {
            this.id = connectionId;
        }

        public HostedResultSet executeQuery(String sql) {
            int resultSetId = JdbcGraalvisorAdapter.statement_executeQuery(this.id, sql);
            return new HostedResultSet(resultSetId);
        }

        @Override
        public void close() {
            JdbcGraalvisorAdapter.statement_close(this.id);
        }

        @Override
        protected void finalize() {
            JdbcGraalvisorAdapter.statement_close(this.id);
        }
    }

    public static class HostedResultSet implements AutoCloseable {

        private final int id;

        private HostedResultSet(int id) {
            this.id = id;
        }

        public HostedResultSetMetaData getMetaData() {
            return new HostedResultSetMetaData(this.id);
        }

        public boolean next() {
            return JdbcGraalvisorAdapter.resultSet_next(this.id);
        }

        public int getInt(int columnIndex) {
            return JdbcGraalvisorAdapter.resultSet_getInt(this.id, columnIndex);
        }

        public int getInt(String columnLabel) {
            return JdbcGraalvisorAdapter.resultSet_getInt(this.id, columnLabel);
        }

        public String getString(int columnIndex) {
            return JdbcGraalvisorAdapter.resultSet_getString(this.id, columnIndex);
        }

        public String getString(String columnLabel) {
            return JdbcGraalvisorAdapter.resultSet_getString(this.id, columnLabel);
        }

        @Override
        public void close() {
            JdbcGraalvisorAdapter.resultSet_close(this.id);
        }

        @Override
        protected void finalize() {
            JdbcGraalvisorAdapter.resultSet_close(this.id);
        }
    }

    public static class HostedResultSetMetaData {

        private final int resultSetId;

        private HostedResultSetMetaData(int resultSetId) {
            this.resultSetId = resultSetId;
        }

        public int getColumnCount() {
            return JdbcGraalvisorAdapter.resultSetMetaData_getColumnCount(this.resultSetId);
        }

        public String getColumnName(int column) {
            return JdbcGraalvisorAdapter.resultSetMetaData_getColumnName(this.resultSetId, column);
        }

    }
}
