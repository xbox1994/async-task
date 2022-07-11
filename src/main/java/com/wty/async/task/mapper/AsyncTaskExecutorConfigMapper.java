package com.wty.async.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wty.async.task.domain.AsyncTaskExecutorConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AsyncTaskExecutorConfigMapper extends BaseMapper<AsyncTaskExecutorConfig> {
    @Select("select * from async_task_executor_config where type = #{type} limit 1")
    AsyncTaskExecutorConfig selectByType(String type);
}
