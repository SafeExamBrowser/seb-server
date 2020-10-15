/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteProctoringRoom {

    @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID)
    public final Long id;

    @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_NAME)
    public final String name;

    @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_SIZE)
    public final Integer roomSize;

    @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_SUBJECT)
    public final String subject;

    @JsonCreator
    public RemoteProctoringRoom(
            @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID) final Long id,
            @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_NAME) final String name,
            @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_SIZE) final Integer roomSize,
            @JsonProperty(Domain.REMOTE_PROCTORING_ROOM.ATTR_SUBJECT) final String subject) {

        this.id = id;
        this.examId = examId;
        this.name = name;
        this.roomSize = roomSize;
        this.subject = subject;
    }

    public Long getId() {
        return this.id;
    }

    public Long getExamId() {
        return this.examId;
    }

    public String getName() {
        return this.name;
    }

    public Integer getRoomSize() {
        return this.roomSize;
    }

    public String getSubject() {
        return this.subject;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("RemoteProctoringRoom [id=");
        builder.append(this.id);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", roomSize=");
        builder.append(this.roomSize);
        builder.append(", subject=");
        builder.append(this.subject);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final RemoteProctoringRoom other = (RemoteProctoringRoom) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        return true;
    }

}
