package org.graalvm.argo.graalvisor.sandboxing;

import java.io.File;
import java.io.IOException;
import org.graalvm.argo.graalvisor.base.NativeFunction;
import org.graalvm.argo.graalvisor.utils.sharedmemory.ReceiveOnlySharedMemoryChannel;
import org.graalvm.argo.graalvisor.utils.sharedmemory.SendOnlySharedMemoryChannel;
import org.graalvm.nativeimage.c.function.CFunction;

import com.oracle.svm.graalvisor.api.GraalVisorAPI;
import com.oracle.svm.graalvisor.types.GuestIsolateThread;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class ProcessSandboxHandle extends SandboxHandle {

    /**
     * Location on disk where shared memory channel files are placed.
     */
    private static final String SMEM_DIR = "/tmp";
    private static final String SMEM_PREFIX = SMEM_DIR + "/shared-mem-channel";
    private static final String P2C_SMEM_PREFIX = SMEM_PREFIX + "-to-";
    private static final String C2P_SMEM_PREFIX = SMEM_PREFIX + "-from-";

    /**
     * Signal number for sending a SIGKILL signal.
     */
    private static final int SIGKILL = 9;

    /**
     * Process identifier of the subprocess/child.
     */
    private int childPid;

    /**
     * Shared memory channel used to send invocation arguments.
     */
    private SendOnlySharedMemoryChannel sender;

    /**
     * Shared memory channel used to receive invocation replies.
     */
    private ReceiveOnlySharedMemoryChannel receiver;

    static {
         SignalHandler shandler = new SignalHandler() {
            @Override
            public void handle(Signal s) {
                for (final File file : new File(SMEM_DIR).listFiles()) {
                    if (!file.isDirectory() && file.getPath().startsWith(P2C_SMEM_PREFIX)) {
                        destroyChild(Integer.parseInt(file.getName().split("-")[4]));
                    }
                }
                System.exit(0);
            }
        };
        Signal.handle(new Signal("TERM"), shandler);
        Signal.handle(new Signal("INT"), shandler);
    }

    private static File parentToChildChannel(int pid) {
        return new File(String.format("%s%d", P2C_SMEM_PREFIX, pid));
    }

    private static File childToParentChannel(int pid) {
        return new File(String.format("%s%d", C2P_SMEM_PREFIX, pid));
    }

    @CFunction
    public static native int fork();

    @CFunction
    public static native int kill(int pid, int sig);

    private void child(ProcessSandboxProvider rsProvider) {
        NativeFunction function = (NativeFunction) rsProvider.getFunction();
        GraalVisorAPI gvAPI = rsProvider.getGraalvisorAPI();
        GuestIsolateThread ithread = gvAPI.createIsolate();
        try {
            while(true) {
                sender.writeString(gvAPI.invokeFunction(ithread, function.getEntryPoint(), receiver.readString()));
            }
        } catch(Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            kill((int) ProcessHandle.current().pid(), SIGKILL);
        }
    }

    public ProcessSandboxHandle(ProcessSandboxProvider rsProvider) throws IOException {
        if ((childPid = fork())== 0) {
            childPid = (int) ProcessHandle.current().pid();
            sender = new SendOnlySharedMemoryChannel(childToParentChannel(childPid));
            receiver = new ReceiveOnlySharedMemoryChannel(parentToChildChannel(childPid));
            child(rsProvider);
        } else {
            sender = new SendOnlySharedMemoryChannel(parentToChildChannel(childPid));
            receiver = new ReceiveOnlySharedMemoryChannel(childToParentChannel(childPid));
        }
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws Exception {
        sender.writeString(jsonArguments);
        return receiver.readString();
    }

    private static void destroyChild(int pid) {
        parentToChildChannel(pid).delete();
        childToParentChannel(pid).delete();
        System.out.println("Killing process " + pid);
        kill(pid, SIGKILL);
    }

    @Override
    public void destroyHandle() throws IOException {
        this.sender.close();
        this.receiver.close();
        destroyChild(childPid);
    }

    @Override
    public String toString() {
        return Integer.toString(childPid);
    }
}
