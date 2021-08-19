package com.lambda_manager.encoders;

public interface Coder {

    String encode(String username, String lambdaName);

    String[] decode(String encodedName);

    String decodeUsername(String encodedName);

    String decodeFunctionName(String encodedName);
}
