package org.graalvm.argo.graalvisor.function;

import java.util.Locale;

import org.graalvm.argo.graalvisor.sandboxing.SandboxProvider;

import com.oracle.svm.graalvisor.polyglot.PolyglotLanguage;

public class PolyglotFunction {
    private final String name;
    private final String entryPoint;
    private final PolyglotLanguage language;
    private SandboxProvider sprovider;
    private final int cpuCgroupQuota;

    public PolyglotFunction(String name, String entryPoint, String language, int cpuCgroupQuota) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.language = PolyglotLanguage.valueOf(language.toUpperCase(Locale.ROOT));
        this.cpuCgroupQuota = cpuCgroupQuota;
    }

    public String getName() {
        return name;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public PolyglotLanguage getLanguage() {
        return language;
    }

    public void setSandboxProvider(SandboxProvider sprovider) {
        this.sprovider = sprovider;
    }

    public SandboxProvider getSandboxProvider() {
        return this.sprovider;
    }

    public int getCpuCgroupQuota() {
        return this.cpuCgroupQuota;
    }
}
