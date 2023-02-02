package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.utils.IsolateUtils.copyString;
import static org.graalvm.argo.graalvisor.utils.IsolateUtils.retrieveString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.graalvm.argo.graalvisor.base.IsolateObjectWrapper;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import org.graalvm.argo.graalvisor.base.PolyglotLanguage;
import org.graalvm.argo.graalvisor.engine.FunctionStorage;
import org.graalvm.argo.graalvisor.utils.IsolateUtils;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.Isolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.function.CEntryPoint;

import com.oracle.svm.graalvisor.types.GuestIsolateThread;

/**
 * A runtime proxy that runs requests in isolates.
 */
public class SubstrateVMProxy extends RuntimeProxy {

    /**
     * A Request object is used as a communication packet between a foreground thread and a background thread.
     */
    static class Request {

        final String input;

        String output;

        public Request(String input) {
            this.input = input;
        }

        public String getInput() {
            return input;
        }

        public String setOutput(String output) {
            return this.output = output;
        }

        public String getOutput() {
            return output;
        }
    }

    /**
     * An isolate worker is a thread that retrieves requests from a queue and runs them in its own isolate.
     */
    static class IsolateWorker extends Thread {

        private final PolyglotFunction function;

        private FunctionPipeline pipeline;

        public IsolateWorker(PolyglotFunction function, FunctionPipeline pipeline) {
            this.function = function;
            this.pipeline = pipeline;
        }

