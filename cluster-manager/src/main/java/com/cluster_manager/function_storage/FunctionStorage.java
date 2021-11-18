package com.cluster_manager.function_storage;

import io.micronaut.context.BeanContext;

/**
 * The function storage 
 */
public interface FunctionStorage {
    String register(int allocate, String username, String functionName, String functionLanguage, String functionEntryPoint, String arguments, byte[] functionCode, BeanContext beanContext) throws Exception;

    String unregister(String username, String functionName, BeanContext beanContext) throws Exception;
}
