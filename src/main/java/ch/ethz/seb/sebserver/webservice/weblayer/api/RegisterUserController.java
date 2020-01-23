/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.REGISTER_ENDPOINT)
public class RegisterUserController {

    private final InstitutionDAO institutionDAO;
    private final UserActivityLogDAO userActivityLogDAO;
    private final UserDAO userDAO;
    private final ClientCredentialService clientCredentialService;

    protected RegisterUserController(
            final InstitutionDAO institutionDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final UserDAO userDAO,
            final ClientCredentialService clientCredentialService,
            @Qualifier(WebSecurityConfig.USER_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder userPasswordEncoder) {

        this.institutionDAO = institutionDAO;
        this.userActivityLogDAO = userActivityLogDAO;
        this.userDAO = userDAO;
        this.clientCredentialService = clientCredentialService;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public UserInfo registerNewUser(
            @RequestParam(name = Domain.USER.ATTR_INSTITUTION_ID, required = true) final String institutionId,
            @RequestParam(name = Domain.USER.ATTR_NAME, required = true) final String name,
            @RequestParam(name = Domain.USER.ATTR_USERNAME, required = true) final String username,
            @RequestParam(name = Domain.USER.ATTR_EMAIL, required = false) final String email,
            @RequestParam(
                    name = Domain.USER.ATTR_LANGUAGE,
                    required = false,
                    defaultValue = Constants.DEFAULT_LANG_CODE) final String lang,
            @RequestParam(
                    name = Domain.USER.ATTR_TIMEZONE,
                    required = false,
                    defaultValue = Constants.DEFAULT_TIME_ZONE_CODE) final String timezone,
            @RequestParam(name = PasswordChange.ATTR_NAME_NEW_PASSWORD, required = true) final String pwd,
            @RequestParam(name = PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, required = true) final String rpwd) {

        final Collection<APIMessage> errors = new ArrayList<>();

        // check institution info
        Long instId = null;
        if (StringUtils.isNotBlank(institutionId)) {
            try {
                instId = Long.parseLong(institutionId);
            } catch (final Exception e) {
                instId = this.institutionDAO
                        .all(null, true)
                        .getOrThrow()
                        .stream()
                        .filter(inst -> inst.urlSuffix != null && institutionId.equals(inst.urlSuffix))
                        .findFirst()
                        .map(inst -> inst.id)
                        .orElse(null);
            }
        }

        if (instId == null) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            "user",
                            Domain.USER.ATTR_INSTITUTION_ID,
                            "user:institutionId:notNull")));
        }

        // check password-match
        final CharSequence rawPWD = this.clientCredentialService.decrypt(pwd);
        final CharSequence rawRPWD = this.clientCredentialService.decrypt(rpwd);
        if (!rawPWD.equals(rawRPWD)) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            "passwordChange",
                            PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                            "user:confirmNewPassword:password.mismatch")));
        }

        if (!errors.isEmpty()) {
            throw new APIMessageException(errors);
        }

        final UserMod user = new UserMod(
                null,
                instId,
                name,
                username,
                rawPWD,
                rawRPWD,
                email,
                Locale.forLanguageTag(lang),
                DateTimeZone.forID(timezone),
                new HashSet<>(Arrays.asList(UserRole.EXAM_SUPPORTER.name())));

        return this.userDAO.createNew(user)
                .flatMap(this.userActivityLogDAO::logCreate)
                .map(u -> {
                    Utils.clear(rawPWD);
                    Utils.clear(rawRPWD);
                    return u;
                })
                .getOrThrow();
    }

}
