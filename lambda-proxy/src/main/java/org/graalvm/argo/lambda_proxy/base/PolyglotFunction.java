package org.graalvm.argo.lambda_proxy.base;

import static org.graalvm.argo.lambda_proxy.PolyglotProxy.APP_DIR;

import java.io.FileNotFoundException;
import java.util.Locale;

import com.oracle.svm.graalvisor.api.GraalVisorAPI;

public class PolyglotFunction {
    private String name;
    private String entryPoint;
    private PolyglotLanguage language;
    private String source;
    private GraalVisorAPI graalVisorAPI;

    public PolyglotFunction(String name, String entryPoint, String language, String source) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.language = PolyglotLanguage.valueOf(language.toUpperCase(Locale.ROOT));
        if (this.language.equals(PolyglotLanguage.JAVA)) {
            try {
                this.graalVisorAPI = new GraalVisorAPI(APP_DIR + name);
            } catch (FileNotFoundException e) {
                System.err.println("SO file not found.");
                e.printStackTrace();
            }
        } else {
            this.source = source;
        }
        System.out.println("PolyglotFunction created");
    }

    public GraalVisorAPI getGraalVisorAPI() {
        return graalVisorAPI;
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
