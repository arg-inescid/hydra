package org.graalvm.argo.lambda_manager.encoders;

public interface Coder {

    String encodeFunctionName(String username, String functionName);

    String[] decode(String encodedName);

    String decodeUsername(String encodedName);

    String decodeFunctionName(String encodedName);
}
