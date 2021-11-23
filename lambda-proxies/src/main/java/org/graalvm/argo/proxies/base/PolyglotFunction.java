package org.graalvm.argo.proxies.base;

import org.graalvm.polyglot.Value;

public class PolyglotFunction {
    private String language;
    private String name;
    private String sourceCode;
    private Value evaluatedFunction;

    public PolyglotFunction(String name, String languageCode, String sourceCode) {
        this.language = languageCode;
        this.name = name;
        this.sourceCode = sourceCode;
    }

    public Value getEvaluatedFunction() {
        return evaluatedFunction;
    }

    public void setEvaluatedFunction(Value evaluatedFunction) {
        this.evaluatedFunction = evaluatedFunction;
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
