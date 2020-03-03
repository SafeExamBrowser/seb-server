/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

/** Defines a form binding to get form parameter and values either in JSON format
 *  or as form-url-encoded string */
public interface FormBinding {

    /** Get the form parameter and values in JSON format
     *
     * @return the form parameter and values in JSON format */
    String getFormAsJson();

    /** Get the form parameter and values in form-url-encoded string format.
     *
     * @return the form parameter and values in form-url-encoded string format.*/
    String getFormUrlEncoded();

}
