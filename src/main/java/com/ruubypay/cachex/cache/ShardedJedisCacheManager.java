package com.ruubypay.cachex.cache;

import com.ruubypay.cachex.ICacheManager;
import com.ruubypay.cachex.ISerializer;
import com.ruubypay.cachex.seriallizer.StringSerializer;
import com.ruubypay.cachex.to.CacheWrapper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * Redis缓存管理
 * @author chenhaiyang
 */
@Slf4j
public class ShardedJedisCacheManager implements ICacheManager {

    /**
     * 分布式锁的前缀
     */
    private static final String LOCK_PREFIX="lock-";

    /**
     * 获取分布式锁的key
     * @param matchKey 匹配key
     * @return 分布式锁的key
     */
    private static String getLockKey(String matchKey){
        return LOCK_PREFIX.concat(matchKey);
    }

    /**
     * 获取分布式锁成功
     */
    private static final String LOCK_SUCCESS="OK";
    /**
     * 只有当key不存在的时候，才set
     */
    private static final String SET_IF_NOT_EXISTS="NX";
    /**
     * 给key设置超时时间
     */
    private static final String SET_WITH_EXPIRE_TIME="PX";
    /**
     * 删除分布式锁的lua脚本
     */
    private static final String DELET_LOCK_LUA="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    /**
     * jedis scan操作，游标的起始位置
     */
    private static final  String CURSOR_START="0";
    /**
     * 用于处理删除缓存的任务的线程池
     */
    private ThreadPoolExecutor deleteCacheThreadPool =
            new ThreadPoolExecutor(5, 10, 0L,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                    (ThreadFactory) Thread::new);

    /**
     * shardedjedis连接池
     */
    final private ShardedJedisPool shardedJedisPool;
    /**
     * 释放资源
     * @param shardedJedis 连接
     */
    private void returnResource(ShardedJedis shardedJedis) {
        if(shardedJedis!=null) {
            shardedJedis.close();
        }
    }
    /**
     * 序列化value的序列化方式
     */
    final private ISerializer<Object> valueSerializer;
    /**
     * 序列化key的序列化方式
     */
    final private ISerializer<String> keySerializer = new StringSerializer();

    public ShardedJedisCacheManager(ShardedJedisPool shardedJedisPool,ISerializer<Object> valueSerializer){
        this.shardedJedisPool= Objects.requireNonNull(shardedJedisPool);
        this.valueSerializer=Objects.requireNonNull(valueSerializer);
    }

    @Override
    public boolean setCache(String cacheKey, CacheWrapper<Object> result) {
        if(null==cacheKey||cacheKey.length()==0){
            return false;
        }

        ShardedJedis shardedJedis=null;
        try {
            int expire=result.getExpire();
            shardedJedis=shardedJedisPool.getResource();
            Jedis jedis=shardedJedis.getShard(cacheKey);
            if(expire == 0) {
                jedis.set(keySerializer.serialize(cacheKey), valueSerializer.serialize(result));
            } else if(expire > 0) {
                jedis.setex(keySerializer.serialize(cacheKey), expire, valueSerializer.serialize(result));
            }
            return true;
        } catch(Exception ex) {
            log.error("set cache error", ex);
            return false;
        } finally {
            returnResource(shardedJedis);
        }
    }

    /**
     * 获取一个可以删除的许可，在redis实现里可以用分布式锁，避免多个节点都在删除key。key只需要删除一次就可以了
     * @param matchKey 缓存模糊匹配的键。过期时间为毫秒
     * @param requestId 请求Id
     * @param maxLockTime 分布式锁的最大锁定时长
     * @return 返回是否成功获取许可
     */
    @Override
    public boolean getDeleteAuth(String matchKey,String requestId,int maxLockTime) {
        ShardedJedis shardedJedis=null;
        try{
            String lockKey=getLockKey(matchKey);
            shardedJedis=shardedJedisPool.getResource();

            Jedis jedis = shardedJedis.getShard(lockKey);
            String result= jedis.set(lockKey,requestId,SET_IF_NOT_EXISTS,SET_WITH_EXPIRE_TIME,maxLockTime);
            return LOCK_SUCCESS.equals(result);
        }catch (Exception e){
            log.error("set lockkey fail,matchKey:[{}],requestId:[{}]",matchKey,requestId,e);
            return false;
        }finally {
            returnResource(shardedJedis);
        }

    }

    @Override
    public boolean canUseCache(String matchKey,String cacheKey) {
        ShardedJedis shardedJedis=null;
        try{
            shardedJedis=shardedJedisPool.getResource();
            return !shardedJedis.exists(getLockKey(matchKey))&& !shardedJedis.exists(getLockKey(cacheKey));
        }catch (Exception e){
            log.error("exists lockkey fail,matchKey:[{}],cacheKey:[{}]",matchKey,cacheKey,e);
            return false;
        }finally {
            returnResource(shardedJedis);
        }
    }

