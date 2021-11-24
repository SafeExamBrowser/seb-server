/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.jcache.config.JCacheConfigurerSupport;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@EnableCaching
@WebServiceProfile
@Configuration
public class CacheConfig extends JCacheConfigurerSupport {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${spring.cache.jcache.config}")
    String jCacheConfig;

    @Override
    @Bean
    public CacheManager cacheManager() {
        try {
            final CachingProvider cachingProvider = Caching.getCachingProvider();
            final javax.cache.CacheManager cacheManager =
                    cachingProvider.getCacheManager(new URI(this.jCacheConfig), this.getClass().getClassLoader());

            final CompositeCacheManager composite = new CompositeCacheManager();
            composite.setCacheManagers(Arrays.asList(
                    new JCacheCacheManager(cacheManager),
                    new ConcurrentMapCacheManager()));
            composite.setFallbackToNoOpCache(true);

            return composite;

        } catch (final URISyntaxException e) {
            log.error("Failed to initialize caching with EHCache. Fallback to simple caching");
            return new ConcurrentMapCacheManager();
        }
    }
}
