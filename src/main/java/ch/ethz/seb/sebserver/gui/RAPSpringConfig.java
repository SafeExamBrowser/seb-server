/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.eclipse.rap.rwt.engine.RWTServlet;
import org.eclipse.rap.rwt.engine.RWTServletContextListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Configuration
@GuiProfile
public class RAPSpringConfig {

    @Value("${sebserver.gui.entrypoint}")
    private String entrypoint;

    @Bean
    public ServletContextInitializer initializer() {
        return new ServletContextInitializer() {

            @Override
            public void onStartup(final ServletContext servletContext) throws ServletException {
                servletContext.setInitParameter(
                        "org.eclipse.rap.applicationConfiguration",
                        RAPConfiguration.class.getName());
            }
        };
    }

    @Bean
    public ServletListenerRegistrationBean<ServletContextListener> listenerRegistrationBean() {
        final ServletListenerRegistrationBean<ServletContextListener> bean =
                new ServletListenerRegistrationBean<>();
        bean.setListener(new RWTServletContextListener());
        return bean;
    }

    @Bean
    public ServletRegistrationBean<RWTServlet> servletRegistrationBean() {
        return new ServletRegistrationBean<>(new RWTServlet(), this.entrypoint + "/*");
    }

}
