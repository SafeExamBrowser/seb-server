/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Configuration
@WebServiceProfile
public class WebserviceConfig {

    @Value("${sebserver.webservice.clean-db-on-startup:false}")
    boolean cleanDBOnStartup;

    @Lazy
    @Bean
    public JNCryptor jnCryptor() {
        final AES256JNCryptor aes256jnCryptor = new AES256JNCryptor();
        aes256jnCryptor.setPBKDFIterations(Constants.JN_CRYPTOR_ITERATIONS);
        return aes256jnCryptor;
    }

    /** For test, development and demo profile, we want to always clean up and
     * Start the migration from scratch to work with the same data.
     *
     * @return FlywayMigrationStrategy for "dev-ws", "test", "demo" profiles */
    @Bean
    @Profile(value = { "dev-ws", "test", "demo" })
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        final FlywayMigrationStrategy strategy = new FlywayMigrationStrategy() {
            @Override
            public void migrate(final Flyway flyway) {
                if (WebserviceConfig.this.cleanDBOnStartup) {
                    flyway.clean();
                }
                flyway.migrate();
            }
        };

        return strategy;
    }

}
