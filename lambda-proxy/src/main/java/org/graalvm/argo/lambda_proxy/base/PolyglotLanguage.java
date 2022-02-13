package org.graalvm.argo.lambda_proxy.base;

public enum PolyglotLanguage {
    JAVASCRIPT("js"),
    PYTHON("python"),
    JAVA("java");

    private final String language;

    PolyglotLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return this.language;
    }
}
