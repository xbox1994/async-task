package com.wty.async.task.executor;

import com.wty.async.task.data.AsyncTask;
import com.wty.async.task.enums.AsyncTaskExecutorTypeEnum;

public interface IAsyncTaskExecutor {
    AsyncTaskExecutorTypeEnum getType();
    default int checkReady(AsyncTask asyncTask){
        return 0;
    }
    boolean execute(AsyncTask asyncTask);
    default long cleanDataTime(){
        return 0L;
    }
    default int maxExecuteCount(){
        return -1;
    }
}
