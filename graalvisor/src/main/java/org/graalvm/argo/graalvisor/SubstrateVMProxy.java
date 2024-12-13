package org.graalvm.argo.graalvisor;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.argo.graalvisor.function.PolyglotFunction;
import org.graalvm.argo.graalvisor.sandboxing.SandboxHandle;

import com.sun.net.httpserver.HttpExchange;

/**
 * A runtime proxy that runs requests on Native image-based sandboxes.
 */
public class SubstrateVMProxy extends RuntimeProxy {

    /**
     * A Request object is used as a communication packet between a foreground thread and a background thread.
     */
    static class Request {

        final HttpExchange he;

        final long startTime;

        final String input;

        public Request(HttpExchange he, long startTime, String input) {
            this.he = he;
            this.startTime = startTime;
            this.input = input;
        }

        public HttpExchange getHttpExchange() {
            return this.he;
        }

        public long getStartTime() {
            return this.startTime;
        }

        public String getInput() {
            return input;
        }
    }

    /**
     * An isolate worker is a thread that retrieves requests from a queue and runs them in its own isolate.
     */
    static class Worker extends Thread {

        private final FunctionPipeline pipeline;

        public Worker(FunctionPipeline pipeline) {
            this.pipeline = pipeline;
        }

        private void processRequest(SandboxHandle shandle, Request req) {
            String output = "(uninitialized output)";
            try {
                output = shandle.invokeSandbox(req.getInput());
                RuntimeProxy.PROCESSED_REQUESTS.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace(System.err);
                output = getName();
            } finally {
                sendReply(req.getHttpExchange(), req.getStartTime(), output);
            }
        }

        public void runInternal() throws Exception {
            SandboxHandle shandle = prepareSandbox(pipeline.getFunction());
            Request req = null;
            int numberAttempts = 0;

            try {
                while (true) {
                    req = pipeline.queue.poll();

                    if (req != null) {
                        // Processes a single request in the pipeline.
                        processRequest(shandle, req);
                        // Decrement active requests in the pipeline.
                        pipeline.active.getAndDecrement();
                        // Reset the counter since we successfully processed a request
                        numberAttempts = 0;
                    } else {
                        // Sleep for 1 millisecond before trying again
                        Thread.sleep(1);

                        // Check if we have been polling for more than 60,000 times. Due to the 1 millisecond sleep, this is similar to a 60-second timeout
                        if (numberAttempts++ > 60000) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }

            // Worker is terminating. Decrement worker count.
            pipeline.workers.decrementAndGet();
            // Also destroy the sandbox.
            destroySandbox(pipeline.getFunction(), shandle);
        }

        @Override
        public void run() {
            try {
                runInternal();
            } catch (Exception e) {
                System.err.println(String.format("[thread %s] Error: thread quit unexpectedly", Thread.currentThread().getId()));
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * A function pipeline contains a queue used to submit requests for a function along with the number of free workers for this function.
     */
    static class FunctionPipeline {

        private final PolyglotFunction function;

        private final ConcurrentLinkedQueue<Request> queue;

        private final AtomicInteger active = new AtomicInteger(0);

        private final AtomicInteger workers = new AtomicInteger(0);

        private AtomicInteger maxWorkers = new AtomicInteger(0);

        public FunctionPipeline(PolyglotFunction function) {
            this.function = function;
            this.queue = new ConcurrentLinkedQueue<>();
        }

        public void invokeInCachedSandbox(Request req) {
            int cActive = active.incrementAndGet();
            int cWorkers = workers.intValue();
            int cMaxWorkers = maxWorkers.intValue();

            // Checks if we need to launch additional sandboxes.
            if (cWorkers < cActive && (cMaxWorkers == 0 || cWorkers < cMaxWorkers)) {
                synchronized (this) {
                    // Repeats the check to verify that we won the race.
                    if (workers.intValue() < active.intValue()) {
                        workers.incrementAndGet();
                        new Worker(this).start();
                    }
                }
            }

            // Adding request to the function pipeline queue.
            queue.add(req);
        }

        public void setMaxWorkers(int value) {
            this.maxWorkers.set(value);
        }

        public PolyglotFunction getFunction() {
            return this.function;
        }
    }

    /**
     * A map of queues is used to send requests into worker threads.
     */
    private static ConcurrentMap<String, FunctionPipeline> queues = new ConcurrentHashMap<>();

    public SubstrateVMProxy(int port, String appDir) throws IOException {
        super(port, appDir);
    }

    private static FunctionPipeline getFunctionPipeline(PolyglotFunction function) {
        FunctionPipeline pipeline = queues.get(function.getName());

        if (pipeline == null) {
            FunctionPipeline newpipeline = new FunctionPipeline(function);
            FunctionPipeline oldpipeline = queues.putIfAbsent(function.getName(), newpipeline);
            pipeline = oldpipeline == null ? newpipeline : oldpipeline;
        }

        return pipeline;
    }

    private static SandboxHandle prepareSandbox(PolyglotFunction function) throws IOException {
        long start = System.nanoTime();
        SandboxHandle worker = function.getSandboxProvider().createSandbox();
        long finish = System.nanoTime();
        System.out.println(String.format("[thread %s] New %s sandbox %s in %s us", Thread.currentThread().getId(), function.getSandboxProvider().getName(), worker, (finish - start)/1000));
        return worker;
    }

    private static void destroySandbox(PolyglotFunction function, SandboxHandle shandle) throws IOException {
        System.out.println(String.format("[thread %s] Destroying %s sandbox %s", Thread.currentThread().getId(), function.getSandboxProvider().getName(), shandle));
        function.getSandboxProvider().destroySandbox(shandle);
    }

    protected void setMaxSandboxes(PolyglotFunction function, int max) {
        getFunctionPipeline(function).maxWorkers.set(max);
    }

    @Override
    protected void invoke(
            HttpExchange he,
            PolyglotFunction function,
            boolean cached,
            int warmupConc,
            int warmupReqs,
            long startTime,
            String arguments) {
        String output = "(uninitialized output)";
        if (warmupConc != 0 || warmupReqs != 0) {
            try {
                output = function.getSandboxProvider().warmupProvider(warmupConc, warmupReqs, arguments);
            } catch (Exception e) {
                e.printStackTrace();
                output = e.getLocalizedMessage();
            } finally {
                sendReply(he, startTime, output);
            }
        } else if (cached) {
            getFunctionPipeline(function).invokeInCachedSandbox(new Request(he, startTime, arguments));
        } else {
            try {
                SandboxHandle shandle = prepareSandbox(function);
                output = shandle.invokeSandbox(arguments);
                RuntimeProxy.PROCESSED_REQUESTS.incrementAndGet();
                destroySandbox(function, shandle);
            } catch (Exception e) {
                e.printStackTrace();
                output = e.getLocalizedMessage();
            } finally {
                sendReply(he, startTime, output);
            }
        }
    }
}
