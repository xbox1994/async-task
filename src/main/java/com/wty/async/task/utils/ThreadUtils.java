package com.wty.async.task.utils;

public class ThreadUtils {
    public static final long SLEEP_TIME_400MS = 400;
    public static final long SLEEP_TIME_1S = 1000;
    public static final long SLEEP_TIME_3S = 3000;

    public static final long SLEEP_TIME_1M = 60000;

    public static final long SLEEP_TIMES_5 = 5;
    public static final long SLEEP_TIMES_15 = 15;


    public static void sleep(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
