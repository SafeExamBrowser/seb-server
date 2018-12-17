/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER_ACTIVITY_LOG;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;

public class UserActivityLog implements Entity {

    @JsonProperty(Domain.ATTR_ID)
    public final Long id;
    @JsonProperty(USER_ACTIVITY_LOG.ATTR_USER_UUID)
    public final String userUUID;
    @JsonProperty(USER_ACTIVITY_LOG.ATTR_TIMESTAMP)
    public final Long timestamp;
    @JsonProperty(USER_ACTIVITY_LOG.ATTR_ACTIVITY_TYPE)
    public final ActivityType activityType;
    @JsonProperty(USER_ACTIVITY_LOG.ATTR_ENTITY_TYPE)
    public final EntityType entityType;
    @JsonProperty(USER_ACTIVITY_LOG.ATTR_ENTITY_ID)
    public final String entityId;
    @JsonProperty(USER_ACTIVITY_LOG.ATTR_MESSAGE)
    public final String message;

    @JsonCreator
    public UserActivityLog(
            @JsonProperty(Domain.ATTR_ID) final Long id,
            @JsonProperty(USER_ACTIVITY_LOG.ATTR_USER_UUID) final String userUUID,
            @JsonProperty(USER_ACTIVITY_LOG.ATTR_TIMESTAMP) final Long timestamp,
            @JsonProperty(USER_ACTIVITY_LOG.ATTR_ACTIVITY_TYPE) final ActivityType activityType,
            @JsonProperty(USER_ACTIVITY_LOG.ATTR_ENTITY_TYPE) final EntityType entityType,
            @JsonProperty(USER_ACTIVITY_LOG.ATTR_ENTITY_ID) final String entityId,
            @JsonProperty(USER_ACTIVITY_LOG.ATTR_MESSAGE) final String message) {

        this.id = id;
        this.userUUID = userUUID;
        this.timestamp = timestamp;
        this.activityType = activityType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.message = message;
    }

    @Override
    @JsonIgnore
    public EntityType entityType() {
        return EntityType.USER_ACTIVITY_LOG;
    }

    @Override
    public String getId() {
        return (this.id != null) ? String.valueOf(this.id) : null;
    }

    public String getUserUUID() {
        return this.userUUID;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public ActivityType getActivityType() {
        return this.activityType;
    }

    public EntityType getEntityType() {
        return this.entityType;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return "UserActivityLog [id=" + this.id + ", userUUID=" + this.userUUID + ", timestamp=" + this.timestamp
                + ", activityType="
                + this.activityType + ", entityType=" + this.entityType + ", entityId=" + this.entityId + ", message="
                + this.message + "]";
    }

}
