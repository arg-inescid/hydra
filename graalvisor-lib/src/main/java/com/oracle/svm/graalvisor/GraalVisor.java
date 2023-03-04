package com.oracle.svm.graalvisor;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;
import com.oracle.svm.graalvisor.types.GraalVisorIsolate;
import com.oracle.svm.graalvisor.types.GraalVisorIsolateThread;

@CContext(GraalVisor.GraalVisorAPIDirectives.class)
public class GraalVisor {

    public interface HostReceiveStringFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        ObjectHandle invoke(GraalVisorIsolateThread thread, CCharPointer cString);
    }

    @CStruct("graal_visor_t")
    public interface GraalVisorStruct extends PointerBase {

        @CField("f_host_isolate")
        GraalVisorIsolate getHostIsolate();

        /* Write access of a field. A call to the function is replaced with a raw memory store. */
        @CField("f_host_isolate")
        void setHostIsolate(GraalVisorIsolate hostIsolate);

        @CField("f_host_receive_string")
        HostReceiveStringFunctionPointer getHostReceiveStringFunction();

        @CField("f_host_receive_string")
        void setHostReceiveStringFunction(HostReceiveStringFunctionPointer printFunction);
    }

    static class GraalVisorAPIDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList("<graal_visor.h>");
        }

        @Override
        public List<String> getOptions() {
            File headerFiles = new File(System.getProperty("com.oracle.svm.graalvisor.libraryPath"));
            return Collections.singletonList("-I" + headerFiles.getAbsoluteFile());
        }
    }

}
