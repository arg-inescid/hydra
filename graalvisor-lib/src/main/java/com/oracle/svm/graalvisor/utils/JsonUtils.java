package com.oracle.svm.graalvisor.utils;

import com.fasterxml.jackson.jr.ob.JSON;

public class JsonUtils {
    public static final JSON json = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT);
}
