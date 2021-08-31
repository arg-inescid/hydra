package com.lambda_manager.collectors.meta_info;

import com.lambda_manager.utils.Messages;

public enum FunctionLanguage {
	JAVA("JAVA");

	private String language;

	FunctionLanguage(String language) {
        this.language = language;
    }

    public String toString() {
        return this.language;
    }

    public static FunctionLanguage fromString(String text) throws Exception {
        for (FunctionLanguage b : FunctionLanguage.values()) {
            if (b.language.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new Exception(String.format(Messages.ERROR_FUNCTION_LANG, text));
    }
}
