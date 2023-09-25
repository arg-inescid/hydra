package com.oracle.svm.graalvisor.guestapi.jdbc;

import java.util.HashMap;
import java.util.Map;

import com.oracle.svm.graalvisor.guestapi.GuestAPI;
import com.oracle.svm.graalvisor.jdbc.MethodIdentifier;
import com.oracle.svm.graalvisor.utils.JsonUtils;

public class JdbcGraalvisorAdapter {

    // Connection
    static int connection_getConnection(String url, String user, String password) {
        Map<String, Object> args = new HashMap<>();
        args.put("url", url);
        args.put("user", user);
        args.put("password", password);
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.CONNECTION__GET_CONNECTION.getValue(), JsonUtils.mapToJson(args));
        return Integer.valueOf(tryReadResult(resultString));
    }

    static int connection_createStatement(int connectionId) {
        Map<String, Object> args = new HashMap<>();
        args.put("connectionId", Integer.toString(connectionId));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.CONNECTION__CREATE_STATEMENT.getValue(), JsonUtils.mapToJson(args));
        return Integer.valueOf(tryReadResult(resultString));
    }

    static String connection_close(int connectionId) {
        Map<String, Object> args = new HashMap<>();
        args.put("connectionId", Integer.toString(connectionId));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.CONNECTION__CLOSE.getValue(), JsonUtils.mapToJson(args));
        return tryReadResult(resultString);
    }

    static String custom_connection_release(int connectionId) {
        Map<String, Object> args = new HashMap<>();
        args.put("connectionId", Integer.toString(connectionId));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.CONNECTION__RELEASE.getValue(), JsonUtils.mapToJson(args));
        return tryReadResult(resultString);
    }

    // Statement
    static int statement_executeQuery(int statementId, String sql) {
        Map<String, Object> args = new HashMap<>();
        args.put("statementId", Integer.toString(statementId));
        args.put("sql", sql);
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.STATEMENT__EXECUTE_QUERY.getValue(), JsonUtils.mapToJson(args));
        return Integer.valueOf(tryReadResult(resultString));
    }

    static String statement_close(int statementId) {
        Map<String, Object> args = new HashMap<>();
        args.put("statementId", Integer.toString(statementId));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.STATEMENT__CLOSE.getValue(), JsonUtils.mapToJson(args));
        return tryReadResult(resultString);
    }

    // ResultSet
    static boolean resultSet_next(int resultSetId) {
        Map<String, Object> args = new HashMap<>();
        args.put("resultSetId", Integer.toString(resultSetId));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.RESULT_SET__NEXT.getValue(), JsonUtils.mapToJson(args));
        return Integer.valueOf(tryReadResult(resultString)) == 1;
    }

    static int resultSet_getInt(int resultSetId, int columnIndex) {
        Map<String, Object> args = new HashMap<>();
        args.put("resultSetId", Integer.toString(resultSetId));
        args.put("columnIndex", Integer.toString(columnIndex));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.RESULT_SET__GET_INT_INDEX.getValue(), JsonUtils.mapToJson(args));
        return Integer.valueOf(tryReadResult(resultString));
    }

    static int resultSet_getInt(int resultSetId, String columnLabel) {
        Map<String, Object> args = new HashMap<>();
        args.put("resultSetId", Integer.toString(resultSetId));
        args.put("columnLabel", columnLabel);
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.RESULT_SET__GET_INT_LABEL.getValue(), JsonUtils.mapToJson(args));
        return Integer.valueOf(tryReadResult(resultString));
    }

    static String resultSet_getString(int resultSetId, int columnIndex) {
        Map<String, Object> args = new HashMap<>();
        args.put("resultSetId", Integer.toString(resultSetId));
        args.put("columnIndex", Integer.toString(columnIndex));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.RESULT_SET__GET_STRING_INDEX.getValue(), JsonUtils.mapToJson(args));
        return tryReadResult(resultString);
    }

    static String resultSet_getString(int resultSetId, String columnLabel) {
        Map<String, Object> args = new HashMap<>();
        args.put("resultSetId", Integer.toString(resultSetId));
        args.put("columnLabel", columnLabel);
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.RESULT_SET__GET_STRING_LABEL.getValue(), JsonUtils.mapToJson(args));
        return tryReadResult(resultString);
    }

    static String resultSet_close(int resultSetId) {
        Map<String, Object> args = new HashMap<>();
        args.put("resultSetId", Integer.toString(resultSetId));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.RESULT_SET__CLOSE.getValue(), JsonUtils.mapToJson(args));
        return tryReadResult(resultString);
    }

    // ResultSetMetaData
    static int resultSetMetaData_getColumnCount(int resultSetId) {
        Map<String, Object> args = new HashMap<>();
        args.put("resultSetId", Integer.toString(resultSetId));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.RESULT_SET_META_DATA__GET_COLUMN_COUNT.getValue(), JsonUtils.mapToJson(args));
        return Integer.valueOf(tryReadResult(resultString));
    }

    static String resultSetMetaData_getColumnName(int resultSetId, int column) {
        Map<String, Object> args = new HashMap<>();
        args.put("resultSetId", Integer.toString(resultSetId));
        args.put("column", Integer.toString(column));
        String resultString = GuestAPI.executeDBMethod(MethodIdentifier.RESULT_SET_META_DATA__GET_COLUMN_NAME.getValue(), JsonUtils.mapToJson(args));
        return tryReadResult(resultString);
    }

    // TODO: throw a checked exception such as SQLException?
    private static String tryReadResult(String resultString) {
        if (resultString == null) {
            throw new RuntimeException("resultString is null");
        }
        Map<String, Object> result = JsonUtils.jsonToMap(resultString);
        Object errorResponse = result.get("error");
        if (errorResponse != null) {
            throw new RuntimeException(errorResponse.toString());
        } else {
            return (String) result.get("data");
        }
    }
}
