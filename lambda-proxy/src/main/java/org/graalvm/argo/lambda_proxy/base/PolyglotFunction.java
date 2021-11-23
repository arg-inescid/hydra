package org.graalvm.argo.lambda_proxy.base;

import org.graalvm.polyglot.Source;

public class PolyglotFunction {
    private final String name;
    private final String entryPoint;
    private final String language;
    private final String sourceCode;
    private Source evaluatedSource;

    public PolyglotFunction(String name, String entryPoint, String language, String sourceCode) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.language = language;
        this.sourceCode = sourceCode;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public Source getSource() {
        return evaluatedSource;
    }

    public void setSource(Source evaluatedSource) {
        this.evaluatedSource = evaluatedSource;
    }

    public String getLanguage() {
        return language;
    }

    public String getName() {
        return name;
    }

    public String getSourceCode() {
        return sourceCode;
    }

}
