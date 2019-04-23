/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.sql.DataSource;

import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

// TODO check if DataSourceAutoConfiguration and TokenStore bean definition is really needed here
//      or if it is possible to move them to the WebServiceSecurityConfig.
//      test with starting web and gui separately as well as together
@Configuration
@WebServiceProfile
@Import(DataSourceAutoConfiguration.class)
public class WebServiceInit implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(WebServiceInit.class);

    @Autowired
    private Environment environment;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        log.info("Initialize SEB-Server Web-Service Component");

        try {
            log.info("----> config server address: {}", this.environment.getProperty("server.address"));
            log.info("----> config server port: {}", this.environment.getProperty("server.port"));

            log.info("----> local host address: {}", InetAddress.getLocalHost().getHostAddress());
            log.info("----> local host name: {}", InetAddress.getLocalHost().getHostName());

            log.info("----> remote host address: {}", InetAddress.getLoopbackAddress().getHostAddress());
            log.info("----> remote host address: {}", InetAddress.getLoopbackAddress().getHostName());
        } catch (final UnknownHostException e) {
            log.error("Unknown Host: ", e);
        }

        // TODO whatever has to be initialized for the web-service component right after startup comes here

    }

    @Lazy
    @Bean
    public TokenStore tokenStore(final DataSource dataSource) {
        return new JdbcTokenStore(dataSource);
    }

    @Lazy
    @Bean
    public JNCryptor jnCryptor() {
        return new AES256JNCryptor();
    }
}
