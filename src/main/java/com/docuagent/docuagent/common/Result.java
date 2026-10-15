package com.docuagent.docuagent.common;

public record Result<T>(int code, String msg, T data) {
}