        private void processRequest(IsolateObjectWrapper isolateObjectWrapper, Request req) {
            synchronized (req) {
                // Get input from request, invoke function in isolate, fill output.
                try {
                    req.setOutput(invokeInternal(isolateObjectWrapper, function, req.getInput()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Notify the frontend thread about the result being ready.
                req.notify();
            }
        }

        private IsolateObjectWrapper prepareIsolate() {
            synchronized (pipeline) {
                if (! function.getLanguage().equals(PolyglotLanguage.JAVA)) {
                    if (pipeline.workers.isEmpty()) {
                        IsolateObjectWrapper worker = createIsolate(function);
                        System.out.println(String.format("[thread %s] New isolate %s", Thread.currentThread().getId(), worker.getIsolate().rawValue()));
                        pipeline.workers.add(worker);
                        return worker;
                    } else {
                        Isolate isolate = pipeline.workers.get(0).getIsolate();
                        IsolateThread isolateThread = Isolates.attachCurrentThread(isolate);
                        System.out.println(String.format("[thread %s] Attached to isolate %s", Thread.currentThread().getId(), isolate.rawValue()));
                        IsolateObjectWrapper worker = new IsolateObjectWrapper(isolate, isolateThread);
                        pipeline.workers.add(worker);
                        return worker;
                    }
                } else {
                    IsolateObjectWrapper worker = createIsolate(function);
                    System.out.println(String.format("[thread %s] New isolate %s", Thread.currentThread().getId(), worker.getIsolate().rawValue()));
                    pipeline.workers.add(worker);
                    return worker;
                }
            }
        }

        private void disposeIsolate(IsolateObjectWrapper worker) {
            synchronized (pipeline) {
                pipeline.workers.remove(worker);
                if (pipeline.workers.isEmpty()) {
                    tearDownIsolate(function, worker);
                    System.out.println(String.format("[thread %s] Destroying isolate %s", Thread.currentThread().getId(), worker.getIsolate().rawValue()));
                } else {
                    Isolates.detachThread(worker.getIsolateThread());
                    System.out.println(String.format("[thread %s] Detaching from isolate %s", Thread.currentThread().getId(), worker.getIsolate().rawValue()));
                }
            }
        }

        @Override
        public void run() {
            IsolateObjectWrapper isolateObjectWrapper = prepareIsolate();
            Request req = null;
            pipeline.freeworkers++;

            try {
                while ((req = pipeline.queue.poll(10, TimeUnit.SECONDS)) != null) {
                    pipeline.freeworkers--;
                    processRequest(isolateObjectWrapper, req);
                    pipeline.freeworkers++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Between the loop exit and now, there could be a new request.
            pipeline = queues.remove(function.getName());
            while ((req = pipeline.queue.poll()) != null) {
                processRequest(isolateObjectWrapper, req);
            }

            disposeIsolate(isolateObjectWrapper);
        }
    }

    /**
     * A function pipeline contains a queue used to submit requests for a function along with the number of free workers for this function.
     */
    static class FunctionPipeline {

        private final BlockingQueue<Request> queue;

        private final List<IsolateObjectWrapper> workers;

        private volatile int freeworkers = 0;

        public FunctionPipeline() {
            this.queue = new ArrayBlockingQueue<>(16);
            this.workers = new ArrayList<>();
        }
    }

    /**
     * A map of queues is used to send requests into worker threads.
     */
    private static ConcurrentMap<String, FunctionPipeline> queues = new ConcurrentHashMap<>();

    public SubstrateVMProxy(int port) throws IOException {
        super(port);
    }

    private String invokeInIsolateWorker(FunctionPipeline pipeline, String input) {
        Request req = new Request(input);
        synchronized (req) {
            pipeline.queue.add(req);
            while(req.getOutput() == null) {
                try {
                    req.wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            return req.getOutput();
        }
    }

    private FunctionPipeline getFunctionPipeline(PolyglotFunction function) {
        FunctionPipeline pipeline = queues.get(function.getName());
        if (pipeline == null) {
            FunctionPipeline newpipeline = new FunctionPipeline();
            FunctionPipeline oldpipeline = queues.putIfAbsent(function.getName(), newpipeline);
            pipeline = oldpipeline == null ? newpipeline : oldpipeline;
            // We could loose the race to install a queue for this function.
            if (oldpipeline == null) {
                // We won the race, create isolate worker.
                new IsolateWorker(function, pipeline).start();
            }
        } else if (!pipeline.queue.isEmpty() && pipeline.freeworkers < pipeline.queue.size()) {
            // If the queue is not empty and if there not enough workers, we create a new isolate worker to help processing requests.
            new IsolateWorker(function, pipeline).start();
        }
        return pipeline;
    }

    @SuppressWarnings("unused")
    @CEntryPoint
    private static void installSourceCode(@CEntryPoint.IsolateThreadContext IsolateThread workingThread,
                    ObjectHandle functionName,
                    ObjectHandle entryPoint,
                    ObjectHandle language,
                    ObjectHandle sourceCode) {
        FunctionStorage.FTABLE.put(retrieveString(functionName), new PolyglotFunction(retrieveString(functionName), retrieveString(entryPoint), retrieveString(language), retrieveString(sourceCode)));
    }

    public static IsolateObjectWrapper createIsolate(PolyglotFunction function) {
        if (function.getLanguage().equals(PolyglotLanguage.JAVA)) {
            GuestIsolateThread guestThread = function.getGraalVisorAPI().createIsolate();
            return new IsolateObjectWrapper(Isolates.getIsolate(guestThread), guestThread);
        } else {
            // create a new isolate and setup configurations in that isolate.
            IsolateThread isolateThread = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
            Isolate isolate = Isolates.getIsolate(isolateThread);
            // initialize source code into isolate
            installSourceCode(isolateThread,
                            copyString(isolateThread, function.getName()),
                            copyString(isolateThread, function.getEntryPoint()),
                            copyString(isolateThread, function.getLanguage().name()),
                            copyString(isolateThread, function.getSource()));
            return new IsolateObjectWrapper(isolate, isolateThread);
        }
    }

    public static void tearDownIsolate(PolyglotFunction function, IsolateObjectWrapper workingIsolate) {
        if (workingIsolate != null) {
            if (function.getLanguage().equals(PolyglotLanguage.JAVA)) {
                function.getGraalVisorAPI().tearDownIsolate((GuestIsolateThread) workingIsolate.getIsolateThread());
            } else {
                Isolates.tearDownIsolate(workingIsolate.getIsolateThread());
            }
        }
    }

    @SuppressWarnings("unused")
    @CEntryPoint
    public static ObjectHandle invokeFunction(@CEntryPoint.IsolateThreadContext IsolateThread processContext, IsolateThread defaultContext,
                    ObjectHandle functionHandle, ObjectHandle argumentHandle) throws Exception {
        String functionName = retrieveString(functionHandle);
        String argumentString = retrieveString(argumentHandle);
        String resultString = languageEngine.invoke(functionName, argumentString);
        return IsolateUtils.copyString(defaultContext, resultString);
    }

    public static String invokeInternal(IsolateObjectWrapper isolate, PolyglotFunction function, String jsonArguments) throws Exception {
        if (function.getLanguage().equals(PolyglotLanguage.JAVA)) {
            GuestIsolateThread isolateThread = (GuestIsolateThread) isolate.getIsolateThread();
            return function.getGraalVisorAPI().invokeFunction(isolateThread, function.getEntryPoint(), jsonArguments);
        } else {
            IsolateThread isolateThread = isolate.getIsolateThread();
            return retrieveString(invokeFunction(isolateThread, CurrentIsolate.getCurrentThread(), copyString(isolateThread, function.getName()), copyString(isolateThread, jsonArguments)));
        }
    }

    @Override
    protected String invoke(PolyglotFunction function, boolean cached, String arguments) throws Exception {
        String res;

        if (cached) {
            res = invokeInIsolateWorker(getFunctionPipeline(function), arguments);
        } else {
            IsolateObjectWrapper isolateObjectWrapper = createIsolate(function);
            // Copy serialized input into heap space of the process isolate.
            res = invokeInternal(isolateObjectWrapper, function, arguments);
            // Tear down isolate (could be offline?)
            tearDownIsolate(function, isolateObjectWrapper);
        }

        return res;
    }
}
