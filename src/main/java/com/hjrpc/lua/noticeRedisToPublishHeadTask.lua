-- 通知redis发布一个普通头任务
local expiredValues = redis.call('zrange', KEYS[1], 0, -1);
if #expiredValues > 0 then
    for _, v in expiredValues(tab) do
        local taskId, taskType, taskDataJson, executeTime, retryTimes, status = struct.unpack('Lc0Lc0Lc0li4i4', v);
        local taskIdInHash = redis.call('hget', KEYS[2], taskDataJson);
        if taskIdInHash ~= nil then
            if (status == 2) then
                return ;
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