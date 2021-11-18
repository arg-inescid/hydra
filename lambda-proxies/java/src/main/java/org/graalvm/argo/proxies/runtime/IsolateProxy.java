package org.graalvm.argo.proxies.runtime;

import static org.graalvm.argo.proxies.utils.IsolateUtils.retrieveString;
import static org.graalvm.argo.proxies.utils.JsonUtils.json;
import static org.graalvm.argo.proxies.utils.ThreadLocalIsolate.threadLocalIsolate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.graalvm.argo.proxies.base.IsolateObjectWrapper;
import org.graalvm.argo.proxies.base.IsolateThreadFactory;
import org.graalvm.argo.proxies.engine.LanguageEngine;
import org.graalvm.argo.proxies.utils.IsolateUtils;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.function.CEntryPoint;

/**
 * Methods for starting proxy inside Native Image, where each request is executed inside separated
 * isolate
 */
public class IsolateProxy extends RuntimeProxy {

    public IsolateProxy(int port, LanguageEngine engine, boolean concurrent) throws IOException {
        super(port, engine, concurrent);
    }

    /**
     * Entry point for workIsolate that finish the invocation
     *
     * @param processContext The context of the current thread in the isolate in which the code
     *            should execute.
     * @param defaultContext The context of the current thread in the parent's isolate.
     * @param argumentHandle The parameters for invocation
     * @return An object handle within the parent isolate's context for a {@link ByteBuffer} object
     *         containing the SVG document.
     */
    @SuppressWarnings("unused")
    @CEntryPoint
    static ObjectHandle invoke(@CEntryPoint.IsolateThreadContext IsolateThread processContext, IsolateThread defaultContext, ObjectHandle functionHandle, ObjectHandle argumentHandle)
                    throws InvocationTargetException, IllegalAccessException, IOException, ClassNotFoundException, NoSuchMethodException {
        // Resolve and delete the argumentsHandle and functionHandle, deserialize request json
        String functionName = retrieveString(functionHandle);
        String argumentString = retrieveString(argumentHandle);
        String resultString = IsolateProxy.languageEngine.invoke(functionName, argumentString);
        return IsolateUtils.copyString(defaultContext, resultString);
    }

    @Override
    protected String invoke(String functionName, String arguments) throws Exception {
        long start = System.currentTimeMillis();
        Map<String, Object> output = new HashMap<>();
        IsolateObjectWrapper isolateObjectWrapper = threadLocalIsolate.get();
        IsolateThread processContext = isolateObjectWrapper.getIsolateThread();
        // register function
        languageEngine.registerFunction(functionName, isolateObjectWrapper);
        // copy serialized input into heap space of the process isolate
        IsolateThread defaultContext = CurrentIsolate.getCurrentThread();
        ObjectHandle functionHandle = IsolateUtils.copyString(processContext, functionName);
        ObjectHandle argumentHandle = IsolateUtils.copyString(processContext, arguments);
        // invoke function and deserialize output
        ObjectHandle outputHandle = invoke(processContext, defaultContext, functionHandle, argumentHandle);
        String outputString = retrieveString(outputHandle);
        output.put("result", outputString);
        output.put("process time", System.currentTimeMillis() - start);
        String ret = null;
        try {
            ret = json.asString(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public void start() {
        registerInvocationHandler();
        languageEngine.registerHandler(server);
        IsolateThreadFactory factory = new IsolateThreadFactory();
        factory.setCleanUp((isolateObjectWrapper) -> languageEngine.cleanUp(isolateObjectWrapper));
        ExecutorService executorService = concurrent ? Executors.newCachedThreadPool(factory) : Executors.newFixedThreadPool(1, factory);
        server.setExecutor(executorService);
        server.start();
    }

}
