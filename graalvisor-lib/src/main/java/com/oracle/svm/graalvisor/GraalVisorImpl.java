package com.oracle.svm.graalvisor;

import static org.graalvm.nativeimage.UnmanagedMemory.malloc;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.Isolates.IsolateException;
import org.graalvm.nativeimage.Isolates.ProtectionDomain;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CEntryPointLiteral;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.WordFactory;

import com.oracle.svm.core.c.function.CEntryPointCreateIsolateParameters;
import com.oracle.svm.core.os.MemoryProtectionProvider;
import com.oracle.svm.core.os.MemoryProtectionProvider.UnsupportedDomainException;
import com.oracle.svm.graalvisor.api.AsGraalVisorHost;

public class GraalVisorImpl {

    private static final CEntryPointLiteral<GraalVisor.HostReceiveStringFunctionPointer> hostReceiveStringFunctionPointer = CEntryPointLiteral.create(
                    GraalVisorImpl.class,
                    "hostReceiveString",
                    IsolateThread.class, CCharPointer.class);

    private static GraalVisor.GraalVisorStruct graalVisorStructHost;
    private static CEntryPointCreateIsolateParameters createIsolateParametersStructHost;

    public synchronized static GraalVisor.GraalVisorStruct getGraalVisorHostDescriptor() {
        if (graalVisorStructHost.isNull()) {
            /* Note that malloc can only be invoked during runtime! */
            graalVisorStructHost = malloc(SizeOf.get(GraalVisor.GraalVisorStruct.class));
            graalVisorStructHost.setHostReceiveStringFunction(hostReceiveStringFunctionPointer.getFunctionPointer());
        }
        return graalVisorStructHost;
    }

    public synchronized static CEntryPointCreateIsolateParameters getCreateIsolateParametersHostDescriptor() {
        if (createIsolateParametersStructHost.isNull()) {
            /* Note that malloc can only be invoked during runtime! */
            createIsolateParametersStructHost = malloc(SizeOf.get(CEntryPointCreateIsolateParameters.class));

            /* Set null values for aux image options. */
            createIsolateParametersStructHost.setAuxiliaryImagePath(WordFactory.nullPointer());
            createIsolateParametersStructHost.setAuxiliaryImageReservedSpaceSize(WordFactory.nullPointer());

            /* Set struct version to 3 to use v3 fields. */
            createIsolateParametersStructHost.setVersion(3);

            /* Set default protection key. */
            if (MemoryProtectionProvider.isAvailable()) {
                try {
                    int pkey = MemoryProtectionProvider.singleton().asProtectionKey(ProtectionDomain.NO_DOMAIN);
                    createIsolateParametersStructHost.setProtectionKey(pkey);
                } catch (UnsupportedDomainException e) {
                    throw new IsolateException(e.getMessage());
                }
            }

            /* Prepare isolate parameters. */
            String[] args = {"-XX:+PrintGC", "-XX:+VerboseGC", "-XX:+PrintGCSummary", "-XX:+PrintHeapShape", "-XX:MinHeapSize=33554432", "-XX:MaxHeapSize=268435456"};
            int argc = args.length;
            createIsolateParametersStructHost.setArgc(argc);
            createIsolateParametersStructHost.setArgv(WordFactory.nullPointer());

            if (argc > 0) {
                CCharPointerPointer argv = UnmanagedMemory.malloc(SizeOf.unsigned(CCharPointerPointer.class).multiply(argc));
                for (int i = 0; i < argc; i++) {
                    argv.write(i, CTypeConversion.toCString(args[i]).get());
                }
                createIsolateParametersStructHost.setArgv(argv);
            }
        }
        return createIsolateParametersStructHost;
    }

    @SuppressWarnings("unused")
    @CEntryPoint(include = AsGraalVisorHost.class)
    private static ObjectHandle hostReceiveString(@CEntryPoint.IsolateThreadContext IsolateThread hostThread, CCharPointer cString) {
        String targetString = CTypeConversion.toJavaString(cString);
        return ObjectHandles.getGlobal().create(targetString);
    }
}
