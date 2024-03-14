/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.auth;

public class DisposedOAuth2RestTemplateException extends IllegalStateException {

    private static final long serialVersionUID = -8439656564917103027L;

    public DisposedOAuth2RestTemplateException(final String s) {
        super(s);
    }

}
