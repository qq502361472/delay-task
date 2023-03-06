-- 通知redis重置任务状态并且发布一个新的头任务
local expiredValues = redis.call('zrange', KEYS[1], 0, -1);
if #expiredValues > 0 then
    for _, v in expiredValues(tab) do
        local taskId, taskType, taskDataJson, executeTime, retryTimes, status = struct.unpack('Lc0Lc0Lc0li4i4', v);
        local taskIdInHash = redis.call('hget', KEYS[2], taskDataJson);
        if taskIdInHash ~= nil then
            if status == 2 then
                redis.call('zrem', KEYS[1], v);
                if retryTimes + 1 >= 5 then
                    local delayTask = '{\"taskId\":\"' .. taskId .. '\",\"taskType\":\"' .. taskType ..
                            '\",\"taskDataJson\":\"' .. taskDataJson .. '\",\"executeTime\":'
                            .. executeTime .. ',\"retryTimes\":' .. retryTimes + 1 .. ',\"status\":' .. 1 .. '}';
                    redis.call('lpush', 'errorList', delayTask);
                    break ;
                else
                    local value = struct.pack('Lc0Lc0Lc0li4i4', string.len(taskId), taskId, string.len(taskType)
                    , taskType, string.len(taskDataJson), taskDataJson, executeTime, retryTimes + 1, 1);
                    redis.call('zadd', KEYS[1], executeTime, value)
                    local delayTask = '{\"taskId\":\"' .. taskId .. '\",\"taskType\":\"' .. taskType ..
                            '\",\"taskDataJson\":\"' .. taskDataJson .. '\",\"executeTime\":'
                            .. executeTime .. ',\"retryTimes\":' .. retryTimes + 1 .. ',\"status\":' .. 1 .. '}';
                    redis.call('publish delayMessage ' .. delayTask);
                    return ;
                end
            else
                local delayTask = '{\"taskId\":\"' .. taskId .. '\",\"taskType\":\"' .. taskType ..
                        '\",\"taskDataJson\":\"' .. taskDataJson .. '\",\"executeTime\":'
                        .. executeTime .. ',\"retryTimes\":' .. retryTimes .. ',\"status\":' .. status .. '}';
                redis.call('publish delayMessage ' .. delayTask);
                return ;
            end
        end ;
    end
end ;