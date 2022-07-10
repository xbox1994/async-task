package com.wty.async.task.service;


import com.wty.async.task.data.AsyncTask;
import com.wty.async.task.executor.IAsyncTaskExecutor;
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
            final long sleepTime = 400;
            long sleepTimes = 0;
            // TODO: 创建执行器信息保存到数据库中
            while (running.get()) {
                try {
                    int size = doAsyncTaskDispatch();
                    if (size <= 0) {
                        log.info("current thread name:{}", Thread.currentThread().getName());
                        // 防止出错时高频执行CPU打满
                        if (sleepTimes <= 5) {
                            Thread.sleep(sleepTime);
                        } else if (sleepTimes <= 15) {
                            Thread.sleep(1000L);
                        } else {
                            Thread.sleep(3000L);
                        }
                        sleepTimes++;
                    } else {
                        sleepTimes = 0;
                    }
                } catch (Exception e) {
                    log.error("error", e);
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    public int doAsyncTaskDispatch() {
        // TODO:查数据库有哪些任务待执行，获取到具体执行器去执行
        IAsyncTaskExecutor asyncTaskExecutorDemo = executorMap.get("asyncTaskExecutorDemo");
        asyncTaskExecutorDemo.checkReady(new AsyncTask());
        asyncTaskExecutorDemo.execute(new AsyncTask());
        return 0;
    }
}
