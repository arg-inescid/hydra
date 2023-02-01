package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.utils.IsolateUtils.retrieveString;
import static org.graalvm.argo.graalvisor.utils.JsonUtils.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.graalvm.argo.graalvisor.base.IsolateObjectWrapper;
import org.graalvm.argo.graalvisor.base.PolyglotLanguage;
import org.graalvm.argo.graalvisor.engine.PolyglotEngine;
import org.graalvm.argo.graalvisor.utils.IsolateUtils;
import org.graalvm.nativeimage.Isolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.function.CEntryPoint;

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

        private final String functionName;

        private FunctionPipeline pipeline;

        public IsolateWorker(String functionName, FunctionPipeline pipeline) {
            this.functionName = functionName;
            this.pipeline = pipeline;
        }

        private void processRequest(IsolateObjectWrapper isolateObjectWrapper, Request req) {
            synchronized (req) {
                // Get input from request, invoke function in isolate, fill output.
                try {
                    req.setOutput(languageEngine.invoke(isolateObjectWrapper, functionName, req.getInput()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Notify the frontend thread about the result being ready.
                req.notify();
            }
        }

        private IsolateObjectWrapper prepareIsolate() {
            synchronized (pipeline) {
                if (! PolyglotEngine.functionTable.get(functionName).getLanguage().equals(PolyglotLanguage.JAVA)) {
                    if (pipeline.workers.isEmpty()) {
                        IsolateObjectWrapper worker = languageEngine.createIsolate(functionName);
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
                    IsolateObjectWrapper worker = languageEngine.createIsolate(functionName);
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
                    languageEngine.tearDownIsolate(functionName, worker);
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
            pipeline = queues.remove(functionName);
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

    @SuppressWarnings("unused")
    @CEntryPoint
    public static ObjectHandle invoke(@CEntryPoint.IsolateThreadContext IsolateThread processContext, IsolateThread defaultContext,
                    ObjectHandle functionHandle, ObjectHandle argumentHandle) throws Exception {
        // Resolve and delete the argumentsHandle and functionHandle, deserialize request json
        String functionName = retrieveString(functionHandle);
        String argumentString = retrieveString(argumentHandle);
        String resultString = SubstrateVMProxy.languageEngine.invoke(functionName, argumentString);
        return IsolateUtils.copyString(defaultContext, resultString);
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

    private FunctionPipeline getFunctionPipeline(String functionName) {
        FunctionPipeline pipeline = queues.get(functionName);
        if (pipeline == null) {
            FunctionPipeline newpipeline = new FunctionPipeline();
            FunctionPipeline oldpipeline = queues.putIfAbsent(functionName, newpipeline);
            pipeline = oldpipeline == null ? newpipeline : oldpipeline;
            // We could loose the race to install a queue for this function.
            if (oldpipeline == null) {
                // We won the race, create isolate worker.
                new IsolateWorker(functionName, pipeline).start();
            }
        } else if (!pipeline.queue.isEmpty() && pipeline.freeworkers < pipeline.queue.size()) {
            // If the queue is not empty and if there not enough workers, we create a new isolate worker to help processing requests.
            new IsolateWorker(functionName, pipeline).start();
        }
        return pipeline;
    }

    @Override
    protected String invoke(String functionName, boolean cached, String arguments) throws Exception {
        String res;
        long start = System.nanoTime();

        if (cached) {
            res = invokeInIsolateWorker(getFunctionPipeline(functionName), arguments);
        } else {
            IsolateObjectWrapper isolateObjectWrapper = languageEngine.createIsolate(functionName);
            // Copy serialized input into heap space of the process isolate.
            res = languageEngine.invoke(isolateObjectWrapper, functionName, arguments);
            // Tear down isolate (could be offline?)
            languageEngine.tearDownIsolate(functionName, isolateObjectWrapper);
        }

        long finish = System.nanoTime();
        Map<String, Object> output = new HashMap<>();
        output.put("result", res);
        output.put("process time (us)", (finish - start) / 1000);
        return json.asString(output);
    }
}
