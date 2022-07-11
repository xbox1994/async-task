package com.wty.async.task.domain;

import lombok.Data;

@Data
public class AsyncTaskExecutorConfig {
    private Long id;
    private String type;
    private Integer parallelCurrent;
    private Integer parallelMax;
    private Integer status;
    private Long nextTime;
    private String owners;
}
