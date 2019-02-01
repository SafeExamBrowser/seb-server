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
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {
        // OAuth2ResourceServerAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        //DataSourceAutoConfiguration.class
})
@Configuration
@Import(DataSourceAutoConfiguration.class)
public class SEBServer {

    public static void main(final String[] args) {
        org.apache.ibatis.logging.LogFactory.useSlf4jLogging();
        SpringApplication.run(SEBServer.class, args);
    }

}
