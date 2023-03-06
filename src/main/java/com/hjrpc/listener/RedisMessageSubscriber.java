package com.hjrpc.listener;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hjrpc.entity.DelayTask;

public interface RedisMessageSubscriber {

    /**
     * 监听到新的延时任务。这里延时任务
     *
     * @param delayTaskJson 延时任务JSON
     */
    default void onMessage(String delayTaskJson) {
        onDelayTaskMessage(JSONObject.parseObject(delayTaskJson, new TypeReference<DelayTask>() {
        }));
    }

    void onDelayTaskMessage(DelayTask delayTask);
}
