/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@EnableAsync
//@Configuration
//@WebServiceProfile
@Deprecated
public class ControllerConfig implements WebMvcConfigurer {

//    @Override
//    public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
//        configurer.setTaskExecutor(threadPoolTaskExecutor());
//        configurer.setDefaultTimeout(30000);
//    }
//
//    public AsyncTaskExecutor threadPoolTaskExecutor() {
//        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(7);
//        executor.setMaxPoolSize(42);
//        executor.setQueueCapacity(11);
//        executor.setThreadNamePrefix("mvc-");
//        executor.initialize();
//        return executor;
//    }

}
