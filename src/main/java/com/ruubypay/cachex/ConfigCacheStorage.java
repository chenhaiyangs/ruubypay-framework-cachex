package com.ruubypay.cachex;

import com.ruubypay.cachex.to.CacheConfigLast;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注解配置本地缓存
 * @author chenhaiyang
 */
class ConfigCacheStorage {

    private static Map<String,CacheConfigLast> configCaches = new ConcurrentHashMap<>();

    /**
     * 获取上一次的注解配置
     * @param key key
     * @return 返回配置
     */
    static CacheConfigLast getConfigByKey(String key){
        return configCaches.get(key);
    }

    /**
     * 设置注解配置
     * @param key key
     * @param cacheConfig 缓存配置
     */
    static void putConfigByKey(String key,CacheConfigLast cacheConfig){
        configCaches.put(key,cacheConfig);
    }
}
