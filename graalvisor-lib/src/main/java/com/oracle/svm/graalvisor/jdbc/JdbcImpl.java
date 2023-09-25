package com.oracle.svm.graalvisor.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.oracle.svm.graalvisor.utils.JsonUtils;

/**
 * This class contains implementations of the JDBC-related methods.
 */
public class JdbcImpl {

    private static final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
    private static final Map<Integer, Statement> statements = new ConcurrentHashMap<>();
    private static final Map<Integer, ResultSet> resultSets = new ConcurrentHashMap<>();

    private static final Map<String, ConcurrentLinkedQueue<Connection>> connectionCache = new ConcurrentHashMap<>(); 

    private static int currentObjectId = 1;

    private synchronized static int nextId() {
        return currentObjectId++;
    }

    public static String invoke(int methodCode, String arguments) {
        MethodIdentifier methodId = MethodIdentifier.fromCode(methodCode);
        Map<String, Object> result = new HashMap<>();
        try {
            String resultData = invokeWrapper(methodId, JsonUtils.jsonToMap(arguments));
            result.put("data", resultData);
        } catch (SQLException | ObjectNotFoundException | UnsupportedOperationException e) {
            System.err.println(e.getMessage());
            result.put("data", "");
            result.put("error", e.getMessage());
        }
        return JsonUtils.mapToJson(result);
    }

    private static String invokeWrapper(MethodIdentifier methodIdentifier, Map<String, Object> args) throws SQLException, ObjectNotFoundException, UnsupportedOperationException {
        String result = null;
        switch (methodIdentifier) {
            case CONNECTION__GET_CONNECTION:
                int connId = hostConnection_getConnection((String) args.get("url"), (String) args.get("user"), (String) args.get("password"));
                result = Integer.toString(connId);
                break;
            case CONNECTION__CREATE_STATEMENT:
                int stmtId = hostConnection_createStatement(Integer.valueOf((String) args.get("connectionId")));
                result = Integer.toString(stmtId);
                break;
            case CONNECTION__CLOSE:
                int exitCode = hostConnection_close(Integer.valueOf((String) args.get("connectionId")));
                result = Integer.toString(exitCode);
                break;
            case CONNECTION__RELEASE:
                exitCode = hostConnection_release(Integer.valueOf((String) args.get("connectionId")));
                result = Integer.toString(exitCode);
                break;
            case STATEMENT__EXECUTE_QUERY:
                int rsId = hostStatement_executeQuery(Integer.valueOf((String) args.get("statementId")), (String) args.get("sql"));
                result = Integer.toString(rsId);
                break;
            case STATEMENT__CLOSE:
                exitCode = hostStatement_close(Integer.valueOf((String) args.get("statementId")));
                result = Integer.toString(exitCode);
                break;
            case RESULT_SET__NEXT:
                int isNext = hostResultSet_next(Integer.valueOf((String) args.get("resultSetId")));
                result = Integer.toString(isNext);
                break;
            case RESULT_SET__GET_INT_INDEX:
                int getIntResult = hostResultSet_getInt(Integer.valueOf((String) args.get("resultSetId")), Integer.valueOf((String) args.get("columnIndex")));
                result = Integer.toString(getIntResult);
                break;
            case RESULT_SET__GET_INT_LABEL:
                getIntResult = hostResultSet_getInt(Integer.valueOf((String) args.get("resultSetId")), (String) args.get("columnLabel"));
                result = Integer.toString(getIntResult);
                break;
            case RESULT_SET__GET_STRING_INDEX:
                result = hostResultSet_getString(Integer.valueOf((String) args.get("resultSetId")), Integer.valueOf((String) args.get("columnIndex")));
                break;
            case RESULT_SET__GET_STRING_LABEL:
                result = hostResultSet_getString(Integer.valueOf((String) args.get("resultSetId")), (String) args.get("columnLabel"));
                break;
            case RESULT_SET__CLOSE:
                exitCode = hostResultSet_close(Integer.valueOf((String) args.get("resultSetId")));
                result = Integer.toString(exitCode);
                break;
            case RESULT_SET_META_DATA__GET_COLUMN_COUNT:
                int columnCount = hostResultSetMetaData_getColumnCount(Integer.valueOf((String) args.get("resultSetId")));
                result = Integer.toString(columnCount);
                break;
            case RESULT_SET_META_DATA__GET_COLUMN_NAME:
                result = hostResultSetMetaData_getColumnName(Integer.valueOf((String) args.get("resultSetId")), Integer.valueOf((String) args.get("column")));
                break;
            default:
                throw new UnsupportedOperationException(methodIdentifier);
        }
        return result;
    }

