/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Abstract factory to create an LmsAPITemplate for specific LMS type */
public interface LmsAPITemplateFactory {

    /** Defines the LMS type if a specific implementation.
     * This is used by the service to collect and map the template for specific LMS types.
     *
     * @return the LMS type if a specific implementation */
    LmsType lmsType();

    /** Creates a LmsAPITemplate for the specific implements LMS type.
     *
     * @param lmsSetup the LMS setup data to initialize the template
     * @param credentials the access data for accessing the LMS API. Either client credentials or access token from LMS
     *            setup input
     * @param proxyData The proxy data used to connect to the LMS if needed.
     * @return Result refer to the LmsAPITemplate or to an error when happened */
    Result<LmsAPITemplate> create(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final ProxyData proxyData);

}
