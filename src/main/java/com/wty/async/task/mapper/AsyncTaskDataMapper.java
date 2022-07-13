package com.wty.async.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wty.async.task.domain.AsyncTaskData;
import com.wty.async.task.domain.AsyncTaskExecutorConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AsyncTaskDataMapper extends BaseMapper<AsyncTaskData> {
    @Select("select * from async_task_data where status = 1 and type = #{type} and plan_time > 0 and plan_time <= #{currentTime} " +
            "limit #{max}")
    List<AsyncTaskData> loadForExecuteByType(int type, long currentTime, int max);
}
