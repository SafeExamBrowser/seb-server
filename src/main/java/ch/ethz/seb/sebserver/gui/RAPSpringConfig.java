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

import org.eclipse.rap.rwt.engine.RWTServlet;
import org.eclipse.rap.rwt.engine.RWTServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Configuration
@GuiProfile
public class RAPSpringConfig {

    private static final Logger log = LoggerFactory.getLogger(RAPSpringConfig.class);

    @Value("${sebserver.gui.entrypoint}")
    private String entrypoint;

    @Value("${sebserver.gui.external.messages:messages}")
    private String externalMessagesPath;

    @Value("${sebserver.gui.remote.proctoring.entrypoint:/remote-proctoring}")
    private String remoteProctoringEndpoint;

    @Value("${sebserver.gui.remote.proctoring.api-servler.endpoint:/remote-view-servlet}")
    private String remoteProctoringViewServletEndpoint;

    @Bean
    public StaticApplicationPropertyResolver staticApplicationPropertyResolver() {
        return new StaticApplicationPropertyResolver();
    }

    @Bean
    public ServletContextInitializer initializer() {
        staticApplicationPropertyResolver();
        return new RAPServletContextInitializer();
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
        return new ServletRegistrationBean<>(
                new RWTServlet(),
                this.entrypoint + "/*",
                this.remoteProctoringEndpoint + "/*");
    }

    @Bean
    public ServletRegistrationBean<ProctoringServlet> servletProctoringRegistrationBean(
            final ApplicationContext applicationContext) {

        final ProctoringServlet proctoringServlet = applicationContext
                .getBean(ProctoringServlet.class);
        return new ServletRegistrationBean<>(
                proctoringServlet,
                this.remoteProctoringEndpoint + this.remoteProctoringViewServletEndpoint + "/*");
    }

    @Bean
    public MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource reloadableResourceBundleMessageSource =
                new ReloadableResourceBundleMessageSource();

        log.info(" +++ Register external messages resources from: {}", this.externalMessagesPath);

        reloadableResourceBundleMessageSource.setBasenames(
                this.externalMessagesPath,
                "classpath:messages");

        return reloadableResourceBundleMessageSource;
    }

    private static class RAPServletContextInitializer implements ServletContextInitializer {
        @Override
        public void onStartup(final ServletContext servletContext) {
            servletContext.setInitParameter(
                    "org.eclipse.rap.applicationConfiguration",
                    RAPConfiguration.class.getName());
        }
    }

}
