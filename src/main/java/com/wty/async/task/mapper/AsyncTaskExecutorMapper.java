package com.wty.async.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wty.async.task.domain.AsyncTaskExecutor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AsyncTaskExecutorMapper extends BaseMapper<AsyncTaskExecutor> {
    @Select("select * from async_task_executor where executor = #{executor}")
    AsyncTaskExecutor selectByExecutor(String executorName);

    @Select("select * from async_task_executor")
    List<AsyncTaskExecutor> listAll();
}
