package com.wty.async.task.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AsyncTaskStatusEnum {
    READY(1, "待执行"),
    RUNNING(2, "执行中"),
    SUCCESS(3, "成功"),
    FAIL(4, "失败"),
    CANCEL(5, "已取消"),
    FAIL_TIMEOUT(6, "超时失败"),
    ;

    private int code;
    private String desc;
}
