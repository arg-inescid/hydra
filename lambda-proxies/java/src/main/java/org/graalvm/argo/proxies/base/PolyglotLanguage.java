package org.graalvm.argo.proxies.base;

public enum PolyglotLanguage {
    JAVASCRIPT("js"),
    PYTHON("python");

    private final String language;

    PolyglotLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return this.language;
    }
}
