package com.lambda_manager.collectors.lambda_info;

import com.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import java.util.Timer;

public class LambdaInstanceInfo {

    private int id;
    private int openRequestCount;
    private String args;
    private Timer timer;
    private boolean updated = false;
    private ConnectionTriplet<String, String, RxHttpClient> connectionTriplet;

    public LambdaInstanceInfo(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getOpenRequestCount() {
        return openRequestCount;
    }

    public void setOpenRequestCount(int openRequestCount) {
        this.openRequestCount = openRequestCount;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public ConnectionTriplet<String, String, RxHttpClient> getConnectionTriplet() {
        return connectionTriplet;
    }

    public void setConnectionTriplet(ConnectionTriplet<String, String, RxHttpClient> connectionTriplet) {
        this.connectionTriplet = connectionTriplet;
    }

    public void shouldUpdateID(LambdaInstancesInfo lambdaInstancesInfo) {
        if (lambdaInstancesInfo.isUpdateID() && !updated) {
            id = lambdaInstancesInfo.getId();
            updated = true;
        }
    }
}
