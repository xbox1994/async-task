package com.wty.async.task.service;


import com.wty.async.task.data.AsyncTask;
import com.wty.async.task.domain.AsyncTaskData;
import com.wty.async.task.domain.AsyncTaskExecutor;
import com.wty.async.task.domain.AsyncTaskExecutorConfig;
import com.wty.async.task.enums.AsyncTaskStatusEnum;
import com.wty.async.task.executor.IAsyncTaskExecutor;
import com.wty.async.task.mapper.AsyncTaskDataMapper;
import com.wty.async.task.mapper.AsyncTaskExecutorConfigMapper;
import com.wty.async.task.mapper.AsyncTaskExecutorMapper;
import com.wty.async.task.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
    private AsyncTaskExecutorMapper asyncTaskExecutorMapper;
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
            // 创建当前执行器信息保存到数据库中，用-1作为特殊标识出当前执行器
            AsyncTaskExecutor asyncTaskExecutor = asyncTaskExecutorMapper.selectByExecutor(processName);
            if (asyncTaskExecutor != null) {
                asyncTaskExecutor.setTaskId(-1L);
                asyncTaskExecutor.setUpdateTime(System.currentTimeMillis());
                asyncTaskExecutorMapper.updateById(asyncTaskExecutor);
            } else {
                asyncTaskExecutor = new AsyncTaskExecutor();
                asyncTaskExecutor.setExecutor(processName);
                asyncTaskExecutor.setCreateTime(System.currentTimeMillis());
                asyncTaskExecutor.setUpdateTime(asyncTaskExecutor.getCreateTime());
                asyncTaskExecutor.setTaskId(-1L);
                asyncTaskExecutorMapper.insert(asyncTaskExecutor);
            }

            // 执行死循环调度，直到服务接收到退出消息，TODO IServiceClose接口
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
     * 第三层：单个执行器使用配置提交任务
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
        int abnormalSize = 0;
        List<Long> ids = new ArrayList<>();
        if (idleSize > 0) {
            Long lastTime = asyncTaskExecutorConfig.getNextTime();
            long nextTime = System.currentTimeMillis() - ThreadUtils.SLEEP_TIME_1M;
            // 保证1分钟内不会再次执行执行任务的部分
//            int lockCount = asyncTaskExecutorConfigMapper.updateConfigTime(asyncTaskExecutorConfig.getId(), lastTime, nextTime);
            log.info("正在执行的执行器id：{}", asyncTaskExecutorConfig.getId());
//            if (lockCount > 0) {
            // 因服务重启导致的异常任务处理，如果有重新调度的需要算到当前调度的数量中
            abnormalSize = fixAbnormal(asyncTaskExecutorConfig.getId(), taskExecutor);
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
                            doTask(data, taskExecutor);
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
        return ids.size() + abnormalSize;
    }

    private int fixAbnormal(Long id, IAsyncTaskExecutor taskExecutor) {
        List<AsyncTaskExecutor> asyncTaskExecutors = asyncTaskExecutorMapper.listAll();
        if (asyncTaskExecutors.isEmpty()) {
            return 0;
        }
        List<AsyncTaskExecutor> currentAsyncTaskExecutor = new ArrayList<>();
        List<String> activeExecutors = new ArrayList<>();
        for (AsyncTaskExecutor asyncTaskExecutor : asyncTaskExecutors) {
            if (asyncTaskExecutor.getTaskId() == null) {
                continue;
            }
            if (asyncTaskExecutor.getTaskId() > 0) {
                if (asyncTaskExecutor.getExecutor().startsWith(taskExecutor.getType().name() + ":")) {
                    currentAsyncTaskExecutor.add(asyncTaskExecutor);
                }
            } else {
                activeExecutors.add(asyncTaskExecutor.getExecutor());
            }
        }
        if (currentAsyncTaskExecutor.isEmpty()) {
            return 0;
        }
        int size = 0;
        // 查所有在执行的任务，如果有任务不是运行中的节点执行的，则是异常任务，需要重新调度
        for (AsyncTaskExecutor asyncTaskExecutor : currentAsyncTaskExecutor) {
            if (activeExecutors.stream().noneMatch(e -> e.contains(asyncTaskExecutor.getExecutor()))) {
                continue;
            }
            log.warn("重新调度执行异常任务：{}", asyncTaskExecutor);
            AsyncTaskData asyncTaskData = asyncTaskDataMapper.selectById(asyncTaskExecutor.getTaskId());
            if (asyncTaskData != null && asyncTaskData.getStatus() == AsyncTaskStatusEnum.RUNNING.getCode()) {
                // 异常任务还在执行中，则需要调度
                executorService.submit(() -> {
                    try {
                        doTask(asyncTaskData, taskExecutor);
                    } catch (Exception e) {
                        log.error("重新调度异常任务失败", e);
                    } finally {
                        asyncTaskExecutorConfigMapper.updateForFinishTask(id);
                    }
                });
                size++;
            }
            asyncTaskExecutorMapper.deleteById(asyncTaskExecutor.getId());
        }
        return size;
    }

    /**
     * 第四层：锁定单条任务记录并执行任务
     *
     * @param data
     * @param taskExecutor
     */
    private void doTask(AsyncTaskData data, IAsyncTaskExecutor taskExecutor) {
        String executorName = String.format("%s:%s:%s", taskExecutor.getType().getDesc(), processName, Thread.currentThread().getName());
        long startTime = System.currentTimeMillis();
        Long taskId = data.getId();
        int cnt = asyncTaskDataMapper.lockForExecute(taskId, startTime, data.getUpdateTime(), executorName);
        if (cnt > 0) {
            data.setStartTime(startTime);
            data.setUpdateTime(startTime);
            data.setStatus(AsyncTaskStatusEnum.RUNNING.getCode());
            data.setExecutor(executorName);
            data.setExecuteCount(data.getExecuteCount() + 1);
            // 每次执行时插入正在执行的执行器信息
            AsyncTaskExecutor asyncTaskExecutor = asyncTaskExecutorMapper.selectByExecutor(executorName);
            if (asyncTaskExecutor != null) {
                asyncTaskExecutor.setTaskId(taskId);
                asyncTaskExecutor.setUpdateTime(System.currentTimeMillis());
                asyncTaskExecutorMapper.updateById(asyncTaskExecutor);
            } else {
                asyncTaskExecutor = new AsyncTaskExecutor();
                asyncTaskExecutor.setExecutor(executorName);
                asyncTaskExecutor.setTaskId(taskId);
                asyncTaskExecutor.setCreateTime(System.currentTimeMillis());
                asyncTaskExecutor.setUpdateTime(asyncTaskExecutor.getCreateTime());
                asyncTaskExecutorMapper.insert(asyncTaskExecutor);
            }
            try {
                // 转换执行器任务部分数据为任务执行数据
                AsyncTask asyncTask = new AsyncTask();
                asyncTask.setExecuteCount(data.getExecuteCount());
                asyncTask.setPlanTime(data.getPlanTime());
                asyncTask.setStartTime(data.getStartTime());
                asyncTask.setBizId(data.getBizId());
                asyncTask.setBizData(data.getBizData());
                // 设置traceId为一个新的traceId
                MDC.put("traceId", data.getTraceId());
                // 执行执行器的checkReady方法，当ready之后执行execute方法，否则等待n秒后执行
                int checkReady = taskExecutor.checkReady(asyncTask);
                if (checkReady == 0) {
                    boolean success = taskExecutor.execute(asyncTask);
                    data.setStatus(success ? AsyncTaskStatusEnum.SUCCESS.getCode() : AsyncTaskStatusEnum.FAIL.getCode());
                    data.setEndTime(System.currentTimeMillis());
                    log.info("执行:{}, {}", success, data);
                } else if (checkReady > 0) {
                    data.setStatus(AsyncTaskStatusEnum.READY.getCode());
                    data.setStartTime(0L);
                    data.setEndTime(0L);
                    data.setExecutor("");
                    // 执行到这里的任务的当前事件肯定是大于原来的计划时间，查出来的
                    data.setPlanTime(System.currentTimeMillis() + checkReady * ThreadUtils.SLEEP_TIME_1S);
                    if (data.getExecuteCount() >= taskExecutor.maxExecuteCount()) {
                        // 任务最大执行次数，超过次数后会失败且报警
                        log.warn("任务最大执行次数，超过次数后会失败且报警: {}", data);
                    } else {
                        log.info("准备下次执行:{}", data);
                    }
                } else {
                    data.setStatus(AsyncTaskStatusEnum.CANCEL.getCode());
                    data.setEndTime(System.currentTimeMillis());
                    log.info("取消执行:{}", data);
                }
            } catch (Exception e) {
                // 如果任务执行异常，设置任务数据的状态和结束时间
                data.setStatus(AsyncTaskStatusEnum.FAIL.getCode());
                data.setEndTime(System.currentTimeMillis());
                log.error("任务执行异常: {}, {}", data, e.getMessage());
            } finally {
                // 最终执行：保存任务数据到数据库、删除正在执行的执行器信息、移除当前设置的traceId
                asyncTaskDataMapper.updateById(data);
                asyncTaskExecutorMapper.deleteById(asyncTaskExecutor.getId());
                MDC.remove("traceId");
                log.info("任务执行结束处理完成:{}", data);
            }
        }
    }

    private int getIdleSize() {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        return threadPoolExecutor.getMaximumPoolSize() - threadPoolExecutor.getActiveCount();
    }
}
