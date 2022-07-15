package com.wty.async.task.executor;

import com.wty.async.task.data.AsyncTask;
import com.wty.async.task.enums.AsyncTaskExecutorTypeEnum;

public interface IAsyncTaskExecutor {
    AsyncTaskExecutorTypeEnum getType();

    /**
     * 检查当前任务是否可执行
     * @param asyncTask
     * @return
     */
    default int checkReady(AsyncTask asyncTask){
        return 0;
    }

    /**
     * 执行任务
     * @param asyncTask
     * @return
     */
    boolean execute(AsyncTask asyncTask);

    /**
     * 数据清理时间，删除plan_time小于返回时间执行成功的记录
     * @return
     */
    default long cleanDataTime(){
        return 0L;
    }

    /**
     * 任务最大执行次数，超过次数后会失败且报警
     * @return
     */
    default int maxExecuteCount(){
        return -1;
    }
}
