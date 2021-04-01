package com.lambda_manager.encoders.impl;

import com.lambda_manager.encoders.Encoder;

@SuppressWarnings("unused")
public class DefaultEncoder implements Encoder {

    @Override
    public String encode(String username, String lambdaName) {
        return username + "_" + lambdaName;
    }
}
