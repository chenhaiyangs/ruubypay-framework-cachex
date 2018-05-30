package com.ruubypay.cachex.seriallizer;

import com.ruubypay.cachex.ISerializer;

import java.nio.charset.Charset;

/**
 * String类型的元素的序列化
 * @author chenhaiyang
 */
public class StringSerializer implements ISerializer<String> {

    private final Charset charset;

    public StringSerializer() {
        this(Charset.forName("UTF8"));
    }

    public StringSerializer(Charset charset) {
        this.charset=charset;
    }

    @Override
    public String deserialize(byte[] bytes, Class returnType) throws Exception {
        return(bytes == null ? null : new String(bytes, charset));
    }

    @Override
    public byte[] serialize(String string) throws Exception {
        return(string == null ? null : string.getBytes(charset));
    }
}
