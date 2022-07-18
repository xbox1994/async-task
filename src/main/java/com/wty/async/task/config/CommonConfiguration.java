package com.wty.async.task.config;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.wty.async.task.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

@Configuration
@EnableCaching
@Slf4j
public class CommonConfiguration {
    @Autowired
    private List<IServiceClose> serviceCloses;

    @EventListener
    public void close(ContextClosedEvent event){
        if (CollectionUtils.isNotEmpty(serviceCloses)){
            for (IServiceClose serviceClose : serviceCloses) {
                serviceClose.close();
                log.info(serviceClose.getClass().getCanonicalName() + "关闭完成");
            }
        }
        log.info("服务开始停止，大约需要: {} 秒", ThreadUtils.SLEEP_TIMES_5);
        ThreadUtils.sleep(ThreadUtils.SLEEP_TIME_5S);
        log.info("服务停止完成");
    }
}
