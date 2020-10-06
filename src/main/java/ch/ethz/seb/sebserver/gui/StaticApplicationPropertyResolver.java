/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

public class StaticApplicationPropertyResolver implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(StaticApplicationPropertyResolver.class);

    private static ApplicationContext CONTEXT;

    public StaticApplicationPropertyResolver() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        CONTEXT = applicationContext;
    }

    public static String getProperty(final String name, final String defaultValue) {
        try {
            final Environment env = CONTEXT.getBean(Environment.class);
            return env.getProperty(name, defaultValue);
        } catch (final Exception e) {
            log.warn("Failed to get property: {} from static context. Return default value: {}", name, defaultValue);
            return defaultValue;
        }
    }

}
