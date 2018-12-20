/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.activation;

import java.util.Collection;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ActivatableEntityDAO;

@Service
@WebServiceProfile
public class EntityActivationService {

    private final Collection<ActivatableEntityDAO<?>> activatableEntityDAOs;

    public EntityActivationService(final Collection<ActivatableEntityDAO<?>> activatableEntityDAOs) {
        this.activatableEntityDAOs = activatableEntityDAOs;
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

}
