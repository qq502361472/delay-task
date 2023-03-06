package com.hjrpc.constants;

public interface DelayQueueConstants {
    /**
     * 延时任务初始化标记前缀
     */
    String INIT_FLAG_PREFIX = "delay:initialized:";
    String CONCAT_STRING = ":";
    String DELAY_QUEUE_KEY_PREFIX = "delay:task:";
    /**
     * 延时任务CHANNEL名
     */
    String DELAY_MESSAGE_CHANNEL = "delayMessageChannel";

    String SUBSCRIBER_METHOD_NAME = "onMessage";

    /**
     * 重新通知redis发送头消息延时时间，延迟实际时间为 RE_NOTICE_DELAY_SECONDS + 0到10之间的随机值
     */
    Integer RE_NOTICE_DELAY_SECONDS = 10;

    String NOTICE_REDIS_TO_PUBLISH_HEAD_TASK_SCRIPT = "local expiredValues = redis.call('zrange', KEYS[1], 0, -1);n" +
            "if #expiredValues > 0 then" +
            "    for _, v in expiredValues(tab) do" +
            "        local taskId, taskType, taskDataJson, executeTime, retryTimes, status = struct.unpack('Lc0Lc0Lc0li4i4', v);" +
            "        local taskIdInHash = redis.call('hget', KEYS[2], taskDataJson);" +
            "        if taskIdInHash ~= nil then" +
            "            if (status == 2) then" +
            "                return ;" +
            "            else" +
            "                local delayTask = '{\"taskId\":\"' .. taskId .. '\",\"taskType\":\"' .. taskType .." +
            "                        '\",\"taskDataJson\":\"' .. taskDataJson .. '\",\"executeTime\":'" +
            "                        .. executeTime .. ',\"retryTimes\":' .. retryTimes .. ',\"status\":' .. status .. '}';" +
            "                redis.call('publish " + DELAY_MESSAGE_CHANNEL + " ' .. delayTask);" +
            "                return ;" +
            "            end" +
            "        end ;" +
            "    end" +
            "end ;";

    String NOTICE_REDIS_TO_RESET_AND_PUBLISH_HEAD_TASK_SCRIPT = "local expiredValues = redis.call('zrange', KEYS[1], 0, -1);" +
            "if #expiredValues > 0 then" +
            "    for _, v in expiredValues(tab) do" +
            "        local taskId, taskType, taskDataJson, executeTime, retryTimes, status = struct.unpack('Lc0Lc0Lc0li4i4', v);" +
            "        local taskIdInHash = redis.call('hget', KEYS[2], taskDataJson);" +
            "        if taskIdInHash ~= nil then" +
            "            if status == 2 then" +
            "                redis.call('zrem', KEYS[1], v);" +
            "                if retryTimes + 1 >= 5 then" +
            "                    local delayTask = '{\"taskId\":\"' .. taskId .. '\",\"taskType\":\"' .. taskType .." +
            "                            '\",\"taskDataJson\":\"' .. taskDataJson .. '\",\"executeTime\":'" +
            "                            .. executeTime .. ',\"retryTimes\":' .. retryTimes + 1 .. ',\"status\":' .. 1 .. '}';" +
            "                    redis.call('lpush', 'errorList', delayTask);" +
            "                    break ;" +
            "                else" +
            "                    local value = struct.pack('Lc0Lc0Lc0li4i4', string.len(taskId), taskId, string.len(taskType)" +
            "                    , taskType, string.len(taskDataJson), taskDataJson, executeTime, retryTimes + 1, 1);" +
            "                    redis.call('zadd', KEYS[1], executeTime, value)" +
            "                    local delayTask = '{\"taskId\":\"' .. taskId .. '\",\"taskType\":\"' .. taskType .." +
            "                            '\",\"taskDataJson\":\"' .. taskDataJson .. '\",\"executeTime\":'" +
            "                            .. executeTime .. ',\"retryTimes\":' .. retryTimes + 1 .. ',\"status\":' .. 1 .. '}';" +
            "                    redis.call('publish " + DELAY_MESSAGE_CHANNEL + " ' .. delayTask);" +
            "                    return ;" +
            "                end" +
            "            else" +
            "                local delayTask = '{\"taskId\":\"' .. taskId .. '\",\"taskType\":\"' .. taskType .." +
            "                        '\",\"taskDataJson\":\"' .. taskDataJson .. '\",\"executeTime\":'" +
            "                        .. executeTime .. ',\"retryTimes\":' .. retryTimes .. ',\"status\":' .. status .. '}';" +
            "                redis.call('publish " + DELAY_MESSAGE_CHANNEL + " ' .. delayTask);" +
            "                return ;" +
            "            end" +
            "        end ;" +
            "    end" +
            "end ;";
}
