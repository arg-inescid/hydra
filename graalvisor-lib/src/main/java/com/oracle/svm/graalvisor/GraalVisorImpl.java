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
import com.oracle.svm.graalvisor.utils.JdbcUtils;

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

    private static GraalVisor.GraalVisorStruct graalVisorStructHost;

    private static final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
    private static int currentConnectionId = 1;

    public synchronized static GraalVisor.GraalVisorStruct getGraalVisorHostDescriptor() {
        if (graalVisorStructHost.isNull()) {
            /* Note that malloc can only be invoked during runtime! */
            graalVisorStructHost = malloc(SizeOf.get(GraalVisor.GraalVisorStruct.class));
            graalVisorStructHost.setHostReceiveStringFunction(hostReceiveStringFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostObtainDBConnectionFunction(hostObtainDBConnectionFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostExecuteDBQueryFunction(hostExecuteDBQueryFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostReturnDBConnectionFunction(hostReturnDBConnectionFunctionPointer.getFunctionPointer());
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
            synchronized (connections) {
                connections.put(currentConnectionId, conn);
                return currentConnectionId++;
            }
        } catch (SQLException e) {
            System.err.println("Failed to obtain a DB connection.");
            e.printStackTrace();
            return 0;
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static CCharPointer hostExecuteDBQuery(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, int connectionId, CCharPointer query) {
        Connection conn = connections.get(connectionId);
        String result = "";
        if (conn == null) {
            System.err.println("Failed to obtain a DB connection with id: " + connectionId);
        } else {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(CTypeConversion.toJavaString(query));
                result = JdbcUtils.resultSetToString(rs);
            } catch (SQLException e) {
                System.err.println("Failed to execute the query.");
            }
        }
        return CTypeConversion.toCString(result).get();
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
}
