package org.clever.data.redis.core;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.clever.data.redis.serializer.RedisSerializer;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 22:43 <br/>
 */
@Slf4j
public class RedisTemplateTest {

    public void t01() {
//        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
//        serializer.setObjectMapper(objectMapper);
        // 创建 LettuceConnectionFactory
        LettuceConnectionFactory redisConnectionFactory = new LettuceConnectionFactory();
        // 创建 RedisTemplate
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置序列化规则
        redisTemplate.setStringSerializer(RedisSerializer.string());
        redisTemplate.setDefaultSerializer(RedisSerializer.string());
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setEnableDefaultSerializer(true);
        // template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        redisTemplate.setValueSerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
//        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
    }
}
