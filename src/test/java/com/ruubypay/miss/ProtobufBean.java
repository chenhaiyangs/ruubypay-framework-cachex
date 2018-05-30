package com.ruubypay.miss;

import lombok.Data;

@Data
public class ProtobufBean<T> {

    private T t;

    private String id;

    private String name;

    private String passWord;

}
