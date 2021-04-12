/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall.CallType;

/** Interface to SEB Server webservice API thought RestCall's
 * or thought Spring's RestTemplate on lower level.
 *
 * A RestCall's can be used to call a specified SEB Server webservice API endpoint with given request parameter.
 * This service collects all the available RestCalls and map them by Class type or EntityType and CallType.
 *
 * For Example if one want to get a certain User-Account by API request on SEB Server webservice API:
 *
 * <pre>
 *  UserInfo userInfo = RestService.getBuilder(GetUserAccount.class)
 *      .withURIVariable(API.PARAM_MODEL_ID, user-account-id)           adds an URI path variable
 *      .withQueryParam(API.PARAM_INSTITUTION_ID, institutionId)        adds a URI query parameter
 *      .call()                                                         executes the API request call
 *      .get(pageContext::notifyError)                                  gets the result or notify an error to the user if happened
 * </pre>
 */
public interface RestService {

    /** Get Spring's RestTemplate that is used within this service.
     *
     * @return Spring's RestTemplate that is used within this service. */
    RestTemplate getWebserviceAPIRestTemplate();

    /** Get Spring's UriComponentsBuilder that is used within this service.
     *
     * @return Spring's UriComponentsBuilder that is used within this service. */
    UriComponentsBuilder getWebserviceURIBuilder();

    /** Get a certain RestCall by Class type.
     *
     * @param type the Class type of the RestCall
     * @return RestCall instance */
    <T> RestCall<T> getRestCall(Class<? extends RestCall<T>> type);

    /** Get a certain RestCall by EntityType and CallType.
     * NOTE not all RestCall can be get within this method. Only the ones that have a defined CallType
     *
     * @param entityType The EntityType of the RestCall
     * @param callType The CallType of the RestCall (not UNDEFINED)
     * @return RestCall instance */
    <T> RestCall<T> getRestCall(EntityType entityType, CallType callType);

    /** Get a certain RestCallBuilder by RestCall Class type.
     *
     * @param type the Class type of the RestCall
     * @return RestCallBuilder instance to build a dedicated call and execute it */
    <T> RestCall<T>.RestCallBuilder getBuilder(Class<? extends RestCall<T>> type);

    /** Get a certain RestCallBuilder by EntityType and CallType.
     *
     * @param entityType The EntityType of the RestCall to get a builder for
     * @param callType The CallType of the RestCall to get a builder for (not UNDEFINED)
     * @return RestCallBuilder instance to build a dedicated call and execute it */
    <T> RestCall<T>.RestCallBuilder getBuilder(
            EntityType entityType,
            CallType callType);

    /** Use this to inject the current SEB Server API access RestTemplate to a long living
     * RestCallBuilder. This is usually used to recover from a disposed API access RestTemplate.
     * 
     * @param <T> The generic type of RestCallBuilder
     * @param builder the RestCallBuilder to inject the current RestTemplate into. */
    default <T> void injectCurrentRestTemplate(final RestCall<T>.RestCallBuilder builder) {
        builder.withRestTemplate(getWebserviceAPIRestTemplate());
    }

}