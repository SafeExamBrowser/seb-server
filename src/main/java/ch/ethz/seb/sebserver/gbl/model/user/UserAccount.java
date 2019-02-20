/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Locale;
import java.util.Set;

import org.joda.time.DateTimeZone;

public interface UserAccount {

    String getModelId();

    Long getInstitutionId();

    String getName();

    String getUsername();

    String getEmail();

    Boolean getActive();

    boolean isActive();

    Locale getLocale();

    DateTimeZone getTimeZone();

    Set<String> getRoles();

    String getNewPassword();

    String getRetypedNewPassword();

}
