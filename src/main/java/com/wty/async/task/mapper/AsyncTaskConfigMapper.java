package com.wty.async.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wty.async.task.domain.AsyncTaskConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AsyncTaskConfigMapper extends BaseMapper<AsyncTaskConfig> {
    @Select("select * from async_task_config")
    AsyncTaskConfig selectOne();
}
