package org.graalvm.argo.proxies.base;

import org.graalvm.nativeimage.Isolate;
import org.graalvm.nativeimage.IsolateThread;

public class IsolateObjectWrapper implements Comparable<IsolateObjectWrapper> {

    private Isolate isolate;
    private IsolateThread isolateThread;

    public IsolateObjectWrapper(Isolate isolate, IsolateThread isolateThread) {
        this.isolate = isolate;
        this.isolateThread = isolateThread;
    }

    public Isolate getIsolate() {
        return isolate;
    }

    public IsolateThread getIsolateThread() {
        return isolateThread;
    }

    @Override
    public int compareTo(IsolateObjectWrapper o) {
        return (this.isolate.rawValue() == o.getIsolate().rawValue()) ? 0 : 1;
    }

}