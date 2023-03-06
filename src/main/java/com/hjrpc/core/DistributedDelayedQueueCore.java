package com.hjrpc.core;

import cn.hutool.core.convert.Convert;
import com.hjrpc.entity.DelayTask;
import com.hjrpc.DistributedDelayedListener;
import com.hjrpc.constants.DelayQueueConstants;
import io.netty.util.HashedWheelTimer;
import io.netty.util.TimerTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hjrpc.constants.DelayQueueConstants.*;

@Slf4j
@RequiredArgsConstructor
public class DistributedDelayedQueueCore implements DelayedCore, ApplicationListener<ApplicationStartedEvent> {
    private final List<DistributedDelayedListener<?>> distributedDelayedListenerList;
    private Map<String, DistributedDelayedListener<?>> distributedDelayedListenerMap;
    private final Map<String, HashedWheelTimer> localHeadTaskMap = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void noticeRedisToPublishHeadTask(DistributedDelayedListener<?> listener) {
        String taskType = getTaskType(listener);
        noticeRedisToPublishHeadTask(taskType);
    }

    @Override
    public void noticeRedisToPublishHeadTask(String taskType) {
        List<String> keys = getRedisZsetAndHashKeyList(taskType);
        stringRedisTemplate.execute(new DefaultRedisScript<>(NOTICE_REDIS_TO_PUBLISH_HEAD_TASK_SCRIPT, String.class)
                , RedisSerializer.string(), RedisSerializer.string(), keys);
    }

    private List<String> getRedisZsetAndHashKeyList(String taskType) {
        String zsetKey = getKey(taskType, "zset");
        String hashKey = getKey(taskType, "hash");
        return Stream.of(zsetKey, hashKey).collect(Collectors.toList());
    }

    private String getKey(String taskType, String concatString) {
        return "{" + DELAY_QUEUE_KEY_PREFIX + taskType + ":}" + concatString;
    }

    @Override
    public boolean tryToLockTask(DelayTask delayTask) {
        return false;
    }

    @Override
    public boolean executeTask(DelayTask delayTask) {
        DistributedDelayedListener<?> listener = distributedDelayedListenerMap.get(delayTask.getTaskType());
        Type type;
        try {
            type = getType(listener);
            listener.invoke(Convert.convert(type, delayTask.getTaskDataJson()));
            return true;
        } catch (Exception e) {
            log.error("类型转换出现异常:", e);
            return false;
        }
    }

    private <T> Type getType(DistributedDelayedListener<T> listener) throws Exception {
        Class<?> clazz = listener.getClass();
        if (AopUtils.isAopProxy(listener)) {
            clazz = Objects.requireNonNull(((Advised) listener).getTargetSource().getTarget()).getClass();
        }
        return ((ParameterizedType) clazz.getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }

    @Override
    public void submitTask(DelayTask delayTask, boolean successFlag) {

    }

    @Override
    public void offer(DelayTask delayTask) {

    }

    @Override
    public void batchOffer(Collection<DelayTask> delayTasks) {

    }

    @Override
    public void remove(String taskDataJson, String taskType) {
        removeAll(Collections.singleton(taskDataJson), taskType);
    }

    @Override
    public void removeAll(Collection<String> taskDataJsons, String taskType) {
        String hashKey = getKey(taskType, "hash");
        stringRedisTemplate.opsForHash().delete(hashKey, taskDataJsons.toArray());
    }

    @Override
    public void pushToLocalDelayTask(DelayTask delayTask, TimerTask task) {
        long delayTime = delayTask.getExecuteTime() - System.currentTimeMillis();
        pushToLocalDelayTask(delayTask, task, delayTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public void pushToLocalDelayTask(DelayTask delayTask, TimerTask task, long delayTime, TimeUnit unit) {
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
        hashedWheelTimer.newTimeout(task, delayTime, unit);
        localHeadTaskMap.put(delayTask.getTaskType(), hashedWheelTimer);
    }

    @Override
    public void clearLocalDelayTask(String taskType) {
        HashedWheelTimer hashedWheelTimer = localHeadTaskMap.get(taskType);
        if (hashedWheelTimer != null) {
            hashedWheelTimer.stop();
            localHeadTaskMap.remove(taskType);
        }
    }

    @Override
    public boolean existsTask(String taskType) {
        return localHeadTaskMap.containsKey(taskType);
    }

    @Override
    public void noticeRedisToResetAndPublishHeadTask(String taskType) {
        List<String> keys = getRedisZsetAndHashKeyList(taskType);
        stringRedisTemplate.execute(new DefaultRedisScript<>(NOTICE_REDIS_TO_RESET_AND_PUBLISH_HEAD_TASK_SCRIPT
                        , String.class), RedisSerializer.string(), RedisSerializer.string(), keys);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        init();
    }

    /**
     * 启动成功后初始化
     */
    private void init() {
        if (distributedDelayedListenerList != null && !distributedDelayedListenerList.isEmpty()) {
            // 初始化监听器map，方便后续读取
            initDistributedDelayedListenerMap();
            //初始化延时任务到redis
            distributedDelayedListenerList.forEach(listener -> {
                String taskType = getTaskType(listener);
                String initFlag = DelayQueueConstants.INIT_FLAG_PREFIX + taskType;
                if (Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(initFlag, "initialized"
                        , 5, TimeUnit.MINUTES))) {
                    // 调用listener的初始化方法
                    batchOffer(listener.init());
                }

                // 通知redis发布头任务
                noticeRedisToPublishHeadTask(listener);
            });
        }
    }

    /**
     * 初始化监听器map，方便后续读取
     */
    private void initDistributedDelayedListenerMap() {
        distributedDelayedListenerMap = distributedDelayedListenerList
                .stream()
                .collect(Collectors.toMap(this::getTaskType, Function.identity()));
    }

    /**
     * 获取监听器对应的类型，这里兼容动态代理的情况
     *
     * @param listener 监听器
     * @return 延时任务的类型
     */
    private String getTaskType(DistributedDelayedListener<?> listener) {
        String taskType = listener.getClass().getTypeName();
        int index = taskType.indexOf("$");
        if (index >= 0) {
            taskType = taskType.substring(0, index);
        }
        return taskType;
    }
}
