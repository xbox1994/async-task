package com.wty.async.task.controller;

import com.wty.async.task.data.RestRsp;
import com.wty.async.task.enums.AsyncTaskExecutorTypeEnum;
import com.wty.async.task.service.AsyncTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/async/task")
public class AsyncTaskController {
    @Autowired
    private AsyncTaskService asyncTaskService;

    @PostMapping("create")
    public RestRsp<Boolean> createTask() {
        asyncTaskService.addTask(AsyncTaskExecutorTypeEnum.DEMO, 1L, "{'k':1, 'v':2}", System.currentTimeMillis());
        return RestRsp.success(false);
    }
}
