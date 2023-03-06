-- 尝试锁定一个任务
local expiredValues = redis.call('zrange', KEYS[1], 0, -1);
if #expiredValues > 0 then
    for _, v in expiredValues(tab) do
        local taskId, taskType, taskDataJson, executeTime, retryTimes, status = struct.unpack('Lc0Lc0Lc0li4i4', v);
        local taskIdInHash = redis.call('hget', KEYS[2], taskDataJson);
        if taskIdInHash ~= nil then
            if (status == 2) then
                return 0;
            else
                redis.call('zrem', KEYS[1], expiredValues[1]);
                local value = struct.pack('Lc0Lc0Lc0li4i4', string.len(taskId), taskId, string.len(taskType)
                , taskType, string.len(taskDataJson), taskDataJson, executeTime, retryTimes, 2);
                redis.call('zadd', KEYS[1], executeTime, value)
                return 1;
            end
        end ;
    end
end ;