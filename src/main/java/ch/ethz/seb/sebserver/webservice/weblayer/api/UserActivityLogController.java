/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_USER_ACTIVITY_LOG)
public class UserActivityLogController {

    private final UserActivityLogDAO userActivityLogDAO;
    private final AuthorizationGrantService authorizationGrantService;

    public UserActivityLogController(
            final UserActivityLogDAO userActivityLogDAO,
            final AuthorizationGrantService authorizationGrantService) {

        this.userActivityLogDAO = userActivityLogDAO;
        this.authorizationGrantService = authorizationGrantService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<UserActivityLog> getAll(
            @RequestParam(required = false) final Long from,
            @RequestParam(required = false) final Long to,
            @RequestParam(required = false) final String activityTypes,
            @RequestParam(required = false) final String entityTypes,
            final Principal principal) {

        return _getAll(null, from, to, activityTypes, entityTypes, principal);
    }

    @RequestMapping(path = "/{userId}", method = RequestMethod.GET)
    public Collection<UserActivityLog> getAllForUser(
            @PathVariable final String userId,
            @RequestParam(required = false) final Long from,
            @RequestParam(required = false) final Long to,
            @RequestParam(required = false) final String activityTypes,
            @RequestParam(required = false) final String entityTypes,
            final Principal principal) {

        return _getAll(userId, from, to, activityTypes, entityTypes, principal);
    }

    private Collection<UserActivityLog> _getAll(
            final String userId,
            final Long from,
            final Long to,
            final String activityTypes,
            final String entityTypes,
            final Principal principal) {

        // fist check if current user has any privileges for this action
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.USER_ACTIVITY_LOG,
                PrivilegeType.READ_ONLY);

        final Set<String> _activityTypes = (activityTypes != null)
                ? Collections.unmodifiableSet(new HashSet<>(
                        Arrays.asList(StringUtils.split(activityTypes, Constants.LIST_SEPARATOR))))
                : null;
        final Set<String> _entityTypes = (entityTypes != null)
                ? Collections.unmodifiableSet(new HashSet<>(
                        Arrays.asList(StringUtils.split(entityTypes, Constants.LIST_SEPARATOR))))
                : null;

        if (_activityTypes != null || _entityTypes != null) {

            return this.userActivityLogDAO.all(userId, from, to, record -> {
                if (_activityTypes != null && !_activityTypes.contains(record.getActivityType())) {
                    return false;
                }
                if (_entityTypes != null && !_entityTypes.contains(record.getEntityType())) {
                    return false;
                }

                return true;
            }).getOrThrow();

        } else {

            return this.userActivityLogDAO.all(userId, from, to, record -> true)
                    .getOrThrow();

        }
    }

}
