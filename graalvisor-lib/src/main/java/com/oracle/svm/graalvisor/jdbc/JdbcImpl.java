package com.oracle.svm.graalvisor.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.oracle.svm.graalvisor.utils.JsonUtils;

/**
 * This class contains implementations of the JDBC-related methods.
 */
public class JdbcImpl {

    private static final Map<String, ConcurrentLinkedQueue<Connection>> connectionsCache = new ConcurrentHashMap<>();
    private static final Map<Long, Map<Integer, ConnectionWrapper>> bookedConnections = new ConcurrentHashMap<>();
    private static final Map<Long, Map<Integer, Statement>> bookedStatements = new ConcurrentHashMap<>();
    private static final Map<Long, Map<Integer, ResultSet>> bookedResultSets = new ConcurrentHashMap<>();

    private static int currentObjectId = 1;

    private synchronized static int nextId() {
        return currentObjectId++;
    }

    public static String invoke(long isolateId, int methodCode, String arguments) {
        MethodIdentifier methodId = MethodIdentifier.fromCode(methodCode);
        Map<String, Object> result = new HashMap<>();
        try {
            String resultData = invokeWrapper(isolateId, methodId, JsonUtils.jsonToMap(arguments));
            result.put("data", resultData);
        } catch (SQLException | ObjectNotFoundException | UnsupportedOperationException e) {
            System.err.println(e.getMessage());
            result.put("data", "");
            result.put("error", e.getMessage());
        }
        return JsonUtils.mapToJson(result);
    }

    private static String invokeWrapper(long isolateId, MethodIdentifier methodIdentifier, Map<String, Object> args) throws SQLException, ObjectNotFoundException, UnsupportedOperationException {
        String result = null;
        switch (methodIdentifier) {
            case CONNECTION__GET_CONNECTION:
                int connId = hostConnection_getConnection(isolateId, (String) args.get("url"), (String) args.get("user"), (String) args.get("password"));
                result = Integer.toString(connId);
                break;
            case CONNECTION__CREATE_STATEMENT:
                int stmtId = hostConnection_createStatement(isolateId, Integer.valueOf((String) args.get("connectionId")));
                result = Integer.toString(stmtId);
                break;
            case CONNECTION__CLOSE:
                int exitCode = hostConnection_close(isolateId, Integer.valueOf((String) args.get("connectionId")));
                result = Integer.toString(exitCode);
                break;
            case CONNECTION__RELEASE:
                exitCode = hostConnection_release(isolateId, Integer.valueOf((String) args.get("connectionId")));
                result = Integer.toString(exitCode);
                break;
            case STATEMENT__EXECUTE_QUERY:
                int rsId = hostStatement_executeQuery(isolateId, Integer.valueOf((String) args.get("statementId")), (String) args.get("sql"));
                result = Integer.toString(rsId);
                break;
            case STATEMENT__CLOSE:
                exitCode = hostStatement_close(isolateId, Integer.valueOf((String) args.get("statementId")));
                result = Integer.toString(exitCode);
                break;
            case RESULT_SET__NEXT:
                int isNext = hostResultSet_next(isolateId, Integer.valueOf((String) args.get("resultSetId")));
                result = Integer.toString(isNext);
                break;
            case RESULT_SET__GET_INT_INDEX:
                int getIntResult = hostResultSet_getInt(isolateId, Integer.valueOf((String) args.get("resultSetId")), Integer.valueOf((String) args.get("columnIndex")));
                result = Integer.toString(getIntResult);
                break;
            case RESULT_SET__GET_INT_LABEL:
                getIntResult = hostResultSet_getInt(isolateId, Integer.valueOf((String) args.get("resultSetId")), (String) args.get("columnLabel"));
                result = Integer.toString(getIntResult);
                break;
            case RESULT_SET__GET_STRING_INDEX:
                result = hostResultSet_getString(isolateId, Integer.valueOf((String) args.get("resultSetId")), Integer.valueOf((String) args.get("columnIndex")));
                break;
            case RESULT_SET__GET_STRING_LABEL:
                result = hostResultSet_getString(isolateId, Integer.valueOf((String) args.get("resultSetId")), (String) args.get("columnLabel"));
                break;
            case RESULT_SET__CLOSE:
                exitCode = hostResultSet_close(isolateId, Integer.valueOf((String) args.get("resultSetId")));
                result = Integer.toString(exitCode);
                break;
            case RESULT_SET_META_DATA__GET_COLUMN_COUNT:
                int columnCount = hostResultSetMetaData_getColumnCount(isolateId, Integer.valueOf((String) args.get("resultSetId")));
                result = Integer.toString(columnCount);
                break;
            case RESULT_SET_META_DATA__GET_COLUMN_NAME:
                result = hostResultSetMetaData_getColumnName(isolateId, Integer.valueOf((String) args.get("resultSetId")), Integer.valueOf((String) args.get("column")));
                break;
            default:
                throw new UnsupportedOperationException(methodIdentifier);
        }
        return result;
    }

    private static int hostConnection_getConnection(long guestIsolateId, String url, String user, String password) throws SQLException {
        String mapKey = new StringBuilder(url).append(user).append(password).toString();
        Connection connection = null;
        Queue<Connection> cache = connectionsCache.get(mapKey);
        if (cache != null) {
            // Try cached version first.
            connection = cache.poll();
        }
        if (connection == null) {
            // Cache miss.
            connection = DriverManager.getConnection(url, user, password);
        }
        int connectionId = nextId();
        addBooked(bookedConnections, guestIsolateId, connectionId, new ConnectionWrapper(connection, mapKey));
        return connectionId;
    }

