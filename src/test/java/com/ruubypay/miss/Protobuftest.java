package com.ruubypay.miss;

import com.ruubypay.cachex.seriallizer.ProtobufSerializer;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Protobuftest {


    @Test
    public void testProtobuf() throws Exception {

        ProtobufBean<Integer> bean = new ProtobufBean<>();
        bean.setId("2");
        bean.setName("root");
        bean.setPassWord("rootForPassword");
        bean.setT(1);


        ProtobufSerializer serializer = new ProtobufSerializer();

        byte[] seriallis = serializer.serialize(bean);
        System.out.println("加密后的结果:"+seriallis);

        Object deserializes = serializer.deserialize(seriallis, ProtobufBean.class);
        System.out.println("解密后的结果:"+deserializes);

    }
}
