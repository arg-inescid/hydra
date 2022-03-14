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

import com.oracle.svm.core.posix.PosixUtils;
import com.oracle.svm.core.posix.headers.Fcntl;
import com.oracle.svm.core.posix.headers.Unistd;
import com.oracle.svm.graalvisor.api.AsGraalVisorHost;
import com.oracle.svm.graalvisor.types.GraalVisorIsolate;
import com.oracle.svm.graalvisor.types.GraalVisorIsolateThread;

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

    private static GraalVisor.GraalVisorStruct graalVisorStructHost;

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

}
