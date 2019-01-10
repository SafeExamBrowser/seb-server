/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.activation;

import java.util.Collection;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ActivatableEntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;

@Service
@WebServiceProfile
public class EntityActivationService {

    private final Collection<ActivatableEntityDAO<?>> activatableEntityDAOs;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserActivityLogDAO userActivityLogDAO;

    public EntityActivationService(
            final Collection<ActivatableEntityDAO<?>> activatableEntityDAOs,
            final ApplicationEventPublisher applicationEventPublisher,
            final UserActivityLogDAO userActivityLogDAO) {

        this.activatableEntityDAOs = activatableEntityDAOs;
        this.applicationEventPublisher = applicationEventPublisher;
        this.userActivityLogDAO = userActivityLogDAO;
    }

    public ApplicationEventPublisher getApplicationEventPublisher() {
        return this.applicationEventPublisher;
    }

    @EventListener(EntityActivationEvent.class)
    public void notifyActivationEvent(final EntityActivationEvent event) {
        for (final ActivatableEntityDAO<?> dao : this.activatableEntityDAOs) {
            if (event.activated) {
                dao.notifyActivation(event.getEntity());
            } else {
                dao.notifyDeactivation(event.getEntity());
            }
        }
    }

    public <T extends Entity> Result<T> setActive(final T entity, final boolean activated) {

        final ActivityType activityType = (activated)
                ? ActivityType.ACTIVATE
                : ActivityType.DEACTIVATE;

        return getDAOForEntity(entity)
                .setActive(entity.getModelId(), activated)
                .flatMap(e -> publishEvent(e, activated))
                .flatMap(e -> this.userActivityLogDAO.log(activityType, e));

    }

    public <T extends Entity> Result<T> publishEvent(final T entity, final boolean activated) {
        this.applicationEventPublisher.publishEvent(new EntityActivationEvent(entity, activated));
        return Result.of(entity);
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> ActivatableEntityDAO<T> getDAOForEntity(final T entity) {
        for (final ActivatableEntityDAO<?> dao : this.activatableEntityDAOs) {
            if (dao.entityType() == entity.entityType()) {
                return (ActivatableEntityDAO<T>) dao;
            }
        }

        return null;
    }

}
