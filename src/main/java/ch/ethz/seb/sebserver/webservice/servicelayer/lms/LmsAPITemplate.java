/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.AbstractCachedCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.AbstractCourseAccess;

/** Defines an LMS API access template to build SEB Server LMS integration.
 * </p>
 * A LMS integration consists of two main parts so far:
 * </p>
 *
 * <pre>
 * - The course API to search and request course data from LMS as well as resolve some
 *   LMS account details for a given examineeId.
 * - The SEB restriction API to apply SEB restriction data to the LMS to restrict a
 *   certain course for SEB.
 * </pre>
 * </p>
 *
 * <b>Course API</b></br>
 * All course API requests of this template shall not block and return as fast as possible
 * with the best result it can provide for the time on that the request was made.
 * </p>
 * Each request to a remote LMS shall be executed within a protected call such that the
 * request don't block the API call as well as do not attack the remote LMS with endless
 * requests on failure.</br>
 * Therefore the abstract class {@link AbstractCourseAccess} defines protected calls
 * for different API calls by using {@link CircuitBreaker}. documentation on the class for
 * more information.
 * </p>
 * Since the course API requests course data from potentially thousands of existing and
 * active courses, the course API can implement some short time caches if needed.</br>
 * The abstract class {@link AbstractCachedCourseAccess} defines such a short time
 * cache for all implementing classes using EH-Cache. See documentation on the class for
 * more information.
 * </p>
 * <b>SEB restriction API</b></br>
 * For this API we need no caching since this is mostly about pushing data to the LMS for the LMS
 * to use. But this calls sahl also be protected within some kind of circuit breaker pattern to
 * avoid blocking on long latency.
 * </p>
 * </p>
 * A {@link LmsAPITemplate } will be constructed within the application with a {@link LmsSetup } instances.
 * The application constructs a {@link LmsAPITemplate } for each type of LMS setup when needed or requested and
 * there is not already a cached template or the cached template is out of date.</br>
 * The {@link LmsSetup } defines the data that is needed to connect to a specific LMS instance of implemented type
 * and is wrapped within a {@link LmsAPITemplate } instance that lives as long as there are no changes to the
 * {@link LmsSetup and the {@link LmsSetup } that is wrapped within the {@link LmsAPITemplate } is up to date.
 * <p>
 * The enum {@link LmsSetup.LmsType } defines the supported LMS types and for each type the supported API part(s).
 * <p>
 * The application uses the test functions that are defined for each LMS API part to test API access for a certain LMS
 * instance respectively the underling {@link LmsSetup }. Concrete implementations can do various tests to check full
 * or partial API Access and can flag missing or wrong {@link LmsSetup } attributes with the resulting
 * {@link LmsSetupTestResult }.</br>
 * SEB Server than uses an instance of this template to communicate with the an LMS. */
public interface LmsAPITemplate extends CourseAccessAPI, SEBRestrictionAPI {

    /** Get the LMS type of the concrete template implementation
     *
     * @return the LMS type of the concrete template implementation */
    LmsSetup.LmsType getType();

    /** Get the underling {@link LmsSetup } configuration for this LmsAPITemplate
     *
     * @return the underling {@link LmsSetup } configuration for this LmsAPITemplate */
    LmsSetup lmsSetup();

    default void dispose() {
        clearCourseCache();
    }

}
