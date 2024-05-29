/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import org.springframework.context.ApplicationEvent;

public class ConnectionConfigurationChangeEvent extends ApplicationEvent  {

    public final Long institutionId;
    public final Long configId;
    public ConnectionConfigurationChangeEvent(final Long institutionId, final Long configId) {
        super(configId);
        this.institutionId = institutionId;
        this.configId = configId;
    }
}
