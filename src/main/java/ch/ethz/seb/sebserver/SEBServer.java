/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

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
 * profiles like dev-ws, dev-gui and prod-ws, prod-gui
 * 
 * TODO documentation for presets to start all-in-one server or separated gui- and webservice- server */
@SpringBootApplication(exclude = {
        // OAuth2ResourceServerAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        //DataSourceAutoConfiguration.class
})
public class SEBServer {

    public static void main(final String[] args) {
        org.apache.ibatis.logging.LogFactory.useSlf4jLogging();
        SpringApplication.run(SEBServer.class, args);
    }

}
