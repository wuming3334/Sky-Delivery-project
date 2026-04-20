package com.sky.exception;


public class PermissionDeniedException extends BaseException {
    public PermissionDeniedException() {
    }

    public PermissionDeniedException(String msg) {
        super(msg);
    }
}