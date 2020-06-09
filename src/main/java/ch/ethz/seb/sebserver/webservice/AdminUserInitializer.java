/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialServiceImpl;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

@Component
@WebServiceProfile
class AdminUserInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserDAO userDAO;
    private final InstitutionDAO institutionDAO;
    private final PasswordEncoder passwordEncoder;
    private final boolean initializeAdmin;
    private final String adminName;
    private final String orgName;

    public AdminUserInitializer(
            final UserDAO userDAO,
            final InstitutionDAO institutionDAO,
            @Qualifier(WebSecurityConfig.USER_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder passwordEncoder,
            @Value("${sebserver.init.adminaccount.gen-on-init:false}") final boolean initializeAdmin,
            @Value("${sebserver.init.adminaccount.username:seb-server-admin}") final String adminName,
            @Value("${sebserver.init.organisation.name:[SET_ORGANIZATION_NAME]}") final String orgName) {

        this.userDAO = userDAO;
        this.institutionDAO = institutionDAO;
        this.passwordEncoder = passwordEncoder;
        this.initializeAdmin = initializeAdmin;
        this.adminName = adminName;
        this.orgName = orgName;
    }

    void initAdminAccount() {
        if (!this.initializeAdmin) {
            log.debug("Create initial admin account is switched on off");
            return;
        }

        try {

            log.debug("Create initial admin account is switched on. Check database if exists...");
            final Result<SEBServerUser> byUsername = this.userDAO.sebServerUserByUsername(this.adminName);
            if (byUsername.hasValue()) {

                log.debug("Initial admin account already exists. Check if the password must be reset...");

                final SEBServerUser sebServerUser = byUsername.get();
                final String password = sebServerUser.getPassword();
                if (this.passwordEncoder.matches("admin", password)) {

                    log.debug("Setting new generated password for already existing admin account");
                    final CharSequence generateAdminPassword = this.generateAdminPassword();
                    if (generateAdminPassword != null) {
                        this.userDAO.changePassword(
                                sebServerUser.getUserInfo().getModelId(),
                                generateAdminPassword);
                        this.writeAdminCredentials(this.adminName, generateAdminPassword);
                    }
                }
            } else {
                final CharSequence generateAdminPassword = this.generateAdminPassword();
                Long institutionId = this.institutionDAO.allMatching(new FilterMap())
                        .getOrElse(Collections::emptyList)
                        .stream()
                        .findFirst()
                        .filter(Institution::isActive)
                        .map(Institution::getInstitutionId)
                        .orElse(-1L);

                if (institutionId < 0) {

                    log.debug("Create new initial institution");
                    institutionId = this.institutionDAO.createNew(new Institution(
                            null,
                            this.orgName,
                            null,
                            null,
                            null,
                            true))
                            .map(inst -> this.institutionDAO.setActive(inst, true).getOrThrow())
                            .map(Institution::getInstitutionId)
                            .getOrThrow();
                }

                this.userDAO.createNew(new UserMod(
                        this.adminName,
                        institutionId,
                        this.adminName,
                        this.adminName,
                        this.adminName,
                        generateAdminPassword,
                        generateAdminPassword,
                        null,
                        null,
                        null,
                        new HashSet<>(Arrays.asList(UserRole.SEB_SERVER_ADMIN.name()))))
                        .flatMap(account -> this.userDAO.setActive(account, true))
                        .map(account -> {
                            writeAdminCredentials(this.adminName, generateAdminPassword);
                            return account;
                        })
                        .getOrThrow();
                }
        } catch (final Exception e) {
            SEBServerInit.INIT_LOGGER.error("---->");
            SEBServerInit.INIT_LOGGER.error("----> SEB Server initial admin-account creation failed: ", e);
            SEBServerInit.INIT_LOGGER.error("---->");
        }
    }

    private void writeAdminCredentials(final String name, final CharSequence pwd) {
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info(
                "----> ******************************************************************************************"
                        + "*****************************************************************************");
        SEBServerInit.INIT_LOGGER.info("----> SEB Server initial admin-account; name: {}, pwd: {}", name, pwd);
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info(
                "----> !!!! NOTE: Do not forget to login and reset the generated admin password immediately !!!!");
        SEBServerInit.INIT_LOGGER.info(
                "----> ******************************************************************************************"
                        + "*****************************************************************************");
        SEBServerInit.INIT_LOGGER.info("---->");
    }

    private CharSequence generateAdminPassword() {
        try {
            return ClientCredentialServiceImpl.generateClientSecret();
        } catch (final Exception e) {
            log.error("Unable to generate admin password: ", e);
            throw e;
        }
    }

}
