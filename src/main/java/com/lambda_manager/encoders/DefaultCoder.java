package com.lambda_manager.encoders;

@SuppressWarnings("unused")
public class DefaultCoder implements Coder {

    @Override
    public String encodeFunctionName(String username, String functionName) {
        return username + "_" + functionName;
    }

    @Override
    public String[] decode(String encodedName) {
        return encodedName.split("_", 2);
    }

    @Override
    public String decodeUsername(String encodedName) {
        return encodedName.split("_", 2)[0];
    }

    @Override
    public String decodeFunctionName(String encodedName) {
        return encodedName.split("_", 2)[1];
    }
}
