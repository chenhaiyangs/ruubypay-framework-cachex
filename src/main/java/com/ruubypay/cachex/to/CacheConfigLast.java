package com.ruubypay.cachex.to;

import lombok.Data;

/**
 * ConfigCacheStorage缓存上一次的配置
 * @author chenhaiyang
 */
@Data
public class CacheConfigLast {
    /**
     * 缓存key,存储一个缓存key表达式的前缀
     */
    private String key;
    /**
     * 是否启用缓存
     */
    private boolean useCache;
    /**
     * 超时时间
     */
    private int expire;

    public CacheConfigLast(CacheConfigCurrent cacheConfigCurrent) {
        this.key=cacheConfigCurrent.getKey();
        this.useCache=cacheConfigCurrent.isUseCache();
        this.expire=cacheConfigCurrent.getExpire();
    }
}
