package com.wty.async.task.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AsyncTaskExecutorTypeEnum {
    DEMO(1, "demo"),
    ;

    private int code;
    private String desc;
}
