package com.wty.async.task.data;

import lombok.Data;

@Data
public class AsyncTask {
    private long id;
    private int executeCount;
    private Long planTime;
    private Long startTime;
    private Long bizId;
    private String bizData;
    private Object taskTempData;
}
