package com.ruubypay.cachex.to;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 缓存包装类
 * @author chenhaiyang
 */
@Data
public class CacheWrapper<T> implements Serializable, Cloneable{

    /**
     * 缓存对象
     */
    private T cacheObject;
    /**
     * 缓存key的时间，单位（秒）
     */
    private int expire;

    /**
     * 最后加载时间 单位（毫秒）
     */
    private long lastLoadTime;

    public CacheWrapper(T result, int expire) {
        this.cacheObject= result;
        this.expire=expire;
        this.lastLoadTime= System.currentTimeMillis();
    }
}
