package org.graalvm.argo.graalvisor.base;

import org.graalvm.nativeimage.Isolate;
import org.graalvm.nativeimage.IsolateThread;

public class IsolateObjectWrapper implements Comparable<IsolateObjectWrapper> {

    private final Isolate isolate;
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

    public void setIsolateThread(IsolateThread isolateThread) {
        this.isolateThread = isolateThread;
    }

    @Override
    public int compareTo(IsolateObjectWrapper o) {
        return (this.isolate.rawValue() == o.getIsolate().rawValue()) ? 0 : 1;
    }

}