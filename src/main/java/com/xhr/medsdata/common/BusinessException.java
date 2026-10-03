package com.xhr.medsdata.common;

/**
 * 业务异常，事务会因 RuntimeException 回滚。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
