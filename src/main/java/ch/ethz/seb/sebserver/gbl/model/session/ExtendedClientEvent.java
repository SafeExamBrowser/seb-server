/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;

public final class ExtendedClientEvent extends ClientEvent {

    public static final String FILTER_ATTRIBUTE_EXAM = Domain.CLIENT_CONNECTION.ATTR_EXAM_ID;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID)
    public final String userSessionId;

    @JsonCreator
    public ExtendedClientEvent(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID) final String userSessionId,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_ID) final Long id,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_CLIENT_CONNECTION_ID) final Long connectionId,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TYPE) final EventType eventType,
            @JsonProperty(ATTR_TIMESTAMP) final Long clientTime,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_SERVER_TIME) final Long serverTime,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE) final Double numValue,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TEXT) final String text) {

        super(id, connectionId, eventType, clientTime, serverTime, numValue, text);

        this.institutionId = institutionId;
        this.examId = examId;
        this.userSessionId = userSessionId;
    }

    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getExamId() {
        return this.examId;
    }

    public String getUserSessionId() {
        return this.userSessionId;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ExtendedClientEvent [institutionId=");
        builder.append(this.institutionId);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", userSessionId=");
        builder.append(this.userSessionId);
        builder.append(", connectionId=");
        builder.append(this.connectionId);
        builder.append(", eventType=");
        builder.append(this.eventType);
        builder.append(", clientTime=");
        builder.append(this.clientTime);
        builder.append(", serverTime=");
        builder.append(this.serverTime);
        builder.append(", numValue=");
        builder.append(this.numValue);
        builder.append(", text=");
        builder.append(this.text);
        builder.append("]");
        return builder.toString();
    }

}
