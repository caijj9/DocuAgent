package com.docuagent.docuagent.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T>{
    public static <T> Result<T> success(T data){
        return new Result<>(0, "成功", data);      // ← 直接构造
    }
    public static <T> Result<T> error(int code, String msg){
        return new Result<>(code, msg, null);      // ← 直接构造
    }
    private int code;
    private String msg;
    private T data;


}
