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
import org.graalvm.word.UnsignedWord;

import com.oracle.svm.graalvisor.types.GraalVisorIsolate;
import com.oracle.svm.graalvisor.types.GraalVisorIsolateThread;

@CContext(GraalVisor.GraalVisorAPIDirectives.class)
public class GraalVisor {

    public interface HostReceiveStringFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        ObjectHandle invoke(GraalVisorIsolateThread thread, CCharPointer cString);
    }

    public interface HostOpenFileFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(GraalVisorIsolateThread hostThread, CCharPointer fileName, int accessMode);
    }

    public interface HostCloseFileFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(GraalVisorIsolateThread hostThread, int fd);
    }

    public interface HostWriteBytesFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(GraalVisorIsolateThread hostThread, int fd, CCharPointer bytes, UnsignedWord length);
    }

    public interface HostReadBytesFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(GraalVisorIsolateThread hostThread, int fd, CCharPointer buffer, int bufferLen, int readOffset);
    }

    public interface HostOpenDBConnectionFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(GraalVisorIsolateThread hostThread, CCharPointer connectionUrl, CCharPointer user, CCharPointer password);
    }

    public interface HostExecuteDBQueryFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(GraalVisorIsolateThread hostThread, int connectionId, CCharPointer query, CCharPointer buffer, int bufferLen);
    }

    public interface HostCloseDBConnectionFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(GraalVisorIsolateThread hostThread, int connectionId);
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

        @CField("f_host_open_file")
        HostOpenFileFunctionPointer getHostOpenFileFunction();

        @CField("f_host_open_file")
        void setHostOpenFileFunction(HostOpenFileFunctionPointer hostOpenFileFunctionPointer);

        @CField("f_host_close_file")
        HostCloseFileFunctionPointer getHostCloseFileFunction();

        @CField("f_host_close_file")
        void setHostCloseFileFunction(HostCloseFileFunctionPointer hostCloseFileFunctionPointer);

        @CField("f_host_write_bytes")
        HostWriteBytesFunctionPointer getHostWriteBytesFunction();

        @CField("f_host_write_bytes")
        void setHostWriteBytesFunction(HostWriteBytesFunctionPointer hostWriteBytesFunctionPointer);

        @CField("f_host_read_bytes")
        HostReadBytesFunctionPointer getHostReadBytesFunction();

        @CField("f_host_read_bytes")
        void setHostReadBytesFunction(HostReadBytesFunctionPointer hostReadBytesFunctionPointer);

        @CField("f_host_open_db_connection")
        HostOpenDBConnectionFunctionPointer getHostOpenDBConnectionFunction();

        @CField("f_host_open_db_connection")
        void setHostOpenDBConnectionFunction(HostOpenDBConnectionFunctionPointer hostOpenDBConnectionFunctionPointer);

        @CField("f_host_execute_db_query")
        HostExecuteDBQueryFunctionPointer getHostExecuteDBQueryFunction();

        @CField("f_host_execute_db_query")
        void setHostExecuteDBQueryFunction(HostExecuteDBQueryFunctionPointer hostExecuteDBQueryFunctionPointer);

        @CField("f_host_close_db_connection")
        HostCloseDBConnectionFunctionPointer getHostCloseDBConnectionFunction();

        @CField("f_host_close_db_connection")
        void setHostCloseDBConnectionFunction(HostCloseDBConnectionFunctionPointer hostCloseDBConnectionFunctionPointer);
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
