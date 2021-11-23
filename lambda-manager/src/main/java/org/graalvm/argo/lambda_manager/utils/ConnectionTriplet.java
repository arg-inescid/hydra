package org.graalvm.argo.lambda_manager.utils;

public class ConnectionTriplet<X, Y, Z> {
    public final X ip;
    public final Y tap;
    public final Z client;

    public ConnectionTriplet(X ip, Y tap, Z client) {
        this.ip = ip;
        this.tap = tap;
        this.client = client;
    }
}
