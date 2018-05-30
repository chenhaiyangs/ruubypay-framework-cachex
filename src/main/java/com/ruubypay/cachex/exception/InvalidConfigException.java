package com.ruubypay.cachex.exception;

/**
 * 配置无效时抛出的异常
 * @author chenhaiyang
 */
public class InvalidConfigException extends RuntimeException{

    public InvalidConfigException(String message) {
        super(message);
    }
}
