package com.wty.async.task.data;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class RestRsp<T>{
    private int status;
    private String message;
    private T data;
    private String traceId; // TODO: 使用返回的traceId查到执行任务信息
    private long timestamp = System.currentTimeMillis();

    public static <T> RestRsp<T> success(T data){
        RestRsp<T> rsp = new RestRsp<>();
        rsp.status = HttpStatus.OK.value();
        rsp.data = data;
        rsp.message = HttpStatus.OK.getReasonPhrase();
        return rsp;
    }
}
