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

/** Supplier for LmsAPITemplate to supply the templat with the needed LMS connection data. */
public interface APITemplateDataSupplier {

    /** Get the LmsSetup instance containing all setup attributes
     *
     * @return the LmsSetup instance containing all setup attributes */
    LmsSetup getLmsSetup();

    /** Get the encoded LMS setup client credentials needed to access the LMS API.
     *
     * @return the encoded LMS setup client credentials needed to access the LMS API. */
    ClientCredentials getLmsClientCredentials();

    /** Get the proxy data if available and if needed for LMS connection
     *
     * @return the proxy data if available and if needed for LMS connection */
    ProxyData getProxyData();

}
