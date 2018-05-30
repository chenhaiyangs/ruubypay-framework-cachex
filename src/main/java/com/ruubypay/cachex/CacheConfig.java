package com.ruubypay.cachex;

import lombok.Data;

import java.util.Map;
import java.util.Objects;

/**
 * 缓存的分布式锁
 * @author chenhaiyang
 */
@Data
public class CacheConfig {
    /**
     * 批量删除任务的分布式锁的最大作用时长，默认为1.5分钟
     */
    private static final int BATCH_LOCK_MAX_TIME=90000;

    /**
     * 缓存框架使用的命名空间。key的构造方式为：namespace-key
     */
    private String namespace="";
    /**
     * 用于批量删除缓存的分布式锁的超时时间。单位ms
     */
    private int batchLockTime=BATCH_LOCK_MAX_TIME;

    /**
     * 缓存配置容器存储每个key的缓存过期时间,配置是否使用等配置
     */
    private Map<String, String> configs;

    /**
     * 如果是在写请求前删除缓存，删除缓存失败可以抛出异常，阻止业务继续进行。保证数据完整性。也可以忽略失败。
     * 当删除缓存失败时，是否抛出业务异常。
     */
    private boolean throwExceptionWhenDeleteCacheFail;

    /**
     * 设置缓存的命名空间
     * @param namespace 命名空间
     */
    public void setNamespace(String namespace){
       this.namespace=Objects.requireNonNull(namespace);
    }

}
