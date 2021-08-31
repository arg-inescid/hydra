package com.lambda_manager.utils.logger;

public class ElapseTimer {

    private static long INIT_TIME;

    public static void init() {
        INIT_TIME = System.currentTimeMillis();
    }

    public static long elapsedTime() {
        if (INIT_TIME == 0) {
            return 0;
        } else {
            return System.currentTimeMillis() - INIT_TIME;
        }
    }
}
