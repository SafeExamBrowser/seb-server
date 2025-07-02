/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.auth;

import javax.servlet.http.HttpSession;

import org.eclipse.rap.rwt.RWT;

/** Single point of access for SEBServerAuthorizationContext */
public interface AuthorizationContextHolder {

    /** Get the SEBServerAuthorizationContext that is bound the the given HttpSession.
     * If there is no AuthorizationContext or an invalid one within the given HttpSession
     * a new one is created for the given HttpSession
     *
     * @param session HttpSession instance
     * @return SEBServerAuthorizationContext instance */
    SEBServerAuthorizationContext getAuthorizationContext(HttpSession session);

    /** Get the WebserviceURIService that is used within this AuthorizationContextHolder
     *
     * @return the WebserviceURIService that is used within this AuthorizationContextHolder */
    WebserviceURIService getWebserviceURIService();

    /** Get the SEBServerAuthorizationContext that is bound the HttpSession of the current
     * RWT UISession.
     * NOTE: This may throw an exception if RWT.getUISession().getHttpSession() throws one
     * This is the case if there is no RWT UISession within the current Thread
     *
     * @return SEBServerAuthorizationContext instance */
    default SEBServerAuthorizationContext getAuthorizationContext() {
        return getAuthorizationContext(RWT.getUISession().getHttpSession());
    }

}
