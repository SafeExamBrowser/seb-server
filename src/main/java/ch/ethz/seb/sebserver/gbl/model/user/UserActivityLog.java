/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActionType;

public class UserActivityLog implements Entity {

    @JsonIgnore
    public final Long id;
    @JsonProperty("userId")
    public final String userId;
    @JsonProperty("timestamp")
    public final Long timestamp;
    @JsonProperty("actionType")
    public final ActionType actionType;
    @JsonProperty("entityType")
    public final EntityType entityType;
    @JsonProperty("entityId")
    public final String entityId;
    @JsonProperty("message")
    public final String message;

    public UserActivityLog(
            final Long id,
            final String userId,
            final Long timestamp,
            final ActionType actionType,
            final EntityType entityType,
            final String entityId,
            final String message) {

        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.message = message;
    }

    @Override
    @JsonIgnore
    public EntityType entityType() {
        return EntityType.USER_LOG;
    }

    @JsonIgnore
    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

    public String getUserId() {
        return this.userId;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public ActionType getActionType() {
        return this.actionType;
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
        return "UserActivityLog [id=" + this.id + ", userId=" + this.userId + ", timestamp=" + this.timestamp
                + ", actionType="
                + this.actionType + ", entityType=" + this.entityType + ", entityId=" + this.entityId + ", message="
                + this.message + "]";
    }

}
