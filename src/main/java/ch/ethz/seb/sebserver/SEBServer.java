/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/** SEB-Server (Safe Exam Browser Server) is a server component to maintain and support
 * Exams running with SEB (Safe Exam Browser). TODO add link(s)
 * <p>
 * SEB-Server uses Spring Boot as main framework is divided into two main components,
 * a webservice component that implements the business logic, persistence management
 * and defines a REST API to expose the services over HTTP. The second component is a
 * GUI component built on RAP RWT/SWT that also uses Spring components to connect and use
 * the webservice over HTTP. The two components are (implementation-wise) completely separated
 * from each other by the Rest API and the webservice can also be used by another client.
 * SEB-Server uses Spring's profiles to consequently separate sub-components of the webservice
 * and GUI and can be used to start the components on separate servers or within the same
 * server instance. Additional to the usual profiles like dev, prod, test there are combining
 * profiles like dev-ws, dev-gui and prod-ws, prod-gui test */
@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class,
})
public class SEBServer {

    public static void main(final String[] args) {
        org.apache.ibatis.logging.LogFactory.useSlf4jLogging();
        SpringApplication.run(SEBServer.class, args);
    }

    @Bean
    public HttpFirewall allowEncodedParamsFirewall() {
        final StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedPercent(true);
        return firewall;
    }

    private Connector redirectConnector(final Environment env) {
        final String sslPort = env.getRequiredProperty("server.port");
        final String httpPort = env.getProperty("sebserver.ssl.redirect.html.port", "80");
        final Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(Integer.parseInt(httpPort));
        connector.setSecure(false);
        connector.setRedirectPort(Integer.parseInt(sslPort));
        return connector;
    }

}
