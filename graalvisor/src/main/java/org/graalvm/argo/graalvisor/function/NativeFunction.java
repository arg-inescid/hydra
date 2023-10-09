package org.graalvm.argo.graalvisor.function;

public class NativeFunction extends PolyglotFunction {

    private final String path;
    private final boolean lazyIsolation;

    public NativeFunction(String name, String entryPoint, String language, String path, boolean lazyIsolation) {
        super(name, entryPoint, language);
        this.path = path;
        this.lazyIsolation = lazyIsolation;
    }

    public String getPath() {
        return this.path;
    }

    public boolean hasLazyIsolation() {
        return lazyIsolation;
    }
}
