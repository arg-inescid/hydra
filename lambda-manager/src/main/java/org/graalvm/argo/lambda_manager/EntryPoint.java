package org.graalvm.argo.lambda_manager;

import io.micronaut.runtime.Micronaut;

public class EntryPoint {

    public static void main(String[] args) {
        Micronaut.run(EntryPoint.class, args);
    }
}
