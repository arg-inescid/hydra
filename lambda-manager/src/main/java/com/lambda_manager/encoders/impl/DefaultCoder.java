package com.lambda_manager.encoders.impl;

import com.lambda_manager.encoders.Coder;

@SuppressWarnings("unused")
public class DefaultCoder implements Coder {

    @Override
    public String encode(String username, String lambdaName) {
        return username + "_" + lambdaName;
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