    private static int hostConnection_getConnection(String url, String user, String password) throws SQLException {
        String mapKey = new StringBuilder(url).append(user).append(password).toString(); 
        Connection conn = DriverManager.getConnection(url, user, password);
        int connId = nextId();
        connections.put(connId, conn);
        return connId;
    }

    private static int hostConnection_createStatement(int connectionId) throws SQLException, ObjectNotFoundException {
        Connection connection = connections.get(connectionId);
        ensureNotNull(connection, connectionId, Connection.class);
        Statement stmt = connection.createStatement();
        int stmtId = nextId();
        statements.put(stmtId, stmt);
        return stmtId;
    }

    private static int hostConnection_close(int connectionId) throws SQLException, ObjectNotFoundException {
        Connection connection = connections.get(connectionId);
        ensureNotNull(connection, connectionId, Connection.class);
        connection.close();
        connections.remove(connectionId);
        return 1;
    }

    private static int hostConnection_release(int connectionId) throws SQLException, ObjectNotFoundException {
        Connection connection = connections.get(connectionId);
        ensureNotNull(connection, connectionId, Connection.class);
        connection.close();
        connections.remove(connectionId);
        return 1;
    }

    private static int hostStatement_executeQuery(int statementId, String sql) throws SQLException, ObjectNotFoundException {
        Statement statement = statements.get(statementId);
        ensureNotNull(statement, statementId, Statement.class);
        ResultSet rs = statement.executeQuery(sql);
        int rsId = nextId();
        resultSets.put(rsId, rs);
        return rsId;
    }

    private static int hostStatement_close(int statementId) throws SQLException, ObjectNotFoundException {
        Statement statement = statements.get(statementId);
        ensureNotNull(statement, statementId, Statement.class);
        statement.close();
        statements.remove(statementId);
        return 1;
    }

    private static int hostResultSet_next(int resultSetId) throws SQLException, ObjectNotFoundException {
        ResultSet rs = resultSets.get(resultSetId);
        ensureNotNull(rs, resultSetId, ResultSet.class);
        return rs.next() ? 1 : 0;
    }

    private static int hostResultSet_getInt(int resultSetId, int columnIndex) throws SQLException, ObjectNotFoundException {
        ResultSet rs = resultSets.get(resultSetId);
        ensureNotNull(rs, resultSetId, ResultSet.class);
        return rs.getInt(columnIndex);
    }

    private static int hostResultSet_getInt(int resultSetId, String columnLabel) throws SQLException, ObjectNotFoundException {
        ResultSet rs = resultSets.get(resultSetId);
        ensureNotNull(rs, resultSetId, ResultSet.class);
        return rs.getInt(columnLabel);
    }

    private static String hostResultSet_getString(int resultSetId, int columnIndex) throws SQLException, ObjectNotFoundException {
        ResultSet rs = resultSets.get(resultSetId);
        ensureNotNull(rs, resultSetId, ResultSet.class);
        return rs.getString(columnIndex);
    }

    private static String hostResultSet_getString(int resultSetId, String columnLabel) throws SQLException, ObjectNotFoundException {
        ResultSet rs = resultSets.get(resultSetId);
        ensureNotNull(rs, resultSetId, ResultSet.class);
        return rs.getString(columnLabel);
    }

    private static int hostResultSet_close(int resultSetId) throws SQLException, ObjectNotFoundException {
        ResultSet rs = resultSets.get(resultSetId);
        ensureNotNull(rs, resultSetId, ResultSet.class);
        rs.close();
        resultSets.remove(resultSetId);
        return 1;
    }

    private static int hostResultSetMetaData_getColumnCount(int resultSetId) throws SQLException, ObjectNotFoundException {
        ResultSet rs = resultSets.get(resultSetId);
        ensureNotNull(rs, resultSetId, ResultSet.class);
        return rs.getMetaData().getColumnCount();
    }

    private static String hostResultSetMetaData_getColumnName(int resultSetId, int column) throws SQLException, ObjectNotFoundException {
        ResultSet rs = resultSets.get(resultSetId);
        ensureNotNull(rs, resultSetId, ResultSet.class);
        return rs.getMetaData().getColumnName(column);
    }

    private static void ensureNotNull(Object object, int objectId, Class<?> klass) throws ObjectNotFoundException {
        if (object == null) {
            throw new ObjectNotFoundException(klass, objectId);
        }
    }

}
