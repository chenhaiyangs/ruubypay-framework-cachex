package com.ruubypay.cachex.to;

import com.ruubypay.cachex.exception.InvalidConfigException;
import lombok.Data;

import java.util.Objects;

/**
 * 当前的调用时缓存配置
 * @author chenhaiyang
 */
@Data
public class CacheConfigCurrent {

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

    public CacheConfigCurrent(String key,int expire,boolean useCache) {
        this.key = key;
        this.useCache = useCache;
        this.expire = expire;
    }


    public CacheConfigCurrent(String prefix, String config) {

        try{
            this.key=prefix;

            Objects.requireNonNull(config);

            String[] configs=config.split(",");
            if(configs.length==1){
                this.useCache=Boolean.parseBoolean(configs[0]);
                this.expire=0;
            }else{
                this.useCache=Boolean.parseBoolean(configs[0]);
                this.expire=Integer.parseInt(configs[1]);
            }
        }catch (Exception e){
            throw new InvalidConfigException(String.format("invalid Config prefix:[%s],config:[%s]",prefix,config));
        }


    }
}
