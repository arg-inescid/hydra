package org.graalvm.argo.graalvisor.engine;

import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.argo.graalvisor.base.PolyglotFunction;

public class FunctionStorage {
	/**
     *  FunctionTable is used to store registered functions inside default and worker isolates.
     */
    public static final ConcurrentHashMap<String, PolyglotFunction> FTABLE = new ConcurrentHashMap<>();
}
