package com.wty.async.task.service;

import com.wty.async.task.domain.AsyncTaskData;
import com.wty.async.task.enums.AsyncTaskExecutorTypeEnum;
import com.wty.async.task.enums.AsyncTaskStatusEnum;
import com.wty.async.task.mapper.AsyncTaskDataMapper;
import com.wty.async.task.utils.ObjectMapperUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AsyncTaskService {

    @Resource
    private AsyncTaskDataMapper asyncTaskDataMapper;

    public void addTask(AsyncTaskExecutorTypeEnum typeEnum, Long bizId, Object bizData, Long planTime) {
        AsyncTaskData data = new AsyncTaskData();
        data.setType(typeEnum.getCode());
        data.setBizId(bizId);
        if (bizData instanceof String) {
            data.setBizData(bizData.toString());
        } else {
            data.setBizData(ObjectMapperUtils.toJSON(bizData));
        }
        data.setBizData(bizData.toString());
        data.setStatus(AsyncTaskStatusEnum.READY.getCode());
        data.setPlanTime(planTime);
        data.setStartTime(0L);
        data.setEndTime(0L);
        data.setExecuteCount(0);
        data.setExecutor("");
        data.setTraceId(""); // TODO
        data.setCreator("xbox1994");
        data.setCreateTime(System.currentTimeMillis());
        data.setUpdateTime(data.getCreateTime());
        asyncTaskDataMapper.insert(data);
    }
}
