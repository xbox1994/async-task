package com.wty.async.task.service;


import com.wty.async.task.data.AsyncTask;
import com.wty.async.task.executor.IAsyncTaskExecutor;
import com.wty.async.task.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AsyncTaskDispatchService {
    @Value("${async.task.enable:false}")
    private Boolean enable;
    private String processName;
    private ExecutorService executorService;
    private AtomicBoolean running = new AtomicBoolean(true);
    @Autowired
    private Map<String, IAsyncTaskExecutor> executorMap;

    @EventListener
    public void init(ContextRefreshedEvent event) {
        log.info("init {}", event);
        if (!enable) {
            log.info("异步任务执行器未启用");
        }

        String host = System.getenv("HOSTNAME");
        String timeStr = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        processName = String.format("%s:%s", host, timeStr);


        // TODO:从数据库中查询配置的线程数
        executorService = Executors.newFixedThreadPool(3, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("AsyncTask-" + t.getId());
            return t;
        });
        executorService.submit(() -> {
            log.info("start");
            final long sleepTime = ThreadUtils.SLEEP_TIME_400MS;
            long sleepTimes = 0;
            // TODO: 创建执行器信息保存到数据库中
            while (running.get()) {
                try {
                    int size = doAsyncTaskDispatch();
                    if (size <= 0) {
                        log.info("current thread name:{}", Thread.currentThread().getName());
                        // 防止出错时高频执行CPU打满
                        if (sleepTimes <= ThreadUtils.SLEEP_TIMES_5) {
                            ThreadUtils.sleep(sleepTime);
                        } else if (sleepTimes <= ThreadUtils.SLEEP_TIMES_15) {
                            ThreadUtils.sleep(ThreadUtils.SLEEP_TIME_1S);
                        } else {
                            ThreadUtils.sleep(ThreadUtils.SLEEP_TIME_3S);
                        }
                        sleepTimes++;
                    } else {
                        sleepTimes = 0;
                    }
                } catch (Exception e) {
                    log.error("error", e);
                    ThreadUtils.sleep(ThreadUtils.SLEEP_TIME_3S);
                }
            }
        });
    }

    public int doAsyncTaskDispatch() {
        // TODO:查数据库有哪些任务待执行，获取到具体执行器去执行
        IAsyncTaskExecutor asyncTaskExecutorDemo = executorMap.get("asyncTaskExecutorDemo");
        AsyncTask asyncTask = new AsyncTask();
        int checkReady = asyncTaskExecutorDemo.checkReady(asyncTask);
        if (checkReady == 0){
            boolean execute = asyncTaskExecutorDemo.execute(asyncTask);
            if (execute) {
                // TODO: 执行成功，修改数据库任务执行状态，任务结束，周期任务还可以执行下次任务
            }else {
                // TODO: 任务执行失败，需要重试策略：直接失败还是重试n次之后再失败
            }
        }
        return 0;
    }
}
