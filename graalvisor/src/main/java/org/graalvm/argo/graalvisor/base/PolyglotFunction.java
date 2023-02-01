package org.graalvm.argo.graalvisor.base;

import static org.graalvm.argo.graalvisor.Proxy.APP_DIR;

import java.io.FileNotFoundException;
import java.util.Locale;

import org.graalvm.polyglot.Engine;

import com.oracle.svm.graalvisor.api.GraalVisorAPI;

public class PolyglotFunction {
    private final String name;
    private final String entryPoint;
    private final PolyglotLanguage language;
    private final String source;
    private GraalVisorAPI graalVisorAPI;
    private Engine engine;

    public PolyglotFunction(String name, String entryPoint, String language, String source) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.language = PolyglotLanguage.valueOf(language.toUpperCase(Locale.ROOT));
        this.source = source;
        if (this.language.equals(PolyglotLanguage.JAVA)) {
            try {
                this.graalVisorAPI = new GraalVisorAPI(APP_DIR + name);
            } catch (FileNotFoundException e) {
                System.err.println("SO file not found.");
                e.printStackTrace();
            }
        } else {
            this.engine = Engine.create();
        }
    }

    public GraalVisorAPI getGraalVisorAPI() {
        return graalVisorAPI;
    }

    public Engine getTruffleEngine() {
        return engine;
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
