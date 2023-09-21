package org.graalvm.argo.graalvisor.function;

public class TruffleFunction extends PolyglotFunction {

    private final String source;

    public TruffleFunction(String name, String entryPoint, String language, String source, int cpuCgroupQuota) {
        super(name, entryPoint, language, cpuCgroupQuota);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
