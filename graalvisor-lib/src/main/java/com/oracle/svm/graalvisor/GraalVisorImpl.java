package com.oracle.svm.graalvisor;

import static com.oracle.svm.graalvisor.utils.file.FileAccessModeUtils.DEFAULT_PERMISSIONS;
import static org.graalvm.nativeimage.UnmanagedMemory.malloc;

import java.io.FileDescriptor;

import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CEntryPointLiteral;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordFactory;

import com.oracle.svm.core.posix.PosixUtils;
import com.oracle.svm.core.posix.headers.Fcntl;
import com.oracle.svm.core.posix.headers.Unistd;
import com.oracle.svm.graalvisor.api.AsGraalVisorHost;
import com.oracle.svm.graalvisor.types.GraalVisorIsolate;
import com.oracle.svm.graalvisor.types.GraalVisorIsolateThread;
import com.oracle.svm.graalvisor.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GraalVisorImpl {

    private static final CEntryPointLiteral<GraalVisor.HostReceiveStringFunctionPointer> hostReceiveStringFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostReceiveString",
                    GraalVisorIsolateThread.class, CCharPointer.class);
    private static final CEntryPointLiteral<GraalVisor.HostOpenFileFunctionPointer> hostOpenFileFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostOpenFile",
                    GraalVisorIsolateThread.class, CCharPointer.class, int.class);
    private static final CEntryPointLiteral<GraalVisor.HostCloseFileFunctionPointer> hostCloseFileFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostCloseFile",
                    GraalVisorIsolateThread.class, int.class);
    private static final CEntryPointLiteral<GraalVisor.HostWriteBytesFunctionPointer> hostWriteBytesFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostWriteBytes",
                    GraalVisorIsolateThread.class, int.class, CCharPointer.class, UnsignedWord.class);
    private static final CEntryPointLiteral<GraalVisor.HostReadBytesFunctionPointer> hostReadBytesFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostReadBytes",
                    GraalVisorIsolateThread.class, int.class, CCharPointer.class, int.class, int.class);
    private static final CEntryPointLiteral<GraalVisor.HostOpenDBConnectionFunctionPointer> hostOpenDBConnectionFunctionPointer = CEntryPointLiteral.create(
		            GraalVisorImpl.class,
		            "hostOpenDBConnection",
		            GraalVisorIsolateThread.class, CCharPointer.class, CCharPointer.class, CCharPointer.class);
    private static final CEntryPointLiteral<GraalVisor.HostExecuteDBQueryFunctionPointer> hostExecuteDBQueryFunctionPointer = CEntryPointLiteral.create(
		            GraalVisorImpl.class,
		            "hostExecuteDBQuery",
		            GraalVisorIsolateThread.class, int.class, CCharPointer.class, CCharPointer.class, int.class);
    private static final CEntryPointLiteral<GraalVisor.HostCloseDBConnectionFunctionPointer> hostCloseDBConnectionFunctionPointer = CEntryPointLiteral.create(
		            GraalVisorImpl.class,
		            "hostCloseDBConnection",
		            GraalVisorIsolateThread.class, int.class);

    private static GraalVisor.GraalVisorStruct graalVisorStructHost;

    private static final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
    private static int currentConnectionId = 1;

    public synchronized static GraalVisor.GraalVisorStruct getGraalVisorHostDescriptor() {
        if (graalVisorStructHost.isNull()) {
            /* Note that malloc can only be invoked during runtime! */
            graalVisorStructHost = malloc(SizeOf.get(GraalVisor.GraalVisorStruct.class));
            graalVisorStructHost.setHostIsolate((GraalVisorIsolate) CurrentIsolate.getIsolate());
            graalVisorStructHost.setHostReceiveStringFunction(hostReceiveStringFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostOpenFileFunction(hostOpenFileFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostCloseFileFunction(hostCloseFileFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostWriteBytesFunction(hostWriteBytesFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostReadBytesFunction(hostReadBytesFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostOpenDBConnectionFunction(hostOpenDBConnectionFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostExecuteDBQueryFunction(hostExecuteDBQueryFunctionPointer.getFunctionPointer());
            graalVisorStructHost.setHostCloseDBConnectionFunction(hostCloseDBConnectionFunctionPointer.getFunctionPointer());
        }
        return graalVisorStructHost;
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static ObjectHandle hostReceiveString(GraalVisorIsolateThread hostThread, CCharPointer cString) {
        String targetString = CTypeConversion.toJavaString(cString);
        return ObjectHandles.getGlobal().create(targetString);
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostOpenFile(GraalVisorIsolateThread hostThread, CCharPointer fileName, int accessMode) {
        return Fcntl.NoTransitions.open(fileName, accessMode, DEFAULT_PERMISSIONS);
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static int hostCloseFile(GraalVisorIsolateThread hostThread, int fd) {
        return Unistd.NoTransitions.close(fd);
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    public static int hostWriteBytes(GraalVisorIsolateThread hostThread, int fd, CCharPointer bytes, UnsignedWord length) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        PosixUtils.setFD(fileDescriptor, fd);
        boolean res = PosixUtils.writeBytes(fileDescriptor, bytes, length);
        if (!res)
            System.out.println("Fail to write bytes to file");
        PosixUtils.flush(fileDescriptor);
        Unistd.fsync(fd);
        return res ? 1 : 0;
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    public static int hostReadBytes(GraalVisorIsolateThread hostThread, int fd, CCharPointer buffer, int bufferLen, int readOffset) {
        return PosixUtils.readBytes(fd, buffer, bufferLen, readOffset);
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    public static int hostOpenDBConnection(GraalVisorIsolateThread hostThread, CCharPointer сConnectionUrl, CCharPointer сUser, CCharPointer cPassword) {
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
            System.out.println("Fail to create DB connection");
            e.printStackTrace();
            return 0;
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    public static int hostExecuteDBQuery(GraalVisorIsolateThread hostThread, int connectionId, CCharPointer query, CCharPointer buffer, int bufferLen) {
        Connection conn = connections.get(connectionId);
        if (conn == null) {
            System.out.println("Fail to obtain DB connection with id: " + connectionId);
            return 0;
        }
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(CTypeConversion.toJavaString(query));
            String result = JdbcUtils.resultSetToString(rs);
            CTypeConversion.toCString(result, buffer, WordFactory.unsigned(bufferLen));
        } catch (SQLException e) {
            System.out.println("Fail to execute the query");
            return 0;
        }
        return 1;
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    public static int hostCloseDBConnection(GraalVisorIsolateThread hostThread, int connectionId) {
        Connection conn = connections.get(connectionId);
        if (conn == null) {
            System.out.println("No DB connection found with id: " + connectionId);
            return 0;
        }
        try {
            conn.close();
            connections.remove(connectionId);
        } catch (SQLException e) {
            System.out.println("Fail to close DB connection with id: " + connectionId);
            return 0;
        }
        return 1;
    }

}
