package com.hjrpc;

import com.hjrpc.entity.DelayTask;

import java.util.ArrayList;
import java.util.List;

public interface DistributedDelayedListener<T> {

    default List<DelayTask> init(){
        return new ArrayList<>();
    }

    void invoke(T t);
}
