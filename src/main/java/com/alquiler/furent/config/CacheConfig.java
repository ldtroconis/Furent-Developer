package com.alquiler.furent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuración de cache híbrida (Redis + Memoria).
 * Si Redis no está disponible, la aplicación arranca usando memoria local.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "furent.features.cache-enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public CacheManager cacheManager(@Autowired(required = false) RedisConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            log.warn(">>> REDIS NO DETECTADO: Usando caché en memoria (ConcurrentMapCacheManager). La app arrancará de todos modos.");
            return new ConcurrentMapCacheManager(
                "products", "categories", "product-detail", "featured-products", 
                "tenant-config", "user-profile", "product-count", "notifications", 
                "reviews", "coupons", "active-combos", "combos", "trending-products", "available-slots"
            );
        }

        log.info(">>> REDIS DETECTADO: Configurando RedisCacheManager con TTLs personalizados.");
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("categories", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("product-detail", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("featured-products", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("tenant-config", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("user-profile", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("product-count", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("notifications", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("reviews", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("coupons", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("combos", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
