package com.wty.async.task.domain;

import lombok.Data;

@Data
public class AsyncTaskExecutor {
    private Long id;
    private String executor; // host:type:index
    private Long taskId; // 当前执行中的任务ID
    private Long createTime;
    private Long updateTime;
}
