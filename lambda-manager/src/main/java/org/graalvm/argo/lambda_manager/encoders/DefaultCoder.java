package org.graalvm.argo.lambda_manager.encoders;

@SuppressWarnings("unused")
public class DefaultCoder implements Coder {

    private static final String SEPARATOR = "-";

    @Override
    public String encodeFunctionName(String username, String functionName) {
        return username + SEPARATOR + functionName;
    }

    @Override
    public String[] decode(String encodedName) {
        return encodedName.split(SEPARATOR, 2);
    }

    @Override
    public String decodeUsername(String encodedName) {
        return encodedName.split(SEPARATOR, 2)[0];
    }

    @Override
    public String decodeFunctionName(String encodedName) {
        return encodedName.split(SEPARATOR, 2)[1];
    }
}
