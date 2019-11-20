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
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

//TODO check if DataSourceAutoConfiguration and TokenStore bean definition is really needed here
//or if it is possible to move them to the WebServiceSecurityConfig.
//test with starting web and gui separately as well as together
@Configuration
@WebServiceProfile
@Import(DataSourceAutoConfiguration.class)
public class WebserviceConfig {

    @Lazy
    @Bean
    public JNCryptor jnCryptor() {
        final AES256JNCryptor aes256jnCryptor = new AES256JNCryptor();
        aes256jnCryptor.setPBKDFIterations(Constants.JN_CRYPTOR_ITERATIONS);
        return aes256jnCryptor;
    }

}
