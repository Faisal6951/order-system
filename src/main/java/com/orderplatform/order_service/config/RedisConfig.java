package com.orderplatform.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

        // @Bean("stringRedisTemplate")
        // public RedisTemplate<String, String>
        // stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        // RedisTemplate<String, String> template = new RedisTemplate<>();
        // template.setConnectionFactory(connectionFactory);
        // template.setKeySerializer(new StringRedisSerializer());
        // template.setValueSerializer(new StringRedisSerializer());
        // return template;
        // }

        @Bean
        @Primary
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                RedisCacheConfiguration config = RedisCacheConfiguration
                                .defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                RedisSerializer.json()));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(config)
                                .build();
        }
}