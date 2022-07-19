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

    @Update("update async_task_data set executor = #{executor}, execute_count = execute_count + 1, status = 2, start_time = #{startTime}, " +
            "update_time = #{startTime} where id = #{id} and update_time = #{updateTime}")
    int lockForExecute(Long id, long startTime, long updateTime, String executor);

    @Select("select async_task_data where biz_id = #{bizId}")
    AsyncTaskData selectByBizId(Long bizId);
}