    /**
     * 当业务进行更新或删除操作时，需要删除缓存，在删除缓存过程中，为了避免脏数据产生。锁1秒，1秒内读请求全部只走db
     * @param cacheKey 缓存key
     * @param value value
     * @return 返回设置key的锁是否成功
     */
    @Override
    public boolean setCacheLock(String cacheKey, String value) {
        if(null==cacheKey||cacheKey.length()==0){
            return false;
        }
        String lockKey=getLockKey(cacheKey);
        ShardedJedis shardedJedis=null;
        try {
            shardedJedis=shardedJedisPool.getResource();
            Jedis jedis=shardedJedis.getShard(lockKey);
            jedis.set(lockKey,value,SET_IF_NOT_EXISTS,SET_WITH_EXPIRE_TIME,1000);
            return true;
        } catch(Exception ex) {
            log.error("set lockKey error", ex);
            return false;
        } finally {
            returnResource(shardedJedis);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CacheWrapper<Object> get(String cacheKey, Method method) {
        if(null==cacheKey||cacheKey.length()==0){
            return null;
        }
        CacheWrapper<Object> res=null;
        ShardedJedis shardedJedis=null;
        try {
            shardedJedis=shardedJedisPool.getResource();
            Jedis jedis=shardedJedis.getShard(cacheKey);
            byte[] bytes=jedis.get(keySerializer.serialize(cacheKey));
            if(bytes==null){
               return null;
            }
            res= (CacheWrapper<Object>) valueSerializer.deserialize(bytes,CacheWrapper.class);
        } catch(Exception ex) {
            log.error("get cache error", ex);
        } finally {
            returnResource(shardedJedis);
        }
        return res;
    }
    @Override
    public boolean delete(String cacheKey) {

        if(null==cacheKey||cacheKey.length()== 0) {
             return false;
        }
        log.info("delete cache:{}",cacheKey);
        ShardedJedis shardedJedis=null;
        try {
            shardedJedis=shardedJedisPool.getResource();
            Jedis jedis=shardedJedis.getShard(cacheKey);
            jedis.del(keySerializer.serialize(cacheKey));
            return true;
        } catch (Exception e) {
            log.error("delete cache error",e);
            return false;
        }finally {
            returnResource(shardedJedis);
        }
    }

    @Override
    public void batchDelete(String matchKey,String requestId) {
        if(null==matchKey||matchKey.length()== 0) {
            return;
        }
        log.info("batch delete cache matchkey:[{}],requestId:[{}]",matchKey,requestId);
        deleteCacheThreadPool.submit(new DeteleCacheRunnable(matchKey,requestId));

    }


    /**
     * 删除缓存的线程类
     * @author chenhaiyang
     */
    class DeteleCacheRunnable implements Runnable{
        /**
         * key类型模糊匹配键
         */
        final private String matchKey;
        /**
         * 请求Id
         */
        final private String requestId;

        DeteleCacheRunnable(String matchKey, String requestId) {
            this.matchKey=matchKey;
            this.requestId=requestId;
        }

        /**
         * 分批次删除缓存键。
         * 删除缓存成功后，将锁清除
         */
        @Override
        public void run(){

            ShardedJedis shardedJedis=shardedJedisPool.getResource();
            Collection<Jedis> jedisCollection= shardedJedis.getAllShards();
            jedisCollection.forEach(this::deleteKeysByMatchKey);
            try{
                returnResource(shardedJedis);
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
            //缓存清理干净后直接删除分布式锁,如果删除锁不成功，则会在2.5分钟后重试。
            while(!deleteLock()){
                try {
                    Thread.sleep(150000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(),e);
                }
                deleteLock();
            }
        }

        /**
         * 根据匹配键 删除缓存key
         * @param jedis jedis
         */
        private void deleteKeysByMatchKey(Jedis jedis){
            try{
                ScanResult<String> result;
                do{
                    result= jedis.scan(CURSOR_START,new ScanParams().count(1000).match(matchKey+"*"));
                    if(!result.getResult().isEmpty()){
                        Set<String> keys = new HashSet<>();
                        keys.addAll(result.getResult());
                        jedis.del(keys.toArray(new String[]{}));
                    }
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(),e);
                    }
                }while(!result.getResult().isEmpty());
            }catch (Exception e){
                log.error("jedisClient:[{}] delete cache error matchKey:[{}]",jedis,matchKey,e);
            }
        }

        /**
         * 清除分布式锁
         * @return 返回其是否清除成功。不报异常则为成功
         */
        private boolean deleteLock() {
            ShardedJedis shardedJedis=null;
            try{
                String lockKey=getLockKey(matchKey);
                shardedJedis=shardedJedisPool.getResource();
                Jedis jedis =shardedJedis.getShard(lockKey);
                Object result = jedis.eval(DELET_LOCK_LUA, Collections.singletonList(lockKey), Collections.singletonList(requestId));
                log.info("delete cacheLockKey matchKey:[{}],result:[{}]",matchKey,result);
                return true;
            }catch (Exception e){
                log.error("delete cacheLockKey err lockKey:[{}]",matchKey,e);
                return false;
            }finally {
                returnResource(shardedJedis);
            }
        }
    }
}
