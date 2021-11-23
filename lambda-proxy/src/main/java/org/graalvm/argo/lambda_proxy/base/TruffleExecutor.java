package org.graalvm.argo.lambda_proxy.base;

import static org.graalvm.argo.lambda_proxy.utils.IsolateUtils.copyString;
import static org.graalvm.argo.lambda_proxy.utils.IsolateUtils.retrieveString;
import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.valueToJson;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.ProxyObject;

public class TruffleExecutor {

    // Explicit Truffle engine for context sharing and source code caching
    private static final Engine engine = Engine.create();
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

    public static void register(String functionName, String functionEntryPoint, String language, String sourceCode) throws FunctionRegistrationFailure {
        Source functionSource = Source.create(language, sourceCode);
        try (Context context = Context.newBuilder().engine(engine).allowAllAccess(true).build()) {
            // parse and evaluate source, caching source inside explicit engine.
            Value function;
            try {
                function = context.parse(functionSource);
            } catch (PolyglotException e) {
                e.printStackTrace();
                throw new FunctionRegistrationFailure("Error during parsing source code. " + e.getMessage());
            }
            function.execute();
            Value entryPointFunction = context.eval(language, functionEntryPoint);
            if (!entryPointFunction.canExecute())
                throw new FunctionRegistrationFailure(
                        String.format("Error: Entry point function %s has not been defined inside provided source code" +
                                " or it is not executable.", functionEntryPoint));
        }
        PolyglotFunction polyglotFunction = new PolyglotFunction(functionName, functionEntryPoint, language, sourceCode);
        polyglotFunction.setSource(functionSource);
        functionTable.put(functionName, polyglotFunction);
    }

    public static void register(String functionName, PolyglotFunction function, IsolateObjectWrapper isolateObjectWrapper) throws FunctionRegistrationFailure {
        IsolateThread processContext = Isolates.getCurrentThread(isolateObjectWrapper.getIsolate());
        ObjectHandle functionHandle = copyString(processContext, functionName);
        ObjectHandle entryPointHandle = copyString(processContext, function.getEntryPoint());
        ObjectHandle languageHandle = copyString(processContext, function.getLanguage());
        ObjectHandle sourceCodeHandle = copyString(processContext, function.getSourceCode());
        register(processContext, functionHandle, entryPointHandle, languageHandle, sourceCodeHandle);
        functionIsolateTable.putIfAbsent(functionName, new HashSet<>());
        functionIsolateTable.get(functionName).add(isolateObjectWrapper);
        isolateFunctionTable.putIfAbsent(isolateObjectWrapper, new HashSet<>());
        isolateFunctionTable.get(isolateObjectWrapper).add(functionName);
    }

    @CEntryPoint
    private static void register(@CEntryPoint.IsolateThreadContext IsolateThread processContext, ObjectHandle functionHandle,
                                 ObjectHandle entryPointHandle, ObjectHandle languageHandle, ObjectHandle sourceCodeHandle)
            throws FunctionRegistrationFailure {

        String functionName = retrieveString(functionHandle);
        String functionEntryPoint = retrieveString(entryPointHandle);
        String language = retrieveString(languageHandle);
        String sourceCode = retrieveString(sourceCodeHandle);
        register(functionName, functionEntryPoint, language, sourceCode);
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

    public static void deregisterIsolate(IsolateObjectWrapper isolateObjectWrapper) {
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
        String resultString = "";
        try (Context context = Context.newBuilder().allowAllAccess(true).build()) {
            ProxyObject args = ProxyObject.fromMap(jsonToMap(arguments));
            // Retrieve function source from cached source
            context.eval(functionTable.get(name).getSource());
            Value function = context.eval(functionTable.get(name).getLanguage(), functionTable.get(name).getEntryPoint());
            try {
                Value res = function.execute(args);
                resultString = valueToJson(res);
            } catch (IllegalArgumentException | IllegalStateException | PolyglotException | UnsupportedOperationException | NullPointerException e) {
                System.err.println("Error happens during invoke polyglot function: ");
                e.printStackTrace();
                resultString = e.getMessage();
            }
        }
        return resultString;
    }
}
