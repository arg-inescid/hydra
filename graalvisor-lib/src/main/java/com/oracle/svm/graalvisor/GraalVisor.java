package com.oracle.svm.graalvisor;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(GraalVisor.GraalVisorAPIDirectives.class)
public class GraalVisor {

    public interface HostReceiveStringFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        ObjectHandle invoke(IsolateThread thread, CCharPointer cString);
    }

    public interface HostObtainDBConnectionFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(IsolateThread thread, CCharPointer connectionUrl, CCharPointer user, CCharPointer password);
    }

    public interface HostExecuteDBQueryFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(IsolateThread thread, int connectionId, CCharPointer query, CCharPointer buffer, int bufferLen);
    }

    public interface HostReturnDBConnectionFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(IsolateThread thread, int connectionId);
    }

    @CStruct("graal_visor_t")
    public interface GraalVisorStruct extends PointerBase {

        @CField("f_host_receive_string")
        HostReceiveStringFunctionPointer getHostReceiveStringFunction();

        @CField("f_host_receive_string")
        void setHostReceiveStringFunction(HostReceiveStringFunctionPointer printFunction);

        @CField("f_host_obtain_db_connection")
        HostObtainDBConnectionFunctionPointer getHostObtainDBConnectionFunction();

        @CField("f_host_obtain_db_connection")
        void setHostObtainDBConnectionFunction(HostObtainDBConnectionFunctionPointer obtainConnectionFunction);

        @CField("f_host_execute_db_query")
        HostExecuteDBQueryFunctionPointer getHostExecuteDBQueryFunction();

        @CField("f_host_execute_db_query")
        void setHostExecuteDBQueryFunction(HostExecuteDBQueryFunctionPointer executeQueryFunction);

        @CField("f_host_return_db_connection")
        HostReturnDBConnectionFunctionPointer getHostReturnDBConnectionFunction();

        @CField("f_host_return_db_connection")
        void setHostReturnDBConnectionFunction(HostReturnDBConnectionFunctionPointer returnConnectionFunction);
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
