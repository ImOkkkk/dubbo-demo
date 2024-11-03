package com.imokkkk.config;

import org.apache.dubbo.cache.CacheFactory;
import org.apache.dubbo.cache.support.lru.LruCacheFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wyliu
 * @date 2024/10/27 16:18
 * @since 1.0
 */
@Configuration
public class DubboConfig {

    @Bean
    public CacheFactory cacheFactory() {
        return new LruCacheFactory();
    }
}
