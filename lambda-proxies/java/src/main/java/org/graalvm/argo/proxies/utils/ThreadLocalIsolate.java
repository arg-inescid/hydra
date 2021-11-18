package org.graalvm.argo.proxies.utils;

import org.graalvm.argo.proxies.base.IsolateObjectWrapper;

public class ThreadLocalIsolate {

    public static final ThreadLocal<IsolateObjectWrapper> threadLocalIsolate = new ThreadLocal<>();

}
