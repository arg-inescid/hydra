package com.oracle.svm.graalvisor.api;

import static com.oracle.svm.core.posix.headers.Dlfcn.RTLD_NOW;
import static com.oracle.svm.core.posix.headers.Dlfcn.dlclose;
import static com.oracle.svm.graalvisor.GraalVisorImpl.getGraalVisorHostDescriptor;
import static com.oracle.svm.graalvisor.utils.StringUtils.retrieveString;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

import com.oracle.svm.core.c.function.CEntryPointCreateIsolateParameters;
import com.oracle.svm.core.c.function.CEntryPointNativeFunctions;
import com.oracle.svm.core.posix.PosixUtils;
import com.oracle.svm.graalvisor.GraalVisor;
import com.oracle.svm.graalvisor.types.GraalVisorIsolateThread;
import com.oracle.svm.graalvisor.types.GuestIsolateThread;

@SuppressWarnings("unused")
public class GraalVisorAPI implements Closeable {

    private static final int RTLD_DEEPBIND = 0x00008;

    private final PointerBase dlHandle;
    private final GuestInstallGraalvisorFunctionPointer guestInstallGraalvisorFunctionPointer;
    private final InvokeFunctionPointer invokeFunctionPointer;
    private final GuestReceiveStringFunctionPointer guestReceiveStringFunctionPointer;
    private final CreateIsolateFunctionPointer createIsolateFunctionPointer;
    private final TearDownIsolateFunctionPointer tearDownIsolateFunctionPointer;

    /**
     * Load .so into memory, find the function pointers of the exposed guest apis
     *
     * @param soName file name of dynamically-linked library built from guest application
     * @throws FileNotFoundException No corresponding file found in LD_LIBRARY_PATH
     */
    public GraalVisorAPI(String soName) throws FileNotFoundException {
        dlHandle = PosixUtils.dlopen(soName, RTLD_NOW() | RTLD_DEEPBIND);
        if (dlHandle.rawValue() == 0) {
            System.err.println(soName + " hasn't been found, please check the file name or your environment variable LD_LIBRARY_PATH.");
            throw new FileNotFoundException(soName + " hasn't been found, please check the file name or your environment variable LD_LIBRARY_PATH.");
        }
        /* register native function using their function pointer inside so */
        guestInstallGraalvisorFunctionPointer = PosixUtils.dlsym(dlHandle, "guest_install_graalvisor");
        guestReceiveStringFunctionPointer = PosixUtils.dlsym(dlHandle, "guest_receive_string");
        invokeFunctionPointer = PosixUtils.dlsym(dlHandle, "invoke_main_function");
        createIsolateFunctionPointer = PosixUtils.dlsym(dlHandle, "graal_create_isolate");
        tearDownIsolateFunctionPointer = PosixUtils.dlsym(dlHandle, "graal_tear_down_isolate");
    }

    /**
     * GraalVisor API: create guest isolate and register GraalVisor into the guest isolate.
     * 
     * @return return the GuestIsolateThread attached to the guest isolate.
     */
    public GuestIsolateThread createIsolate() {
        CEntryPointNativeFunctions.IsolateThreadPointer isolateThreadPointer = StackValue.get(CEntryPointNativeFunctions.IsolateThreadPointer.class);
        createIsolateFunctionPointer.invoke(WordFactory.nullPointer(), WordFactory.nullPointer(), isolateThreadPointer);
        GuestIsolateThread isolateThread = (GuestIsolateThread) isolateThreadPointer.read();
        guestInstallGraalvisorFunctionPointer.invoke(isolateThread, (GraalVisorIsolateThread) CurrentIsolate.getCurrentThread(), getGraalVisorHostDescriptor());
        return isolateThread;
    }

    /**
     * GraalVisor API: tear down guest isolate.
     * 
     * @param thread isolate thread that created by this so.
     */

    public void tearDownIsolate(GuestIsolateThread thread) {
        tearDownIsolateFunctionPointer.invoke(thread);
    }

    /**
     * GraalVisor API: invoke the main function inside the className
     * 
     * @param guestIsolate working isolate
     * @param className fully-qualified class name of guest application class
     * @param arguments arguments passed the application function
     * @return invocation result
     */
    public String invokeFunction(GuestIsolateThread guestIsolate, String className, String arguments) {
        ObjectHandle functionHandle, argumentHandle;
        try (CTypeConversion.CCharPointerHolder cStringHolder = CTypeConversion.toCString(className)) {
            functionHandle = guestReceiveStringFunctionPointer.invoke(guestIsolate, cStringHolder.get());
        }
        try (CTypeConversion.CCharPointerHolder cStringHolder = CTypeConversion.toCString(arguments)) {
            argumentHandle = guestReceiveStringFunctionPointer.invoke(guestIsolate, cStringHolder.get());
        }
        ObjectHandle resultHandle = invokeFunctionPointer.invoke(guestIsolate, functionHandle, argumentHandle);
        return retrieveString(resultHandle);
    }

    @Override
    public void close() throws IOException {
        dlclose(dlHandle);
    }

    interface CreateIsolateFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(CEntryPointCreateIsolateParameters params, CEntryPointNativeFunctions.IsolatePointer isolate, CEntryPointNativeFunctions.IsolateThreadPointer thread);
    }

    interface GuestInstallGraalvisorFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(GuestIsolateThread guestThread, GraalVisorIsolateThread hostThread, GraalVisor.GraalVisorStruct graalVisorStructHost);
    }

    interface TearDownIsolateFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(GuestIsolateThread thread);
    }

    interface InvokeFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        ObjectHandle invoke(GuestIsolateThread guestIsolate, ObjectHandle functionName, ObjectHandle arguments);
    }

    interface GuestReceiveStringFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        ObjectHandle invoke(GuestIsolateThread guestThread, CCharPointer cString);
    }

}
