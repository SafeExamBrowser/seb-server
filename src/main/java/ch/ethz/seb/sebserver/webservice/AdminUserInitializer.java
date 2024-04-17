/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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

@Component
@WebServiceProfile
class AdminUserInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final WebserviceInfo webserviceInfo;
    private final UserDAO userDAO;
    private final InstitutionDAO institutionDAO;
    private final PasswordEncoder passwordEncoder;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final boolean initializeAdmin;
    private final String adminName;
    private final String orgName;
    private final Environment environment;

    public AdminUserInitializer(
            final WebserviceInfo webserviceInfo,
            final UserDAO userDAO,
            final InstitutionDAO institutionDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final Environment environment,
            @Qualifier(WebSecurityConfig.USER_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder passwordEncoder,
            @Value("${sebserver.init.adminaccount.gen-on-init:false}") final boolean initializeAdmin,
            @Value("${sebserver.init.adminaccount.username:seb-server-admin}") final String adminName,
            @Value("${sebserver.init.organisation.name:[SET_ORGANIZATION_NAME]}") final String orgName) {

        this.webserviceInfo = webserviceInfo;
        this.environment = environment;
        this.userDAO = userDAO;
        this.institutionDAO = institutionDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.passwordEncoder = passwordEncoder;
        this.initializeAdmin = initializeAdmin;
        this.adminName = adminName;
        this.orgName = orgName;
    }

    void initAdminAccount() {
        if (!this.initializeAdmin) {
            log.debug("Create initial admin account is switched off");
            return;
        }

        try {

            log.debug("Create initial admin account is switched on. Check database if exists...");
            final Result<SEBServerUser> byUsername = this.userDAO.sebServerAdminByUsername(this.adminName);
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
                        this.printAdminCredentials(this.adminName, generateAdminPassword);
                    }
                }
            } else {
                final String initPWD = environment.getProperty("sebserver.init.adminaccount.init.pwd", "");
                final CharSequence generateAdminPassword = StringUtils.isNotBlank(initPWD)
                        ? initPWD :
                        this.generateAdminPassword();

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
                        new HashSet<>(this.webserviceInfo.isLightSetup() ?
                                UserRole.getLightSetupRoles() :
                                List.of(UserRole.SEB_SERVER_ADMIN.name())
                        )))
                        .flatMap(account -> this.userDAO.setActive(account, true))
                        .map(account -> {
                            printAdminCredentials(this.adminName, generateAdminPassword);
                            if(this.webserviceInfo.isLightSetup()) {
                                writeInitialAdminCredentialsIntoDB(this.adminName, generateAdminPassword);
                            }
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

    private void printAdminCredentials(final String name, final CharSequence pwd) {
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

    private void writeInitialAdminCredentialsIntoDB(final String name, final CharSequence pwd){
        try {
            final Map<String, String> attributes = new HashMap<>();
            attributes.put(
                    Domain.USER.ATTR_USERNAME,
                    name);
            attributes.put(
                    Domain.USER.ATTR_PASSWORD,
                    String.valueOf(pwd));

            this.additionalAttributesDAO.saveAdditionalAttributes(EntityType.USER, Constants.LIGHT_ADMIN_USER_ID, attributes);

        } catch (final Exception e) {
            log.error("Unable to write initial admin credentials into the additional attributes table: ", e);
        }
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
