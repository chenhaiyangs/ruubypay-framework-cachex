package com.ruubypay.cachex;

import com.ruubypay.cachex.annotation.CacheDelete;
import com.ruubypay.cachex.annotation.CacheDeleteKey;
import com.ruubypay.cachex.annotation.CacheEnabled;
import com.ruubypay.cachex.aop.proxy.aspectj.AspectjCacheProxyChain;
import com.ruubypay.cachex.aop.proxy.aspectj.AspectjDeleteCacheAopProxyChain;
import com.ruubypay.cachex.exception.DeleteCacheFailException;
import com.ruubypay.cachex.script.SpelScriptParser;
import com.ruubypay.cachex.to.CacheConfigCurrent;
import com.ruubypay.cachex.to.CacheConfigLast;
import com.ruubypay.cachex.to.CacheWrapper;
import com.ruubypay.cachex.util.CacheConfigStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * 缓存逻辑的核心实现类
 * @author chenhaiyang
 */
@Slf4j
public class CacheHandler {
    /**
     * 缓存配置
     */
    private CacheConfig cacheConfig;
    /**
     * 缓存管理器
     */
    private ICacheManager cacheManager;
    /**
     * 脚本解析器
     */
    private AbstractScriptParser scriptParser;


    public CacheHandler(CacheConfig cacheConfig, ICacheManager cacheManager,AbstractScriptParser scriptParser) {
        this.cacheConfig = Objects.requireNonNull(cacheConfig);
        this.cacheManager = cacheManager;
        this.scriptParser = scriptParser;
    }
    public CacheHandler(CacheConfig cacheConfig, ICacheManager cacheManager) {
        this(cacheConfig, cacheManager,new SpelScriptParser());
    }

    /**
     * 处理缓存逻辑
     * @param proxyChain 切面
     * @param cacheEnabled  缓存配置注解
     * @return 返回AOP的执行结果
     */
    public Object proceed(AspectjCacheProxyChain proxyChain, CacheEnabled cacheEnabled) throws Throwable {

        final String prefix = scriptParser.getPrefix(cacheEnabled.key());
        final CacheConfigCurrent cacheConfigCurrent = getCacheCurrent(prefix,cacheEnabled);
        final CacheConfigLast cacheConfigLast =ConfigCacheStorage.getConfigByKey(prefix);
        //没有namespace的cacheKey,通过标签解析成为真实的key
        final String cacheKey = scriptParser.getExpressValue(cacheEnabled.key(),proxyChain.getTarget(),proxyChain.getArgs());
        //带namespace的cacheKey
        final String nameSpaceCacheKey= cacheConfig.getNamespace().concat("-").concat(cacheKey);
        //存储每个key类型的模糊匹配键,用于删除一批key
        final String matchedKey=cacheConfig.getNamespace().concat("-").concat(scriptParser.buildMatchedPrefix(cacheEnabled.key()));

        //如果当前的状态是使用缓存，但上一次的配置为不使用缓存，则本次先清空缓存，直接放行方法，不缓存结果
        if(CacheConfigStatus.needClearCache(cacheConfigLast,cacheConfigCurrent)){

            //生成一个请求Id
            final String requestId = UUID.randomUUID().toString();
            //只有获取一个删除缓存的许可，才允许进行删除缓存的操作。
            if(cacheManager.getDeleteAuth(matchedKey,requestId,cacheConfig.getBatchLockTime())){

                cacheManager.batchDelete(matchedKey,requestId);
                Object result= proxyChain.doProxyChain(proxyChain.getArgs());
                ConfigCacheStorage.putConfigByKey(prefix,new CacheConfigLast(cacheConfigCurrent));

                log.info("observe useCache become from false to true,now,clear cahce ,cache pattern :[{}]",matchedKey);
                return result;
            }
        }
        //如果配置允许使用缓存，并且缓存介质也允许使用缓存。则先查询缓存，没有查询到，则放行方法，再把方法返回值放到缓存
        if(CacheConfigStatus.isUseCache(cacheConfigCurrent)
                && cacheManager.canUseCache(matchedKey,nameSpaceCacheKey)) {

            CacheWrapper<Object> cacheWrapper = cacheManager.get(nameSpaceCacheKey,proxyChain.getMethod());
            if(cacheWrapper !=null&& cacheWrapper.getCacheObject()!=null){
                log.info("getCache success! args:[{}],key:[{}],data:[{}]",proxyChain.getArgs(),nameSpaceCacheKey, cacheWrapper.getCacheObject());
                ConfigCacheStorage.putConfigByKey(prefix,new CacheConfigLast(cacheConfigCurrent));
                return cacheWrapper.getCacheObject();
            }
            Object result = proxyChain.doProxyChain(proxyChain.getArgs());
            boolean success = cacheManager.setCache(nameSpaceCacheKey,new CacheWrapper<>(result,cacheConfigCurrent.getExpire()));
            log.info("put data [{}] into cache ,key:[{}],value:[{}],put result: [{}]",nameSpaceCacheKey,result,success);
            ConfigCacheStorage.putConfigByKey(prefix,new CacheConfigLast(cacheConfigCurrent));
            return result;
        }
        //不使用缓存，直接放行方法
        ConfigCacheStorage.putConfigByKey(prefix,new CacheConfigLast(cacheConfigCurrent));
        return proxyChain.doProxyChain(proxyChain.getArgs());
    }

