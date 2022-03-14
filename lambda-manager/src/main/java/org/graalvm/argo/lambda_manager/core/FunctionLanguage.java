package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.utils.Messages;

public enum FunctionLanguage {
    NATIVE_JAVA("NATIVE_JAVA"),
    JAVA("JAVA"),
    PYTHON("PYTHON"),
    JAVASCRIPT("JAVASCRIPT");

    private final String language;

    FunctionLanguage(String language) {
        this.language = language;
    }

    public static FunctionLanguage fromString(String text) throws Exception {
        for (FunctionLanguage b : FunctionLanguage.values()) {
            if (b.language.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new Exception(String.format(Messages.ERROR_FUNCTION_LANG, text));
    }

    @Override
    public String toString() {
        return this.language;
    }
}
