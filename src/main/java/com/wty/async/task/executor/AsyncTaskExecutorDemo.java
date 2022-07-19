package com.wty.async.task.executor;

import com.wty.async.task.data.AsyncTask;
import com.wty.async.task.domain.AsyncTaskData;
import com.wty.async.task.enums.AsyncTaskExecutorTypeEnum;
import com.wty.async.task.mapper.AsyncTaskDataMapper;
import com.wty.async.task.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class AsyncTaskExecutorDemo implements IAsyncTaskExecutor {
    @Resource
    private AsyncTaskDataMapper asyncTaskDataMapper;

    @Override
    public AsyncTaskExecutorTypeEnum getType() {
        return AsyncTaskExecutorTypeEnum.DEMO;
    }

    @Override
    public boolean execute(AsyncTask asyncTask) {
        Long bizId = asyncTask.getBizId();
        AsyncTaskData asyncTaskData = asyncTaskDataMapper.selectByBizId(bizId);
        if (asyncTaskData == null) {
            return false;
        }
        log.info("execute demo start: {}", asyncTask);
        // 可通过bizId查询业务相关数据，使用bizData作为当前任务执行参数
        String bizData = asyncTask.getBizData();
        // 假设任务正常执行
        if (bizId != null && bizData != null) {
            // 执行具体耗时业务操作
            ThreadUtils.sleep(ThreadUtils.SLEEP_TIME_3S);

            // 更新任务结果
            asyncTaskData.setResult("done");
            asyncTaskDataMapper.updateById(asyncTaskData);
            log.info("execute demo done: {}", asyncTask);
            return true;
        }
        asyncTaskData.setResult("fail");
        asyncTaskDataMapper.updateById(asyncTaskData);
        log.info("execute demo fail: {}", asyncTask);
        return false;
    }
}
