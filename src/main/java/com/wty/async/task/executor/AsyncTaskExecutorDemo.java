package com.wty.async.task.executor;

import com.wty.async.task.data.AsyncTask;
import com.wty.async.task.enums.AsyncTaskExecutorTypeEnum;
import com.wty.async.task.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AsyncTaskExecutorDemo implements IAsyncTaskExecutor{
    @Override
    public AsyncTaskExecutorTypeEnum getType() {
        return AsyncTaskExecutorTypeEnum.DEMO;
    }

    @Override
    public boolean execute(AsyncTask asyncTask) {
        Long bizId = asyncTask.getBizId();
        // TODO: 数据库查询创建任务时的业务信息，执行具体耗时业务操作
        int businessSleepCount = 3000;
        ThreadUtils.sleep(businessSleepCount);
        log.info("execute done demo");
        return true;
    }
}
