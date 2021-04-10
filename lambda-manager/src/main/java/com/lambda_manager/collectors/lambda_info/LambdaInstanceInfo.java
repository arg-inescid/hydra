package com.lambda_manager.collectors.lambda_info;

import io.micronaut.http.client.RxHttpClient;

import java.util.Timer;

public class LambdaInstanceInfo {

    private final int id;
    private int openRequestCount;
    private String args;
    private Timer timer;
    private String tap;
    private String ip;
    private RxHttpClient client;

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

    public String getTap() {
        return tap;
    }

    public void setTap(String tap) {
        this.tap = tap;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public RxHttpClient getClient() {
        return client;
    }

    public void setClient(RxHttpClient client) {
        this.client = client;
    }
}
