package org.graalvm.argo.lambda_proxy.runtime;

import static org.graalvm.argo.lambda_proxy.Proxy.runInIsolate;
import static org.graalvm.argo.lambda_proxy.utils.IsolateUtils.retrieveString;
import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.graalvm.argo.lambda_proxy.base.IsolateObjectWrapper;
import org.graalvm.argo.lambda_proxy.engine.JavaEngine;
import org.graalvm.argo.lambda_proxy.engine.LanguageEngine;
import org.graalvm.argo.lambda_proxy.engine.PolyglotEngine;
import org.graalvm.argo.lambda_proxy.utils.IsolateUtils;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.function.CEntryPoint;

/**
 * A runtime proxy that runs requests in isolates.
 */
public class IsolateProxy extends RuntimeProxy {

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
            System.out.println(String.format("[Background thread %s] Executing request %s in background", Thread.currentThread().getId(), req.input));
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

        @Override
        public void run() {
            // Create isolate.
            IsolateObjectWrapper isolateObjectWrapper = languageEngine.createIsolate(functionName);
            System.out.println(String.format("[Background thread %s] Creating isolate", Thread.currentThread().getId()));
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

            // Tear down the isolate.
            System.out.println(String.format("[Background thread %s] Destroying isolate", Thread.currentThread().getId()));
            languageEngine.tearDownIsolate(functionName, isolateObjectWrapper);
        }
    }

    /**
     * A function pipeline contains a queue used to submit requests for a fuction along with the number of free workers for this function.
     */
    static class FunctionPipeline {

        private final BlockingQueue<Request> queue;

        private volatile int freeworkers = 0;

        public FunctionPipeline() {
            this.queue = new ArrayBlockingQueue<Request>(16);
        }
    }

    /**
     * A map of queues is used to send requests into worker threads.
     */
    private static ConcurrentMap<String, FunctionPipeline> queues = new ConcurrentHashMap<>();

    static {
        if (runInIsolate) {
            RuntimeProxy.languageEngine = ImageSingletons.contains(JavaEngine.class) ? ImageSingletons.lookup(JavaEngine.class) : ImageSingletons.lookup(PolyglotEngine.class);
        }
    }

    public IsolateProxy(int port, LanguageEngine engine, boolean concurrent) throws IOException {
        super(port, engine, concurrent);
    }

    @SuppressWarnings("unused")
    @CEntryPoint
    public static ObjectHandle invoke(@CEntryPoint.IsolateThreadContext IsolateThread processContext, IsolateThread defaultContext,
                    ObjectHandle functionHandle, ObjectHandle argumentHandle) throws Exception {
        // Resolve and delete the argumentsHandle and functionHandle, deserialize request json
        String functionName = retrieveString(functionHandle);
        String argumentString = retrieveString(argumentHandle);
        String resultString = IsolateProxy.languageEngine.invoke(functionName, argumentString);
        return IsolateUtils.copyString(defaultContext, resultString);
    }

    private String invokeInIsolateWorker(FunctionPipeline pipeline, String input) throws Exception {
        System.out.println(String.format("[Foreground thread %s] Executing request %s in background", Thread.currentThread().getId(), input));
        Request req = new Request(input);
        synchronized (req) {
            pipeline.queue.add(req);
            req.wait();
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

    @Override
    public void start() {
        registerInvocationHandler();
        languageEngine.registerHandler(server);
        ExecutorService executorService = concurrent ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(1);
        server.setExecutor(executorService);
        server.start();
    }

}
