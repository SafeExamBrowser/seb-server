/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.security.Principal;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;

@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}/useraccount")
public class UserAccountController {

    private final UserDAO userDao;
    private final AuthorizationGrantService authorizationGrantService;
    private final UserService userService;

    public UserAccountController(
            final UserDAO userDao,
            final AuthorizationGrantService authorizationGrantService,
            final UserService userService) {

        this.userDao = userDao;
        this.authorizationGrantService = authorizationGrantService;
        this.userService = userService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<UserInfo> getAll(
            //@RequestParam(required = false) final UserFilter filter,
            @RequestBody(required = false) final UserFilter filter,
            final Principal principal) {

        if (this.authorizationGrantService.hasBaseGrant(
                EntityType.USER,
                GrantType.READ_ONLY,
                principal)) {

            return (filter != null)
                    ? this.userDao.all(filter).getOrThrow()
                    : this.userDao.allActive().getOrThrow();

        } else {

            final Predicate<UserInfo> grantFilter = this.authorizationGrantService.getGrantFilter(
                    EntityType.USER,
                    GrantType.READ_ONLY,
                    principal);

            if (filter == null) {

                return this.userDao
                        .all(grantFilter)
                        .getOrThrow();
            } else {

                return this.userDao
                        .all(filter)
                        .getOrThrow()
                        .stream()
                        .filter(grantFilter)
                        .collect(Collectors.toList());
            }
        }
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public UserInfo loggedInUser(final Authentication auth) {
        return this.userService
                .getCurrentUser()
                .getUserInfo();
    }

    @RequestMapping(value = "/{userUUID}", method = RequestMethod.GET)
    public UserInfo accountInfo(@PathVariable final String userUUID, final Principal principal) {
        return this.userDao
                .byUuid(userUUID)
                .flatMap(userInfo -> this.authorizationGrantService.checkGrantForEntity(
                        userInfo,
                        GrantType.READ_ONLY,
                        principal))
                .getOrThrow();

    }

//    @RequestMapping(value = "/", method = RequestMethod.POST)
//    public UserInfo save(
//            @PathVariable final Long institutionId,
//            @RequestBody final UserFilter filter,
//            final Principal principal) {
//
//    }

}
