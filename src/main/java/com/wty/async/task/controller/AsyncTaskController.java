package com.wty.async.task.controller;

import com.wty.async.task.data.RestRsp;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/async/task")
public class AsyncTaskController {

    @PostMapping("create")
    public RestRsp<Boolean> createTask() {
        return RestRsp.success(false);
    }
}
