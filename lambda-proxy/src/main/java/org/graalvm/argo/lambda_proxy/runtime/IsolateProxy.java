package org.graalvm.argo.lambda_proxy.runtime;

import static org.graalvm.argo.lambda_proxy.Proxy.runInIsolate;
import static org.graalvm.argo.lambda_proxy.utils.IsolateUtils.retrieveString;
import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.json;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.graalvm.argo.lambda_proxy.base.FunctionRegistrationFailure;
import org.graalvm.argo.lambda_proxy.base.IsolateObjectWrapper;
import org.graalvm.argo.lambda_proxy.engine.JavaEngine;
import org.graalvm.argo.lambda_proxy.engine.LanguageEngine;
import org.graalvm.argo.lambda_proxy.engine.PolyglotEngine;
import org.graalvm.argo.lambda_proxy.utils.IsolateUtils;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.polyglot.PolyglotException;

/**
 * Methods for starting proxy inside Native Image, where each request is executed inside separated
 * isolate
 */
public class IsolateProxy extends RuntimeProxy {

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
                    ObjectHandle functionHandle, ObjectHandle argumentHandle)
                    throws InvocationTargetException, IllegalAccessException, IOException, ClassNotFoundException, NoSuchMethodException {
        // Resolve and delete the argumentsHandle and functionHandle, deserialize request json
        String functionName = retrieveString(functionHandle);
        String argumentString = retrieveString(argumentHandle);
        String resultString = IsolateProxy.languageEngine.invoke(functionName, argumentString);
        return IsolateUtils.copyString(defaultContext, resultString);
    }

    @Override
    protected String invoke(String functionName, String arguments) throws PolyglotException, IOException,
                    ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException,
                    FunctionRegistrationFailure {
        long start = System.currentTimeMillis();
        Map<String, Object> output = new HashMap<>();
        IsolateObjectWrapper isolateObjectWrapper = languageEngine.createIsolate(functionName);
        // copy serialized input into heap space of the process isolate
        String outputString = languageEngine.invoke(isolateObjectWrapper, functionName, arguments);
        output.put("result", outputString);
        output.put("process time", System.currentTimeMillis() - start);
        String ret = null;
        try {
            ret = json.asString(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        languageEngine.tearDownIsolate(functionName, isolateObjectWrapper);
        return ret;
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
