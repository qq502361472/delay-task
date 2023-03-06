package com.hjrpc.entity;

import lombok.Data;

@Data
public class DelayTask {
    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务类型（取值Listener的名称）
     */
    private String taskType;

    /**
     * 任务数据对象Json
     */
    private String taskDataJson;

    /**
     * 执行时间点的时间戳
     */
    private long executeTime;

    /**
     * 重试的次数
     */
    private int retryTimes;

    /**
     * 任务状态(0:未执行，1：执行失败，2-执行中，3：执行成功)
     */
    private int status;
}
