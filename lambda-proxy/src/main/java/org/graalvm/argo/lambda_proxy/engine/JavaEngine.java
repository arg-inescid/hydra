package org.graalvm.argo.lambda_proxy.engine;

import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.json;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.graalvm.argo.lambda_proxy.base.IsolateObjectWrapper;

public class JavaEngine implements LanguageEngine {
    private static Method method;

    public static void setFunctionName(String functionName) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> cls = Class.forName(functionName);
        method = cls.getMethod("main", Map.class);
    }

    @Override
    public String invoke(String functionName, String arguments) throws InvocationTargetException, IllegalAccessException, IOException, ClassNotFoundException, NoSuchMethodException {
        if (method == null) {
            setFunctionName(functionName);
        }
        // Current assumption is each JavaEngine has only one registered function.
        return json.asString(method.invoke(null, new Object[]{json.mapFrom(arguments)}));
    }

    @Override
    public void cleanUp(IsolateObjectWrapper isolateObjectWrapper) {

    }

}
