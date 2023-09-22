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
        int invoke(IsolateThread thread, int connectionId, CCharPointer query);
    }

    public interface HostReturnDBConnectionFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(IsolateThread thread, int connectionId);
    }

    public interface HostResultSetNextFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(IsolateThread thread, int resultSetId);
    }

    public interface HostResultSetGetIntIndexFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(IsolateThread thread, int resultSetId, int columnIndex);
    }

    public interface HostResultSetGetIntLabelFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(IsolateThread thread, int resultSetId, CCharPointer columnLabel);
    }

    public interface HostResultSetGetStringIndexFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        CCharPointer invoke(IsolateThread thread, int resultSetId, int columnIndex);
    }

    public interface HostResultSetGetStringLabelFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        CCharPointer invoke(IsolateThread thread, int resultSetId, CCharPointer columnLabel);
    }

    public interface HostResultSetMetaDataGetColumnCountFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        int invoke(IsolateThread thread, int resultSetId);
    }

    public interface HostResultSetMetaDataGetColumnNameFunctionPointer extends CFunctionPointer {
        @InvokeCFunctionPointer
        CCharPointer invoke(IsolateThread thread, int resultSetId, int column);
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

        @CField("f_host_resultset_next")
        HostResultSetNextFunctionPointer getHostResultSetNextFunction();

        @CField("f_host_resultset_next")
        void setHostResultSetNextFunction(HostResultSetNextFunctionPointer resultSetNextFunction);

        @CField("f_host_resultset_getint_index")
        HostResultSetGetIntIndexFunctionPointer getHostResultSetGetIntIndexFunction();

        @CField("f_host_resultset_getint_index")
        void setHostResultSetGetIntIndexFunction(HostResultSetGetIntIndexFunctionPointer resultSetGetIntIndexFunction);

        @CField("f_host_resultset_getint_label")
        HostResultSetGetIntLabelFunctionPointer getHostResultSetGetIntLabelFunction();

        @CField("f_host_resultset_getint_label")
        void setHostResultSetGetIntLabelFunction(HostResultSetGetIntLabelFunctionPointer resultSetGetIntLabelFunction);

        @CField("f_host_resultset_getstring_index")
        HostResultSetGetStringIndexFunctionPointer getHostResultSetGetStringIndexFunction();

        @CField("f_host_resultset_getstring_index")
        void setHostResultSetGetStringIndexFunction(HostResultSetGetStringIndexFunctionPointer resultSetGetStringIndexFunction);

        @CField("f_host_resultset_getstring_label")
        HostResultSetGetStringLabelFunctionPointer getHostResultSetGetStringLabelFunction();

        @CField("f_host_resultset_getstring_label")
        void setHostResultSetGetStringLabelFunction(HostResultSetGetStringLabelFunctionPointer resultSetGetStringLabelFunction);

        @CField("f_host_resultsetmetadata_getcolumncount")
        HostResultSetMetaDataGetColumnCountFunctionPointer getHostResultSetMetaDataGetColumnCountFunction();

        @CField("f_host_resultsetmetadata_getcolumncount")
        void setHostResultSetMetaDataGetColumnCountFunction(HostResultSetMetaDataGetColumnCountFunctionPointer resultSetMetaDataGetColumnCountFunction);

        @CField("f_host_resultsetmetadata_getcolumnname")
        HostResultSetMetaDataGetColumnNameFunctionPointer getHostResultSetMetaDataGetColumnNameFunction();

        @CField("f_host_resultsetmetadata_getcolumnname")
        void setHostResultSetMetaDataGetColumnNameFunction(HostResultSetMetaDataGetColumnNameFunctionPointer resultSetMetaDataGetColumnNameFunction);
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