    private static int hostConnection_createStatement(long guestIsolateId, int connectionId) throws SQLException, ObjectNotFoundException {
        Connection connection = ensureNotNull(bookedConnections, guestIsolateId, connectionId, Connection.class).getConnection();
        Statement stmt = connection.createStatement();
        int stmtId = nextId();
        addBooked(bookedStatements, guestIsolateId, stmtId, stmt);
        return stmtId;
    }

    private static int hostConnection_close(long guestIsolateId, int connectionId) throws SQLException, ObjectNotFoundException {
        Connection connection = ensureNotNull(bookedConnections, guestIsolateId, connectionId, Connection.class).getConnection();
        connection.close();
        bookedConnections.get(guestIsolateId).remove(connectionId);
        return 1;
    }

    private static int hostConnection_release(long guestIsolateId, int connectionId) throws SQLException, ObjectNotFoundException {
        ConnectionWrapper cw = ensureNotNull(bookedConnections, guestIsolateId, connectionId, Connection.class);
        bookedConnections.get(guestIsolateId).remove(connectionId);
        String mapKey = cw.getMapKey();
        connectionsCache.computeIfAbsent(mapKey, k -> new ConcurrentLinkedQueue<>());
        connectionsCache.get(mapKey).add(cw.getConnection());
        return 1;
    }

    private static int hostStatement_executeQuery(long guestIsolateId, int statementId, String sql) throws SQLException, ObjectNotFoundException {
        Statement statement = ensureNotNull(bookedStatements, guestIsolateId, statementId, Statement.class);
        ResultSet rs = statement.executeQuery(sql);
        int rsId = nextId();
        addBooked(bookedResultSets, guestIsolateId, rsId, rs);
        return rsId;
    }

    private static int hostStatement_close(long guestIsolateId, int statementId) throws SQLException, ObjectNotFoundException {
        Statement statement = ensureNotNull(bookedStatements, guestIsolateId, statementId, Statement.class);
        statement.close();
        bookedStatements.get(guestIsolateId).remove(statementId);
        return 1;
    }

    private static int hostResultSet_next(long guestIsolateId, int resultSetId) throws SQLException, ObjectNotFoundException {
        ResultSet rs = ensureNotNull(bookedResultSets, guestIsolateId, resultSetId, ResultSet.class);
        return rs.next() ? 1 : 0;
    }

    private static int hostResultSet_getInt(long guestIsolateId, int resultSetId, int columnIndex) throws SQLException, ObjectNotFoundException {
        ResultSet rs = ensureNotNull(bookedResultSets, guestIsolateId, resultSetId, ResultSet.class);
        return rs.getInt(columnIndex);
    }

    private static int hostResultSet_getInt(long guestIsolateId, int resultSetId, String columnLabel) throws SQLException, ObjectNotFoundException {
        ResultSet rs = ensureNotNull(bookedResultSets, guestIsolateId, resultSetId, ResultSet.class);
        return rs.getInt(columnLabel);
    }

    private static String hostResultSet_getString(long guestIsolateId, int resultSetId, int columnIndex) throws SQLException, ObjectNotFoundException {
        ResultSet rs = ensureNotNull(bookedResultSets, guestIsolateId, resultSetId, ResultSet.class);
        return rs.getString(columnIndex);
    }

    private static String hostResultSet_getString(long guestIsolateId, int resultSetId, String columnLabel) throws SQLException, ObjectNotFoundException {
        ResultSet rs = ensureNotNull(bookedResultSets, guestIsolateId, resultSetId, ResultSet.class);
        return rs.getString(columnLabel);
    }

    private static int hostResultSet_close(long guestIsolateId, int resultSetId) throws SQLException, ObjectNotFoundException {
        ResultSet rs = ensureNotNull(bookedResultSets, guestIsolateId, resultSetId, ResultSet.class);
        rs.close();
        bookedResultSets.get(guestIsolateId).remove(resultSetId);
        return 1;
    }

    private static int hostResultSetMetaData_getColumnCount(long guestIsolateId, int resultSetId) throws SQLException, ObjectNotFoundException {
        ResultSet rs = ensureNotNull(bookedResultSets, guestIsolateId, resultSetId, ResultSet.class);
        return rs.getMetaData().getColumnCount();
    }

    private static String hostResultSetMetaData_getColumnName(long guestIsolateId, int resultSetId, int column) throws SQLException, ObjectNotFoundException {
        ResultSet rs = ensureNotNull(bookedResultSets, guestIsolateId, resultSetId, ResultSet.class);
        return rs.getMetaData().getColumnName(column);
    }

    private static <T> T ensureNotNull(Map<Long, Map<Integer, T>> bookedCollection, long isolateId, int objectId, Class<?> klass) throws ObjectNotFoundException {
        Map<Integer, T> map = bookedCollection.get(isolateId);
        if (map == null) {
            throw new ObjectNotFoundException(isolateId, klass);
        }
        T cw = map.get(objectId);
        if (cw == null) {
            throw new ObjectNotFoundException(klass, objectId);
        }
        return cw;
    }

    private static <T> void addBooked(Map<Long, Map<Integer, T>> bookedCollection, long isolateId, int objectId, T object) {
        bookedCollection.computeIfAbsent(isolateId, k -> new HashMap<>());
        bookedCollection.get(isolateId).put(objectId, object);
    }
}
