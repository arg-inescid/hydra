package com.serverless_demo.collectors.lambda_info;

import java.util.Timer;

public class LambdaInstanceInfo {

    private final int id;
    private int openRequestCount;
    private String args;
    private Timer timer;

    public LambdaInstanceInfo(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
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
}
