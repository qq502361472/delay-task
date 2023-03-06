package com.hjrpc.config;

import com.hjrpc.DistributedDelayedListener;
import com.hjrpc.core.DelayedCore;
import com.hjrpc.core.DistributedDelayedQueueCore;
import com.hjrpc.listener.RedisMessageSubscriber;
import com.hjrpc.listener.RedisMessageSubscriberImpl;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

import static com.hjrpc.constants.DelayQueueConstants.DELAY_MESSAGE_CHANNEL;
import static com.hjrpc.constants.DelayQueueConstants.SUBSCRIBER_METHOD_NAME;

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class MyRedisMQConfig {

    private int corePoolSize = 10;
    private int maxPoolSize = 20;
    private int queueCapacity = 900000;

    @Bean
    public Executor redisMqAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("redisMqThread-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        return executor;
    }

    @Bean
    public DelayedCore delayedCore(List<DistributedDelayedListener<?>> distributedDelayedListenerList
            , StringRedisTemplate stringRedisTemplate) {
        return new DistributedDelayedQueueCore(distributedDelayedListenerList, stringRedisTemplate);
    }

    @Bean
    public RedisMessageSubscriber redisMessageSubscriber(DelayedCore delayedCore) {
        return new RedisMessageSubscriberImpl(delayedCore);
    }

    /**
     * 消息监听器，使用MessageAdapter可实现自动化解码及方法代理
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(RedisMessageSubscriber redisMessageSubscriber) {
        return new MessageListenerAdapter(redisMessageSubscriber, SUBSCRIBER_METHOD_NAME);
    }

    /**
     * redis消息监听器容器
     * 可以添加多个监听不同话题的redis监听器，只需要把消息监听器和相应的消息订阅处理器绑定，该消息监听器
     * 通过反射技术调用消息订阅处理器的相关方法进行一些业务处理
     */

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory
            , Executor redisMqAsyncExecutor, MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(redisMqAsyncExecutor);
        container.addMessageListener(listenerAdapter, new PatternTopic(DELAY_MESSAGE_CHANNEL));
        return container;
    }
}