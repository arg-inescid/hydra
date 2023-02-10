package org.graalvm.argo.graalvisor.base;

import java.util.Locale;

public class PolyglotFunction {
    private final String name;
    private final String entryPoint;
    private final PolyglotLanguage language;

    public PolyglotFunction(String name, String entryPoint, String language) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.language = PolyglotLanguage.valueOf(language.toUpperCase(Locale.ROOT));
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
}
