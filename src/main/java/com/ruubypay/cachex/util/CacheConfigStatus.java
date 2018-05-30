package com.ruubypay.cachex.util;

import com.ruubypay.cachex.to.CacheConfigCurrent;
import com.ruubypay.cachex.to.CacheConfigLast;

import java.io.UnsupportedEncodingException;

/**
 * cacheconfigStatus 缓存配置状态机
 * @author chenhaiyang
 */
public class CacheConfigStatus {

    /**
     * 是否需要清空缓存
     * 如果当前的状态是使用缓存，但上一次的配置为不使用缓存，则本次先清空缓存，直接放行方法，不缓存结果
     * 如果过期时间也重新配置了，也会删除原先的缓存。
     * 如果缓存的
     * @param cacheConfigLast 上一次的缓存
     * @param cacheConfigCurrent 当前的缓存
     * @return 返回是否需要清空缓存
     */
    public static boolean needClearCache(CacheConfigLast cacheConfigLast, CacheConfigCurrent cacheConfigCurrent) throws UnsupportedEncodingException{

        return (cacheConfigCurrent.isUseCache() && cacheConfigLast!=null && !cacheConfigLast.isUseCache())
                ||(cacheConfigLast!=null && cacheConfigCurrent.getExpire()!=cacheConfigLast.getExpire());
    }

    /**
     * 是否使用缓存
     * @param cacheConfigCurrent 当前的缓存配置
     * @return 返回是否开启缓存功能
     */
    public static boolean isUseCache(CacheConfigCurrent cacheConfigCurrent){
        return cacheConfigCurrent.isUseCache();
    }
}
