package com.wty.async.task.service;


import com.wty.async.task.domain.AsyncTaskData;
import com.wty.async.task.domain.AsyncTaskExecutorConfig;
import com.wty.async.task.executor.IAsyncTaskExecutor;
import com.wty.async.task.mapper.AsyncTaskDataMapper;
import com.wty.async.task.mapper.AsyncTaskExecutorConfigMapper;
import com.wty.async.task.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AsyncTaskDispatchService {
    public static final int POOL_SIZE = 10;
    @Value("${async.task.enable:false}")
    private Boolean enable;
    private String processName;
    private ExecutorService executorService;
    private AtomicBoolean running = new AtomicBoolean(true);
    @Autowired
    private Map<String, IAsyncTaskExecutor> executorMap;
    @Autowired
    private AsyncTaskExecutorConfigMapper asyncTaskExecutorConfigMapper;
    @Autowired
    private AsyncTaskDataMapper asyncTaskDataMapper;

    @EventListener
    public void init(ContextRefreshedEvent event) {
        log.info("init {}", event);
        if (!enable) {
            log.info("异步任务执行器未启用");
        }

        String host = System.getenv("HOSTNAME");
        String timeStr = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        processName = String.format("%s:%s", host, timeStr);


        int poolSize = POOL_SIZE;
        System.out.println(getClass().getCanonicalName());
        AsyncTaskExecutorConfig asyncTaskExecutorConfig = asyncTaskExecutorConfigMapper.selectByType(getClass().getCanonicalName());
        if (asyncTaskExecutorConfig != null) {
            poolSize = asyncTaskExecutorConfig.getParallelMax();
        }

        executorService = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("AsyncTask-" + t.getId());
            return t;
        });
        run();
    }

    /**
     * 第一层：提交死循环任务到线程池永久执行任务分配，直到标志位为false，停止循环
     */
    private void run() {
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

    /**
     * 第二层：查询可执行的执行器，对每个执行器都尝试执行任务调度
     *
     * @return
     */
    public int doAsyncTaskDispatch() {
        if (getIdleSize() > 0) {
            List<AsyncTaskExecutorConfig> asyncTaskExecutorConfigs = asyncTaskExecutorConfigMapper.listForDispatch(System.currentTimeMillis());
            int size = 0;
            if (asyncTaskExecutorConfigs.isEmpty()) {
                return size;
            }
            for (AsyncTaskExecutorConfig asyncTaskExecutorConfig : asyncTaskExecutorConfigs) {
                size += doAsyncTaskDispatch(asyncTaskExecutorConfig);
            }
            return size;
        }
        // 没有空闲的线程，不执行
        return 0;
    }

    /**
     * 第三层：单个执行器尝试执行任务
     *
     * @param asyncTaskExecutorConfig
     * @return
     */
    private int doAsyncTaskDispatch(AsyncTaskExecutorConfig asyncTaskExecutorConfig) {
        IAsyncTaskExecutor taskExecutor = executorMap.get(asyncTaskExecutorConfig.getType());
        if (taskExecutor == null) {
            log.info("没有找到该类型的执行器：{}", asyncTaskExecutorConfig.getType());
            return 0;
        }
        // 必须每次都判断一次，因为外层for循环会执行本方法多次
        int idleSize = getIdleSize();
        List<Long> ids = new ArrayList<>();
        if (idleSize > 0) {
            Long lastTime = asyncTaskExecutorConfig.getNextTime();
            long nextTime = System.currentTimeMillis() - ThreadUtils.SLEEP_TIME_1M;
            // 保证1分钟内不会再次执行执行任务的部分
//            int lockCount = asyncTaskExecutorConfigMapper.updateConfigTime(asyncTaskExecutorConfig.getId(), lastTime, nextTime);
            log.info("正在执行的执行器id：{}", asyncTaskExecutorConfig.getId());
//            if (lockCount > 0) {
                // TODO: fixAbnormal 异常任务
                int max = Math.min(idleSize, asyncTaskExecutorConfig.getParallelMax() - asyncTaskExecutorConfig.getParallelCurrent());
                if (max > 0) {
                    // 查询异步任务数据表，获取执行器类型对应的需要执行的任务数据
                    List<AsyncTaskData> asyncTaskData = asyncTaskDataMapper.loadForExecuteByType(taskExecutor.getType().getCode(), System.currentTimeMillis(), max);
                    for (AsyncTaskData data : asyncTaskData) {
                        // 记录本次执行的任务id
                        ids.add(data.getId());
                        // 执行器的正在执行数量+1
                        asyncTaskExecutorConfigMapper.updateForStartTask(asyncTaskExecutorConfig.getId());
                        // 执行任务
                        executorService.submit(() -> {
                            try {
                                doTask(data, taskExecutor, asyncTaskExecutorConfig);
                            } catch (Exception e) {
                                log.error("doTask", e);
                            } finally {
                                asyncTaskExecutorConfigMapper.updateForFinishTask(asyncTaskExecutorConfig.getId());
                            }
                        });
                    }
                }
                asyncTaskExecutorConfigMapper.updateConfigTime(asyncTaskExecutorConfig.getId(), nextTime, System.currentTimeMillis());
                if (ids.size() > 0) {
                    log.info("doAsyncTaskDispatch done: {}, {}, {}", asyncTaskExecutorConfig.getType(), ids, getIdleSize());
                }
//            }

        }
        return ids.size();
    }

    /**
     * 第四层：执行任务
     * @param data
     * @param taskExecutor
     * @param asyncTaskExecutorConfig
     */
    private void doTask(AsyncTaskData data, IAsyncTaskExecutor taskExecutor, AsyncTaskExecutorConfig asyncTaskExecutorConfig) {

    }

    private int getIdleSize() {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        return threadPoolExecutor.getMaximumPoolSize() - threadPoolExecutor.getActiveCount();
    }
}
