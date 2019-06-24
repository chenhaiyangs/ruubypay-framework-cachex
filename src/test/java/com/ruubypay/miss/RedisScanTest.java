package com.ruubypay.miss;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

public class RedisScanTest {

    /**
     * jedis scan操作，游标的起始位置
     */
    private static final  String CURSOR_START="0";
    private Jedis jedis = new Jedis("127.0.0.1",6379);

    @Before
    public void before(){
        for(int i=0;i<100000;i++){
            jedis.set(String.format("key%d",i),""+i);
        }
        System.out.println("bofore===");
    }

    @Test
    public void test(){
        deleteKeysByMatchKey(jedis);
    }

    /**
     * 根据匹配键 删除缓存key
     * @param jedis jedis
     */
    private void deleteKeysByMatchKey(Jedis jedis){
        try{
            ScanResult<String> result;
            String rateLau =CURSOR_START;
            do{
                result= jedis.scan(rateLau,new ScanParams().count(1000).match("key*"));
                if(!result.getResult().isEmpty()){
                    Set<String> keys = new HashSet<>();
                    keys.addAll(result.getResult());
                    System.out.println(keys.size());
                    jedis.del(keys.toArray(new String[]{}));
                }
                rateLau = result.getStringCursor();
                System.out.println("这次返回的游标："+rateLau);
                if (CURSOR_START.equals(rateLau)) {
                    break;
                }
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                   e.printStackTrace();
                }
            }while(true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
