package com.wty.async.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wty.async.task.domain.AsyncTaskExecutorConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AsyncTaskExecutorConfigMapper extends BaseMapper<AsyncTaskExecutorConfig> {
    @Select("select * from async_task_executor_config where type = #{type} limit 1")
    AsyncTaskExecutorConfig selectByType(String type);

    // TODO: 异常执行任务重新调度需要改这个SQL
    @Select("select * from async_task_executor_config where status = 1 and #{currentTime} > next_time and next_time > 0 " +
            "and parallel_current < parallel_max")
    List<AsyncTaskExecutorConfig> listForDispatch(Long currentTime);

    @Update("update async_task_executor_config set next_time = #{nextTime} where id = #{id} and next_time = #{lastTime} " +
            "and status = 1 and next_time > 0")
    int updateConfigTime(Long id, Long lastTime, Long nextTime);

    @Update("update async_task_executor_config set parallel_current = parallel_current + 1 where id = #{id}")
    void updateForStartTask(Long id);

    @Update("update async_task_executor_config set parallel_current = parallel_current - 1 where id = #{id}")
    void updateForFinishTask(Long id);
}
