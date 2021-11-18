package org.graalvm.argo.proxies.base;

import static org.graalvm.argo.proxies.utils.IsolateUtils.copyString;
import static org.graalvm.argo.proxies.utils.IsolateUtils.retrieveString;
import static org.graalvm.argo.proxies.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.proxies.utils.JsonUtils.valueToJson;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class TruffleExecutor {

    // Cached Truffle execution context
    private static final Context context = Context.newBuilder().allowAllAccess(true).build();
    // FunctionTable is used to store registered functions inside default and worker isolates
    private static final ConcurrentHashMap<String, PolyglotFunction> functionTable = new ConcurrentHashMap<>();
    // FunctionIsolateTable is used to store function registered isolates inside default isolate.
    // Not used in worker isolate.
    private static final ConcurrentHashMap<String, Set<IsolateObjectWrapper>> functionIsolateTable = new ConcurrentHashMap<>();
    // Management information inside default isolate.
    // Not used in worker isolate.
    private static final ConcurrentHashMap<IsolateObjectWrapper, Set<String>> isolateFunctionTable = new ConcurrentHashMap<>();

    public static PolyglotFunction getFunction(String name) {
        return functionTable.get(name);
    }

    public static void register(String functionName, String language, String sourceCode) throws Exception {
        Value evaluatedFunction = context.eval(language, sourceCode);
        if (!evaluatedFunction.canExecute()) {
            throw new Exception("Fail to evaluate source code.");
        }
        PolyglotFunction polyglotFunction = new PolyglotFunction(functionName, language, sourceCode);
        polyglotFunction.setEvaluatedFunction(evaluatedFunction);
        functionTable.put(functionName, polyglotFunction);
    }

    public static void register(String functionName, PolyglotFunction function, IsolateObjectWrapper isolateObjectWrapper) throws Exception {
        IsolateThread processContext = Isolates.getCurrentThread(isolateObjectWrapper.getIsolate());
        ObjectHandle functionHandle = copyString(processContext, functionName);
        ObjectHandle languageHandle = copyString(processContext, function.getLanguage());
        ObjectHandle sourceCodeHandle = copyString(processContext, function.getSourceCode());
        register(processContext, functionHandle, languageHandle, sourceCodeHandle);
        functionIsolateTable.putIfAbsent(functionName, new HashSet<>());
        functionIsolateTable.get(functionName).add(isolateObjectWrapper);
        isolateFunctionTable.putIfAbsent(isolateObjectWrapper, new HashSet<>());
        isolateFunctionTable.get(isolateObjectWrapper).add(functionName);
    }

    @CEntryPoint
    private static void register(@CEntryPoint.IsolateThreadContext IsolateThread processContext, ObjectHandle functionHandle, ObjectHandle languageHandle, ObjectHandle sourceCodeHandle)
                    throws Exception {
        String functionName = retrieveString(functionHandle);
        String language = retrieveString(languageHandle);
        String sourceCode = retrieveString(sourceCodeHandle);
        register(functionName, language, sourceCode);
    }

    public static boolean functionExists(String functionName) {
        return functionTable.containsKey(functionName);
    }

    public static boolean functionExists(String functionName, IsolateObjectWrapper isolateObjectWrapper) {
        Set<IsolateObjectWrapper> registeredIsolates = functionIsolateTable.get(functionName);
        return registeredIsolates != null && registeredIsolates.contains(isolateObjectWrapper);
    }

    public static boolean deregister(String functionName) {
        // when isolates are torn down, the cached source code would be cleared
        functionIsolateTable.remove(functionName);
        return functionTable.remove(functionName) != null;
    }

    @CEntryPoint
    private static void closeContext(@CEntryPoint.IsolateThreadContext IsolateThread processContext) {
        context.close();
    }

    public static void deregisterIsolate(IsolateObjectWrapper isolateObjectWrapper) {
        // Close context before tearing down
        closeContext(isolateObjectWrapper.getIsolateThread());
        if (!isolateFunctionTable.containsKey(isolateObjectWrapper)) {
            return;
        }
        for (String function : isolateFunctionTable.get(isolateObjectWrapper)) {
            if (functionIsolateTable.containsKey(function)) {
                functionIsolateTable.get(function).remove(isolateObjectWrapper);
            }
        }
        isolateFunctionTable.remove(isolateObjectWrapper);
    }

    public static String invoke(String name, String arguments) {
        if (!functionExists(name)) {
            return "{'Error': 'Uploaded source not existed!'}";
        }
        // how do we determine the function we should call?
        // use proxy object to only send key-value pairs into context
        ProxyObject args = ProxyObject.fromMap(jsonToMap(arguments));
        Value function = functionTable.get(name).getEvaluatedFunction();
        Value res = function.execute(args);
        return valueToJson(res);

    }
}
