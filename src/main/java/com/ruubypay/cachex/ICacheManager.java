package com.ruubypay.cachex;

import com.ruubypay.cachex.to.CacheWrapper;

import java.lang.reflect.Method;

/**
 * 缓存的介质抽象层
 * @author chenhaiyang
 */
public interface ICacheManager {

    /**
     * 往缓存写数据
     * @param cacheKey 缓存Key
     * @param result 缓存包装数据
     * @return 是否设置缓存成功
     */
    boolean setCache(final String cacheKey, final CacheWrapper<Object> result);

    /**
     * 获取一个删除缓存的许可
     * @param likeKey 缓存模糊匹配的键
     * @param requestId 请求Id
     * @param maxLockTime 分布式锁最大锁定时常，单位ms
     * @return 返回能否执行删除缓存的逻辑
     */
    boolean getDeleteAuth(final String likeKey,final String requestId,int maxLockTime);

    /**
     * 是否可以使用缓存，当删除缓存许可存在的时候，不能使用缓存
     * 当缓存功能从 禁用变成启用时，为了避免脏数据，需要将对应key类型的缓存全部删除。
     * 删除缓存会获取一个删除缓存的许可(getDeleteAuth)
     * 当旧的缓存删除完毕的时候，缓存实现类需要自己删除这个许可。
     * 当缓存删除许可一直存在，则为了避免客户端脏数据的产生，禁用缓存。
     * @param matchKey 缓存类型key匹配键
     * @param cacheKey 缓存的具体键类型
     * @return 返回是否可以使用缓存
     */
    boolean canUseCache(final String matchKey,final String cacheKey);

    /**
     * 在更新数据期间，业务查询不应使用缓存，直接走db。因此每次删除缓存，需要设置一个锁，保证数据的完整性
     * @param cacheKey 缓存key
     * @param value value
     * @return 设置锁成功
     */
    boolean setCacheLock(final String cacheKey,final String value);

    /**
     * 根据缓存Key获得缓存中的数据
     * @param cacheKey 缓存key
     * @param method 拦截的方法
     * @return 缓存数据
     */
    CacheWrapper<Object> get(final String  cacheKey, Method method);

    /**
     * 删除缓存
     * @param cacheKey 缓存key
     * @return 是否设置缓存成功
     */
    boolean delete(final String cacheKey);

    /**
     * 批量删除缓存
     * @param requestId 请求Id
     * @param like 模糊键
     */
    void batchDelete(final String like,final String requestId);
}
