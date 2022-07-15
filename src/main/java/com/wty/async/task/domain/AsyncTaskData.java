package com.wty.async.task.domain;

import lombok.Data;

@Data
public class AsyncTaskData {
    private Long id;
    private Integer type; // 执行器类型
    private Long bizId; // 业务id
    private String bizData; // 业务数据
    private Integer status; // 执行状态
    private Long planTime; // 计划执行时间
    private Long startTime; // 最近一次执行开始时间
    private Long endTime; // 最近一次执行结束时间
    private Integer executeCount; // 累计执行次数
    private String executor; // 执行器标识
    private String traceId; // 执行记录traceId
    private String creator; // 任务创建人
    private Long createTime; // 任务创建时间
    private Long updateTime; // 任务更新时间

}
