package com.officeflow.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 全局配置
 * <p>
 * 提供以下能力：
 * 1. RedisTemplate&lt;String, Object&gt; — Jackson JSON 序列化，替代默认 JDK 序列化
 * 2. CacheManager — Spring Cache 注解支持（@Cacheable / @CacheEvict 等）
 * <p>
 * 连接工厂（LettuceConnectionFactory）和连接池由 Spring Boot 自动配置，
 * 通过 application.yml 中的 spring.data.redis.* 属性控制。
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * RedisTemplate — 业务代码注入使用
     * <p>
     * Key 使用 String 序列化，Value 使用 Jackson JSON 序列化。
     * 支持存储任意 Java 对象，Redis 中以可读 JSON 格式存储。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 序列化 — 纯字符串
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 序列化 — Jackson JSON
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager — Spring Cache 注解使用
     * <p>
     * 默认 TTL 30 分钟，由各 @Cacheable 的 cacheNames 区分缓存空间。
     * 序列化方式与 RedisTemplate 一致（Jackson JSON）。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .transactionAware()
                .build();
    }

    /**
     * 创建 Jackson JSON 序列化器
     * <p>
     * 启用默认类型信息，使反序列化时能还原为原始 Java 类型。
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(om);
    }
}
