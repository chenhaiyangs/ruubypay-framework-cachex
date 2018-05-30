package com.ruubypay.cachex.seriallizer;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.ruubypay.cachex.ISerializer;

import java.util.Objects;

/**
 * Protobuf序列化
 * @author chenhaiyang
 */
public class ProtobufSerializer implements ISerializer<Object> {

    @Override
    public byte[] serialize(Object obj) throws Exception {
            return serializee(obj);
    }
    @SuppressWarnings("unchecked")
    private  <T> byte[] serializee(T t) throws Exception{
        return ProtobufIOUtil.toByteArray(t, (Schema<T>) RuntimeSchema.createFrom(t.getClass()),
                LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(byte[] bytes, Class returnType) throws Exception {
        Objects.requireNonNull(returnType);

        RuntimeSchema<Object> runtimeSchema = RuntimeSchema.createFrom(returnType);
        Object t = runtimeSchema.newMessage();
        ProtobufIOUtil.mergeFrom(bytes, t, runtimeSchema);
          return t;
    }
}
