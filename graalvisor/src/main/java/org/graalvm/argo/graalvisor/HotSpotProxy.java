package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.utils.JsonUtils.json;

import java.lang.reflect.Method;
import java.util.Map;

import org.graalvm.argo.graalvisor.base.HotSpotFunction;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import org.graalvm.argo.graalvisor.base.PolyglotLanguage;
import org.graalvm.argo.graalvisor.engine.FunctionStorage;
import org.graalvm.argo.graalvisor.utils.JsonUtils;

/**
 * Runtime proxy that runs on HotSpot JVM. Right now we only support truffle
 * code in HotSpot but it can be extended by running Graalvisor runtimes
 * through JNI.
 */
public class HotSpotProxy extends RuntimeProxy {

    public HotSpotProxy(int port, String entryPoint) throws Exception {
        super(port);
        if (entryPoint != null) {
            Class<?> cls = Class.forName(entryPoint);
            Method method = cls.getMethod("main", Map.class);
            // Here function name doesn't matter, so we can put entryPoint as name
            HotSpotFunction function = new HotSpotFunction(entryPoint, entryPoint, PolyglotLanguage.JAVA.toString(), method);
            FunctionStorage.FTABLE.put(entryPoint, function);
        }
    }

    @Override
    public String invoke(PolyglotFunction function, boolean cached, String arguments) throws Exception {
        if (function.getLanguage() == PolyglotLanguage.JAVA) {
            HotSpotFunction hsFunction = (HotSpotFunction) function;
            Method method = hsFunction.getMethod();
            return json.asString(method.invoke(null, new Object[] { JsonUtils.jsonToMap(arguments) }));
        } else {
            return RuntimeProxy.LANGUAGE_ENGINE.invoke(function.getName(), arguments);
        }
    }
}
