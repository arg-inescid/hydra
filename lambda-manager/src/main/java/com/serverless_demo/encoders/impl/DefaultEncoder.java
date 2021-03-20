package com.serverless_demo.encoders.impl;

import com.serverless_demo.encoders.Encoder;

public class DefaultEncoder implements Encoder {
    @Override
    public String encode(String username, String lambdaName) {
        return username + "_" + lambdaName;
    }
}
