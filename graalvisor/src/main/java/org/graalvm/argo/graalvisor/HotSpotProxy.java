package org.graalvm.argo.graalvisor;

import static com.oracle.svm.graalvisor.utils.JsonUtils.json;

import java.lang.reflect.Method;
import java.util.Map;

import org.graalvm.argo.graalvisor.function.HotSpotFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;
import org.graalvm.argo.graalvisor.function.TruffleFunction;

import com.oracle.svm.graalvisor.utils.JsonUtils;
import com.oracle.svm.graalvisor.polyglot.PolyglotLanguage;

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
            FTABLE.put(entryPoint, function);
        }
    }

    @Override
    public String invoke(PolyglotFunction function, boolean cached, boolean warmup, String arguments) throws Exception {
        if (function.getLanguage() == PolyglotLanguage.JAVA) {
            HotSpotFunction hf = (HotSpotFunction) function;
            Method method = hf.getMethod();
            return json.asString(method.invoke(null, new Object[] { JsonUtils.jsonToMap(arguments) }));
        } else if (function instanceof TruffleFunction){
            TruffleFunction tf = (TruffleFunction) function;
            return RuntimeProxy.LANGUAGE_ENGINE.invoke(tf.getLanguage().toString(), tf.getSource(), tf.getEntryPoint(), arguments);
        } else {
            return String.format("{'Error': 'Function %s not registered or not truffle function!'}", function.getName());
        }
    }
}
