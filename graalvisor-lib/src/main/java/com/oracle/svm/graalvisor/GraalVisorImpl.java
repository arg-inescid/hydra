package com.oracle.svm.graalvisor;

import static org.graalvm.nativeimage.UnmanagedMemory.malloc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CEntryPointLiteral;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import com.oracle.svm.graalvisor.api.AsGraalVisorHost;

public class GraalVisorImpl {

    private static final CEntryPointLiteral<GraalVisor.HostReceiveStringFunctionPointer> hostReceiveStringFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostReceiveString",
                    IsolateThread.class, CCharPointer.class);

    private static final CEntryPointLiteral<GraalVisor.HostObtainDBConnectionFunctionPointer> hostObtainDBConnectionFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostObtainDBConnection",
                    IsolateThread.class, CCharPointer.class, CCharPointer.class, CCharPointer.class);

    private static final CEntryPointLiteral<GraalVisor.HostExecuteDBQueryFunctionPointer> hostExecuteDBQueryFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostExecuteDBQuery",
                    IsolateThread.class, int.class, CCharPointer.class);

    private static final CEntryPointLiteral<GraalVisor.HostReturnDBConnectionFunctionPointer> hostReturnDBConnectionFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostReturnDBConnection",
                    IsolateThread.class, int.class);

    private static final CEntryPointLiteral<GraalVisor.HostResultSetNextFunctionPointer> hostResultSetNextFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostResultSetNext",
                    IsolateThread.class, int.class);

    private static final CEntryPointLiteral<GraalVisor.HostResultSetGetIntIndexFunctionPointer> hostResultSetGetIntIndexFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostResultSetGetIntIndex",
                    IsolateThread.class, int.class, int.class);

    private static final CEntryPointLiteral<GraalVisor.HostResultSetGetIntLabelFunctionPointer> hostResultSetGetIntLabelFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostResultSetGetIntLabel",
                    IsolateThread.class, int.class, CCharPointer.class);

    private static final CEntryPointLiteral<GraalVisor.HostResultSetGetStringIndexFunctionPointer> hostResultSetGetStringIndexFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostResultSetGetStringIndex",
                    IsolateThread.class, int.class, int.class);

    private static final CEntryPointLiteral<GraalVisor.HostResultSetGetStringLabelFunctionPointer> hostResultSetGetStringLabelFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostResultSetGetStringLabel",
                    IsolateThread.class, int.class, CCharPointer.class);

    private static final CEntryPointLiteral<GraalVisor.HostResultSetMetaDataGetColumnCountFunctionPointer> hostResultSetMetaDataGetColumnCountFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostResultSetMetaDataGetColumnCount",
                    IsolateThread.class, int.class);

    private static final CEntryPointLiteral<GraalVisor.HostResultSetMetaDataGetColumnNameFunctionPointer> hostResultSetMetaDataGetColumnNameFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostResultSetMetaDataGetColumnName",
                    IsolateThread.class, int.class, int.class);

    private static GraalVisor.GraalVisorStruct graalVisorStructHost;

    private static final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
    private static final Map<Integer, ResultSet> resultSets = new ConcurrentHashMap<>();
    private static int currentObjectId = 1;

    private synchronized static int nextId() {
        return currentObjectId++;
    }

    public synchronized static GraalVisor.GraalVisorStruct getGraalVisorHostDescriptor() {
        if (graalVisorStructHost.isNull()) {
            /* Note that malloc can only be invoked during runtime! */
            graalVisorStructHost = malloc(SizeOf.get(GraalVisor.GraalVisorStruct.class));
            graalVisorStructHost.setHostReceiveStringFunction(hostReceiveStringFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostObtainDBConnectionFunction(hostObtainDBConnectionFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostExecuteDBQueryFunction(hostExecuteDBQueryFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostReturnDBConnectionFunction(hostReturnDBConnectionFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostResultSetNextFunction(hostResultSetNextFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostResultSetGetIntIndexFunction(hostResultSetGetIntIndexFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostResultSetGetIntLabelFunction(hostResultSetGetIntLabelFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostResultSetGetStringIndexFunction(hostResultSetGetStringIndexFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostResultSetGetStringLabelFunction(hostResultSetGetStringLabelFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostResultSetMetaDataGetColumnCountFunction(hostResultSetMetaDataGetColumnCountFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostResultSetMetaDataGetColumnNameFunction(hostResultSetMetaDataGetColumnNameFunctionPointer.getFunctionPointer());
        }
        return graalVisorStructHost;
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static ObjectHandle hostReceiveString(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, CCharPointer cString) {
        String targetString = CTypeConversion.toJavaString(cString);
        return ObjectHandles.getGlobal().create(targetString);
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostObtainDBConnection(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, CCharPointer сConnectionUrl, CCharPointer сUser, CCharPointer cPassword) {
        String connectionUrl = CTypeConversion.toJavaString(сConnectionUrl);
        String user = CTypeConversion.toJavaString(сUser);
        String password = CTypeConversion.toJavaString(cPassword);
        try {
            Connection conn = DriverManager.getConnection(connectionUrl, user, password);
            int connId = nextId();
            connections.put(connId, conn);
            return connId;
        } catch (SQLException e) {
            System.err.println("Failed to obtain a DB connection.");
            e.printStackTrace();
            return 0;
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostExecuteDBQuery(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int connectionId, CCharPointer query) {
        Connection conn = connections.get(connectionId);
        if (conn == null) {
            System.err.println("Failed to obtain a DB connection with id: " + connectionId);
            return 0;
        } else {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(CTypeConversion.toJavaString(query));
                int rsId = nextId();
                resultSets.put(rsId, rs);
                return rsId;
            } catch (SQLException e) {
                System.err.println("Failed to execute the query.");
                return 0;
            }
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostReturnDBConnection(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int connectionId) {
        Connection conn = connections.get(connectionId);
        if (conn == null) {
            System.err.println("No DB connection found with id: " + connectionId);
            return 0;
        }
        try {
            conn.close();
            connections.remove(connectionId);
        } catch (SQLException e) {
            System.err.println("Failed to close DB connection with id: " + connectionId);
            return 0;
        }
        return 1;
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostResultSetNext(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int resultSetId) {
        ResultSet rs = resultSets.get(resultSetId);
        if (rs == null) {
            System.err.println("No ResultSet found with id: " + resultSetId);
            return 0;
        }
        try {
            return rs.next() ? 1 : 0;
        } catch (SQLException e) {
            System.err.println("Failed to invoke ResultSet::next with id: " + resultSetId);
            return 0;
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostResultSetGetIntIndex(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int resultSetId, int columnIndex) {
        ResultSet rs = resultSets.get(resultSetId);
        if (rs == null) {
            System.err.println("No ResultSet found with id: " + resultSetId);
            return 0;
        }
        try {
            return rs.getInt(columnIndex);
        } catch (SQLException e) {
            System.err.println("Failed to invoke ResultSet::getInt with id: " + resultSetId);
            return 0;
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostResultSetGetIntLabel(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int resultSetId, CCharPointer cColumnLabel) {
        String columnLabel = CTypeConversion.toJavaString(cColumnLabel);
        ResultSet rs = resultSets.get(resultSetId);
        if (rs == null) {
            System.err.println("No ResultSet found with id: " + resultSetId);
            return 0;
        }
        try {
            return rs.getInt(columnLabel);
        } catch (SQLException e) {
            System.err.println("Failed to invoke ResultSet::getInt with id: " + resultSetId);
            return 0;
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static CCharPointer hostResultSetGetStringIndex(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int resultSetId, int columnIndex) {
        ResultSet rs = resultSets.get(resultSetId);
        String result = "";
        if (rs == null) {
            System.err.println("No ResultSet found with id: " + resultSetId);
        } else {
            try {
                result = rs.getString(columnIndex);
            } catch (SQLException e) {
                System.err.println("Failed to invoke ResultSet::getString with id: " + resultSetId);
            }
        }
        return CTypeConversion.toCString(result).get();
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static CCharPointer hostResultSetGetStringLabel(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int resultSetId, CCharPointer cColumnLabel) {
        String columnLabel = CTypeConversion.toJavaString(cColumnLabel);
        ResultSet rs = resultSets.get(resultSetId);
        String result = "";
        if (rs == null) {
            System.err.println("No ResultSet found with id: " + resultSetId);
        } else {
            try {
                result = rs.getString(columnLabel);
            } catch (SQLException e) {
                System.err.println("Failed to invoke ResultSet::getString with id: " + resultSetId);
            }
        }
        return CTypeConversion.toCString(result).get();
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostResultSetMetaDataGetColumnCount(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int resultSetId) {
        ResultSet rs = resultSets.get(resultSetId);
        if (rs == null) {
            System.err.println("No ResultSet found with id: " + resultSetId);
            return 0;
        }
        try {
            return rs.getMetaData().getColumnCount();
        } catch (SQLException e) {
            System.err.println("Failed to invoke ResultSetMetaData::getColumnCount with id: " + resultSetId);
            return 0;
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static CCharPointer hostResultSetMetaDataGetColumnName(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int resultSetId, int column) {
        ResultSet rs = resultSets.get(resultSetId);
        String result = "";
        if (rs == null) {
            System.err.println("No ResultSet found with id: " + resultSetId);
        } else {
            try {
                result = rs.getMetaData().getColumnName(column);
            } catch (SQLException e) {
                System.err.println("Failed to invoke ResultSetMetaData::getColumnName with id: " + resultSetId);
            }
        }
        return CTypeConversion.toCString(result).get();
    }
}
