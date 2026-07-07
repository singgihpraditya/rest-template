package com.example.template.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATEGORIES_WITH_PRODUCT_COUNT_CACHE = "categoriesWithProductCount";
}
