/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Abstract factory to create an LmsAPITemplate for specific LMS type.
 * Since a LmsAPITemplate of a specific LMS type
 * is whether a singleton component nor a simple prototype but one (singleton) instance
 * can exist per defined LMSSetup, we need a specialized factory to build such
 * a LmsAPITemplate for a specific LMSSetup. */
public interface LmsAPITemplateFactory {

    /** Defines the LMS type if a specific implementation.
     * This is used by the service to collect and map the template for specific LMS types.
     *
     * @return the LMS type if a specific implementation */
    LmsType lmsType();

    /** Creates a {@link LmsAPITemplate } for the specific implements LMS type
     * And provides it with the needed {@link APITemplateDataSupplier }
     *
     * @param apiTemplateDataSupplier supplies all needed actual LMS setup data */
    Result<LmsAPITemplate> create(final APITemplateDataSupplier apiTemplateDataSupplier);

}
