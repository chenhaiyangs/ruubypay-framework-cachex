package com.ruubypay.cachex;

/**
 * 往redis中存储缓存内容的抽象层
 * @author chenhaiyang
 */
public interface ISerializer<T>{

    /**
     * 把一个对象序列化成byte数组
     * @param obj 给定对象
     * @return  返回byte数组
     * @throws Exception 异常
     */
    byte[] serialize(final T obj) throws Exception;

    /**
     * 解析一个二进制数组为对象
     * @param bytes byte数组
     * @param clazz AOP的返回值类型
     * @return 对象实例
     * @throws Exception 异常
     */
    T deserialize(final byte[] bytes, final Class clazz) throws Exception;

}
