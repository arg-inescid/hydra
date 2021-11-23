package org.graalvm.argo.lambda_proxy.utils;

import org.graalvm.argo.lambda_proxy.base.IsolateObjectWrapper;

public class ThreadLocalIsolate {

    public static final ThreadLocal<IsolateObjectWrapper> threadLocalIsolate = new ThreadLocal<>();

}
