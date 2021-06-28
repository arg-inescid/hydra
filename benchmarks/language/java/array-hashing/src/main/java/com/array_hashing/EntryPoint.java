package com.array_hashing;

import io.micronaut.runtime.Micronaut;

public class EntryPoint {

    public static void main(String[] args) {
        ArgumentStorage.getServerArgumentStorage().parseArguments(args);
        Micronaut.run(EntryPoint.class, args);
    }
}
