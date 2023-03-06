package com.hjrpc.core;

import com.hjrpc.entity.DelayTask;
import com.hjrpc.DistributedDelayedListener;
import io.netty.util.TimerTask;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface DelayedCore {

    /**
     * 通知redis推送一个普通头任务，只取状态为0和1的数据（未锁定，发送失败的）
     *
     * @param listener 对应的监听器
     */
    void noticeRedisToPublishHeadTask(DistributedDelayedListener<?> listener);

    /**
     * 通知redis重新推送头任务，如果任务状态是推送中，会重置为推送失败，且失败次数加1
     *
     * @param taskType 任务类型
     */
    void noticeRedisToResetAndPublishHeadTask(String taskType);

    /**
     * 通知redis推送一个普通头任务，只取状态为0和1的数据（未锁定，发送失败的）
     *
     * @param taskType 任务类型
     */
    void noticeRedisToPublishHeadTask(String taskType);

    /**
     * 尝试锁定延时任务
     *
     * @param delayTask 延时任务
     */
    boolean tryToLockTask(DelayTask delayTask);

    /**
     * 执行延时任务，执行失败会回滚
     *
     * @param delayTask 延时任务
     */
    boolean executeTask(DelayTask delayTask);

    /**
     * 提交延时任务执行结果
     *
     * @param delayTask   延时任务
     * @param successFlag 执行是否成功标记
     */
    void submitTask(DelayTask delayTask, boolean successFlag);

    /**
     * 推送一个延时任务
     *
     * @param delayTask 延时任务
     */
    void offer(DelayTask delayTask);

    /**
     * 批量推送延时任务
     *
     * @param delayTasks 延时任务
     */
    void batchOffer(Collection<DelayTask> delayTasks);

    /**
     * 删除延时任务
     *
     * @param taskDataJson 延时任务数据
     * @param taskType     延时任务类型
     */
    void remove(String taskDataJson, String taskType);

    /**
     * 删除所有延时任务
     *
     * @param taskDataJsons 延时任务数据集合
     * @param taskType      延时任务类型
     */
    void removeAll(Collection<String> taskDataJsons, String taskType);

    /**
     * 推送到本地延时任务
     *
     * @param delayTask 需要延时的任务
     * @param task      延时时间到期后所执行的方法
     */
    void pushToLocalDelayTask(DelayTask delayTask, TimerTask task);

    /**
     * 推送任务到本地延时任务
     *
     * @param delayTask 需要延时的任务
     * @param task      延时时间到期后所执行的方法
     * @param delayTime 延时时间
     * @param unit      延时时间单位
     */
    void pushToLocalDelayTask(DelayTask delayTask, TimerTask task, long delayTime, TimeUnit unit);

    /**
     * 清空任务类型对应的延时任务
     *
     * @param taskType 延时任务类型
     */
    void clearLocalDelayTask(String taskType);

    /**
     * 判断当前延时任务类型是否存在延时任务
     *
     * @param taskType 延时任务类型
     * @return boolean（true:存在任务  false:不存在任务）
     */
    boolean existsTask(String taskType);

}