    /**
     * 删除缓存的实现
     * 在业务操作完成后删除缓存
     * @param deleteProxyChain 删除缓存配置切面
     * @param cacheDelete 删除缓存的注解
     * @param isBeforeDelete 是否是在更新数据库前删除缓存
     */
    public void deleteCache(AspectjDeleteCacheAopProxyChain deleteProxyChain, CacheDelete cacheDelete, boolean isBeforeDelete) {
        final CacheDeleteKey[] cacheDeleteKeys= cacheDelete.value();
        Arrays.stream(cacheDeleteKeys).forEach(key->deleteCache(deleteProxyChain,key,isBeforeDelete));
    }

    /**
     * 在被拦截方法的前或者后，删除缓存
     * @param deleteProxyChain 切面
     * @param cacheDeleteKey 缓存key
     * @param isBeforeDelete 是否是在更新数据库前删除缓存
     */
    private void deleteCache(AspectjDeleteCacheAopProxyChain deleteProxyChain, CacheDeleteKey cacheDeleteKey,boolean isBeforeDelete) {
        //缓存key
        final String cacheKey = scriptParser.getExpressValue(cacheDeleteKey.key(),deleteProxyChain.getTarget(),deleteProxyChain.getArgs());
        //带namespace的cacheKey
        final String nameSpaceCacheKey= cacheConfig.getNamespace().concat("-").concat(cacheKey);
        //为了尽可能保持数据的一致性，删除缓存时先添加一个1s后过期的key，业务方在1s内的查询先直接查库
        boolean setLockSuccess = cacheManager.setCacheLock(nameSpaceCacheKey,UUID.randomUUID().toString());
        //删除缓存
        boolean deleteSuccess = cacheManager.delete(nameSpaceCacheKey);
        log.info("delete cache key :[{}],setLockResult:[{}],deleteResult:[{}]",nameSpaceCacheKey,setLockSuccess,deleteSuccess);
        if(isBeforeDelete && cacheConfig.isThrowExceptionWhenDeleteCacheFail() && !deleteSuccess){
            throw new DeleteCacheFailException(String.format("delete cache fail cachekey:[%s]",nameSpaceCacheKey));
        }

    }

    /**
     * 获取当前的缓存配置。
     * 如果是fromOuter，则从注入的map中获取，否则，注解使用注解上的配置
     * @param cacheEnabled cache注解
     * @param prefix 前缀（缓存的模版key）
     * @return 返回当前的缓存配置。
     */
    private CacheConfigCurrent getCacheCurrent(String prefix,CacheEnabled cacheEnabled) {
        if (cacheEnabled.configFromOuter()) {
            return new CacheConfigCurrent(prefix, cacheConfig.getConfigs().get(prefix));
        }
        return new CacheConfigCurrent(prefix, cacheEnabled.expire(), cacheEnabled.useCache());
    }
}
