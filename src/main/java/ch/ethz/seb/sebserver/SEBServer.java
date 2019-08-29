/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.ProdGuiProfile;
import ch.ethz.seb.sebserver.gbl.profile.ProdWebServiceProfile;

/** SEB-Server (Safe Exam Browser Server) is a server component to maintain and support
 * Exams running with SEB (Safe Exam Browser). TODO add link(s)
 *
 * SEB-Server uses Spring Boot as main framework is divided into two main components,
 * a webservice component that implements the business logic, persistence management
 * and defines a REST API to expose the services over HTTP. The second component is a
 * GUI component built on RAP RWT/SWT that also uses Spring components to connect and use
 * the webservice over HTTP. The two components are (implementation-wise) completely separated
 * from each other by the Rest API and the webservice can also be used by another client.
 * SEB-Server uses Spring's profiles to consequently separate sub-components of the webservice
 * and GUI and can be used to start the components on separate servers or within the same
 * server instance. Additional to the usual profiles like dev, prod, test there are combining
 * profiles like dev-ws, dev-gui and prod-ws, prod-gui */
@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class,
})
@EnableCaching
public class SEBServer {

    public static void main(final String[] args) {
        org.apache.ibatis.logging.LogFactory.useSlf4jLogging();
        SpringApplication.run(SEBServer.class, args);
    }

    /*
     * Add an additional redirect Connector on http port to redirect all http calls
     * to https.
     *
     * NOTE: This works with TomcatServletWebServerFactory and embedded tomcat.
     * If the webservice and/or gui is going to running on another server or
     * redirect is handled by a proxy, this redirect can be deactivated within
     * the "sebserver.ssl.redirect.enabled" property set to false
     */
    @Bean
    @ProdWebServiceProfile
    @ProdGuiProfile
    public ServletWebServerFactory servletContainer(
            final Environment env,
            final ApplicationContext applicationContext) {

        final String enabled = env.getProperty(
                "sebserver.ssl.redirect.enabled",
                Constants.FALSE_STRING);

        if (!BooleanUtils.toBoolean(enabled)) {
            return new TomcatServletWebServerFactory();
        }

        final TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(final Context context) {
                final SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                final SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(redirectConnector(env));
        return tomcat;
    }

    private Connector redirectConnector(final Environment env) {
        final String sslPort = env.getRequiredProperty("server.port");
        final String httpPort = env.getProperty("sebserver.ssl.redirect.html.port", "80");
        final Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(Integer.valueOf(httpPort));
        connector.setSecure(false);
        connector.setRedirectPort(Integer.valueOf(sslPort));
        return connector;
    }

}
