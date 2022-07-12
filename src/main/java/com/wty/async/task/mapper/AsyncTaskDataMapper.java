package com.wty.async.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wty.async.task.domain.AsyncTaskExecutorConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AsyncTaskDataMapper extends BaseMapper<> {
}
