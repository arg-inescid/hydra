package org.graalvm.argo.lambda_proxy.base;

import java.util.Locale;

public class PolyglotFunction {
    private String name;
    private String entryPoint;
    private PolyglotLanguage language;
    private String source;

    public PolyglotFunction(String name, String entryPoint, String language, String source) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.language = PolyglotLanguage.valueOf(language.toUpperCase(Locale.ROOT));
        this.source = source;
        System.out.println("PolyglotFunction created");
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

    public String getSource() {
        return source;
    }

}
