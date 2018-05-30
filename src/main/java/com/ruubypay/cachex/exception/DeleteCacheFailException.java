package com.ruubypay.cachex.exception;

/**
 * 删除缓存失败抛出异常
 * @author chenhaiyang
 */
public class DeleteCacheFailException extends RuntimeException{

    public DeleteCacheFailException(String msg){
        super(msg);
    }
}
