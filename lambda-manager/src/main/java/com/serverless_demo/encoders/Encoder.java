package com.serverless_demo.encoders;

public interface Encoder {
    String encode(String username, String lambdaName);
}
