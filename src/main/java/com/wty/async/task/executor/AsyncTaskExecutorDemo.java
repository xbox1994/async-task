package com.wty.async.task.executor;

import com.wty.async.task.data.AsyncTask;
import com.wty.async.task.enums.AsyncTaskExecutorTypeEnum;
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
        log.info("execute done demo");
        return true;
    }
}
