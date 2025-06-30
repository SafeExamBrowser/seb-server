/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

/** Defines a User-Account object */
public interface UserAccount extends GrantEntity, Activatable {

    /** The model id of the User-Account (UUID) */
    @Override
    String getModelId();

    /** The institution identifier where the User-Account belongs to */
    @Override
    Long getInstitutionId();

    /** Get the date when the user account was created */
    DateTime getCreationDate();

    /** The first name of the User */
    @Override
    String getName();

    /** The surname of the User */
    String getSurname();

    /** The user-name or login-name of the User-Account */
    String getUsername();

    /** The email of the User-Account */
    String getEmail();

    /** Indicates whether the User-Account is active or not */
    Boolean getActive();

    /** Indicates whether the User-Account is active or not */
    @Override
    boolean isActive();

    /** The language of the User-Account */
    Locale getLanguage();

    /** The time-zone of the User-Account */
    DateTimeZone getTimeZone();

    /** The roles of the User-Account */
    Set<String> getRoles();

    /** The roles of the User-Account as UerRole */
    EnumSet<UserRole> getUserRoles();
    
    boolean isOnlyTeacher();

    /** The EntityKey (ModelId plus EntityType) of the User-Account */
    @Override
    EntityKey getEntityKey();

}
