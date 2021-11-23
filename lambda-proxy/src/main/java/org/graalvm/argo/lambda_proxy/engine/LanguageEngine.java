package org.graalvm.argo.lambda_proxy.engine;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.graalvm.argo.lambda_proxy.base.FunctionRegistrationFailure;
import org.graalvm.argo.lambda_proxy.base.IsolateObjectWrapper;
import org.graalvm.polyglot.PolyglotException;

import com.sun.net.httpserver.HttpServer;

public interface LanguageEngine {

    /**
     * Launch function invocation with given arguments
     *
     * @param functionName  function name of target function being invoked
     * @param jsonArguments Json encoded string as invocation arguments
     * @return return Json encoded String that contains invocation result and execution time
     */
    String invoke(String functionName, String jsonArguments) throws InvocationTargetException, IllegalAccessException,
            IOException, ClassNotFoundException, NoSuchMethodException, PolyglotException;

    /**
     * Register function into worker isolate
     *
     * @param functionName  function being registered
     * @param targetIsolate target worker isolate
     */
    default void registerFunction(String functionName, IsolateObjectWrapper targetIsolate) throws FunctionRegistrationFailure {

    }

    /**
     * Clean up all related states related to isolate that is going to be torn down.
     *
     * @param isolateObjectWrapper Target worker isolate, that is going to be cleaned up
     */
    default void cleanUp(IsolateObjectWrapper isolateObjectWrapper) {
    }

    /**
     * Register path handlers for the corresponding Http server. For Polyglot function we need to
     * register additional path "/register" and "/deregister" for function management
     *
     * @param server reference of server, whose handlers would be registered
     */
    default void registerHandler(HttpServer server) {

    }

}
