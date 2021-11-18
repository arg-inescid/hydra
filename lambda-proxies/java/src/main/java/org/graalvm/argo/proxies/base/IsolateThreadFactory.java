package org.graalvm.argo.proxies.base;

import static org.graalvm.argo.proxies.utils.ThreadLocalIsolate.threadLocalIsolate;

import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.graalvm.argo.proxies.engine.JavaEngine;
import org.graalvm.argo.proxies.engine.PolyglotEngine;
import org.graalvm.argo.proxies.runtime.RuntimeProxy;
import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.function.CEntryPoint;

public class IsolateThreadFactory implements ThreadFactory {

    private static final long MIN_HEAP_SIZE = 512;
    private static final long MAX_HEAP_SIZE = 512;
    Consumer<IsolateObjectWrapper> cleanUp = isolateThread -> {
    };

    @CEntryPoint
    private static void initialize(@CEntryPoint.IsolateThreadContext IsolateThread processContext) {
        RuntimeProxy.languageEngine = ImageSingletons.contains(JavaEngine.class) ? ImageSingletons.lookup(JavaEngine.class) : ImageSingletons.lookup(PolyglotEngine.class);
        RuntimeOptions.set("PrintGC", true);
        RuntimeOptions.set("PrintGCSummary", true);
        RuntimeOptions.set("VerboseGC", true);
        RuntimeOptions.set("PrintGCTimeStamps", true);
        RuntimeOptions.set("MaxHeapSize", MAX_HEAP_SIZE * 1024 * 1024);
        RuntimeOptions.set("MinHeapSize", MIN_HEAP_SIZE * 1024 * 1024);
    }

    public void setCleanUp(Consumer<IsolateObjectWrapper> cleanUp) {
        this.cleanUp = cleanUp;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(() -> {
            try {
                // create isolate and register it inside ThreadLocal
                IsolateThread isolateThread = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
                initialize(isolateThread);
                Isolate isolate = Isolates.getIsolate(isolateThread);
                IsolateObjectWrapper isolateWrapper = new IsolateObjectWrapper(isolate, isolateThread);
                threadLocalIsolate.set(isolateWrapper);
                r.run();
            } finally {
                // tear down isolate before terminating the current thread
                IsolateThread isolateThread = threadLocalIsolate.get().getIsolateThread();
                this.cleanUp.accept(threadLocalIsolate.get());
                System.err.println("Isolate torn down by eviction policy: " + Isolates.getIsolate(isolateThread).rawValue());
                Isolates.tearDownIsolate(isolateThread);
                threadLocalIsolate.remove();
            }
        });
    }

}