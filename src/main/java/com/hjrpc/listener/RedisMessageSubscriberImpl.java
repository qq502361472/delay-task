package com.hjrpc.listener;

import com.hjrpc.entity.DelayTask;
import com.hjrpc.core.DelayedCore;
import io.netty.util.HashedWheelTimer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.hjrpc.constants.DelayQueueConstants.RE_NOTICE_DELAY_SECONDS;

@Slf4j
@RequiredArgsConstructor
public class RedisMessageSubscriberImpl implements RedisMessageSubscriber {

    private final DelayedCore delayedCore;
    private static final Random random = new Random();

    @Override
    public void onDelayTaskMessage(DelayTask delayTask) {
        long delayTime = delayTask.getExecuteTime() - System.currentTimeMillis();
        // 清除本地的延时任务
        delayedCore.clearLocalDelayTask(delayTask.getTaskType());
        if (delayTime <= 10) {
            tryToProcessTask(delayTask);
        } else {
            delayedCore.pushToLocalDelayTask(delayTask, timeout -> tryToProcessTask(delayTask), delayTime
                    , TimeUnit.MICROSECONDS);
        }
    }


    private void tryToProcessTask(DelayTask delayTask) {
        if (delayedCore.tryToLockTask(delayTask)) {
            // 更新状态为执行中
            delayTask.setStatus(2);
            // 尝试执行任务
            try {
                delayedCore.submitTask(delayTask, delayedCore.executeTask(delayTask));
            } catch (Throwable e) {
                log.error("delayedCore.submitTask:", e);
                delayedCore.submitTask(delayTask, false);
            }
        } else {
            // 兼容服务挂掉的情况,10到20秒后还没有取到最新任务就手动通知redis重发任务，这里避免所有服务都同时推送此消息使用随机时间
            new HashedWheelTimer().newTimeout(timeout -> {
                String taskType = delayTask.getTaskType();
                if (!delayedCore.existsTask(taskType)) {
                    log.info("未监听到[{}]任务对应的延时任务，通知mq重新推送头任务", taskType);
                    delayedCore.noticeRedisToResetAndPublishHeadTask(taskType);
                }
            }, random.nextInt(10) + RE_NOTICE_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }
}
