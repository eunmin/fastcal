package com.fastcal.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  @Primary
  public CacheManager caffeineCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .recordStats());
    return cacheManager;
  }

  @Bean
  public CacheManager calendarCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("calendars", "events", "principals");
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats());
    return cacheManager;
  }
}
