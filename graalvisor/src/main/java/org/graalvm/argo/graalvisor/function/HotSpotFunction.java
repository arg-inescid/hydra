package org.graalvm.argo.graalvisor.function;

import java.lang.reflect.Method;

public class HotSpotFunction extends PolyglotFunction {

    private final Method method;

    public HotSpotFunction(String name, String entryPoint, String language, Method method, int cpuCgroupQuota) {
        super(name, entryPoint, language, cpuCgroupQuota);
        this.method = method;
    }

    public Method getMethod() {
        return this.method;
    }
}
