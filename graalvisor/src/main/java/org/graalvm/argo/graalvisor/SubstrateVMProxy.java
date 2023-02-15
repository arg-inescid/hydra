package org.graalvm.argo.graalvisor;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import org.graalvm.argo.graalvisor.sandboxing.SandboxHandle;

/**
 * A runtime proxy that runs requests on Native image-based sandboxes.
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
    static class Worker extends Thread {

        private final PolyglotFunction function;

        private FunctionPipeline pipeline;

        public Worker(PolyglotFunction function, FunctionPipeline pipeline) {
            this.function = function;
            this.pipeline = pipeline;
        }

        private void processRequest(SandboxHandle shandle, Request req) {
            synchronized (req) {
                // Get input from request, invoke function in isolate, fill output.
                try {
                    req.setOutput(shandle.invokeSandbox(req.getInput()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Notify the frontend thread about the result being ready.
                req.notify();
            }
        }

        private SandboxHandle prepareSandbox() throws Exception {
            SandboxHandle worker = function.getSandboxProvider().createSandbox();
            System.out.println(String.format("[thread %s] New sandbox %s", Thread.currentThread().getId(), worker));
            return worker;
        }

        private void disposeSandbox(SandboxHandle shandle) throws Exception {
            function.getSandboxProvider().destroySandbox(shandle);
            System.out.println(String.format("[thread %s] Destroying sandbox %s", Thread.currentThread().getId(), shandle));
        }

        public void runInternal() throws Exception {
            SandboxHandle shandle = prepareSandbox();
            Request req = null;
            pipeline.freeworkers++;

            try {
                while ((req = pipeline.queue.poll(10, TimeUnit.SECONDS)) != null) {
                    pipeline.freeworkers--;
                    processRequest(shandle, req);
                    pipeline.freeworkers++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Between the loop exit and now, there could be a new request.
            pipeline = queues.remove(function.getName());
            while ((req = pipeline.queue.poll()) != null) {
                processRequest(shandle, req);
            }

            disposeSandbox(shandle);
        }

        @Override
        public void run() {
            try {
                runInternal();
            } catch (Exception e) {
                System.err.println(String.format("[thread %s] Error: thread quit unexpectedly", Thread.currentThread().getId()));
                e.printStackTrace();
            }
        }
    }

    /**
     * A function pipeline contains a queue used to submit requests for a function along with the number of free workers for this function.
     */
    static class FunctionPipeline {

        private final BlockingQueue<Request> queue;

        private volatile int freeworkers = 0;

        public FunctionPipeline() {
            this.queue = new ArrayBlockingQueue<>(16);
        }
    }

    /**
     * A map of queues is used to send requests into worker threads.
     */
    private static ConcurrentMap<String, FunctionPipeline> queues = new ConcurrentHashMap<>();

    public SubstrateVMProxy(int port) throws IOException {
        super(port);
    }

    private String invokeInCachedSandbox(FunctionPipeline pipeline, String input) {
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
                new Worker(function, pipeline).start();
            }
        } else if (!pipeline.queue.isEmpty() && pipeline.freeworkers < pipeline.queue.size()) {
            // If the queue is not empty and if there not enough workers, we create a new isolate worker to help processing requests.
            new Worker(function, pipeline).start();
        }
        return pipeline;
    }

    @Override
    protected String invoke(PolyglotFunction function, boolean cached, String arguments) throws Exception {
        String res;

        if (cached) {
            res = invokeInCachedSandbox(getFunctionPipeline(function), arguments);
        } else {
            SandboxHandle shandle = function.getSandboxProvider().createSandbox();
            res = shandle.invokeSandbox(arguments);
            function.getSandboxProvider().destroySandbox(shandle);
        }

        return res;
    }
}
